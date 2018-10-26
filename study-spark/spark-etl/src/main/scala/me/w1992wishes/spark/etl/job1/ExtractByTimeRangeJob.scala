package me.w1992wishes.spark.etl.job1

import java.io.BufferedInputStream
import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.{Duration, LocalDateTime, ZoneId}
import java.util.Properties

import com.typesafe.scalalogging.Logger
import me.w1992wishes.spark.etl.util.{ConnectionUtils, DateUtils}
import org.apache.spark.sql.SparkSession

import scala.collection.mutable.ArrayBuffer

/**
  * 按照时间段提取抓拍库时序数据，默认为提取每日增量抓拍数据
  *
  * @author w1992wishes 2018/9/27 15:45
  */
object ExtractByTimeRangeJob {

  private[this] val LOG = Logger(this.getClass)

  // 文件要放到resources文件夹下
  private[this] val properties = new Properties()
  private[this] val path = getClass.getResourceAsStream("/config.properties")
  properties.load(new BufferedInputStream(path))
  // spark 配置属性
  private[this] val sparkMaster = properties.getProperty("spark.master")
  private[this] val sparkAppName = properties.getProperty("spark.appName")
  private[this] val sparkPartitions = Integer.parseInt(properties.getProperty("spark.partitions", "112"))
  // greenplum 配置属性
  private[this] val dbUrl = properties.getProperty("db.url")
  private[this] val dbDriver = properties.getProperty("db.driver")
  private[this] val dbUser = properties.getProperty("db.user")
  private[this] val dbPasswd = properties.getProperty("db.passwd")
  private[this] val dbSourceTable = properties.getProperty("db.source.table")
  private[this] val dbSinkTable = properties.getProperty("db.sink.table")
  private[this] val dbFetchsize = properties.getProperty("db.fetchsize")
  private[this] val dbBatchsize = properties.getProperty("db.batchsize")

  /**
    * 拆分时间段，用于 spark 并行查询
    *
    * @param start
    * @param end
    * @return
    */
  private[this] def predicates(start: LocalDateTime, end: LocalDateTime): Array[String] = {

    // 查询的开始时间和结束时间的间隔分钟数
    val durationMinutes = Duration.between(start, end).toMinutes
    val timePart = ArrayBuffer[(String, String)]()
    if (sparkPartitions.longValue() < durationMinutes) {
      val step = durationMinutes / (sparkPartitions - 1)
      for (x <- 1 until sparkPartitions) {
        timePart += DateUtils.dateTimeToStr(start.plusMinutes((x - 1) * step)) -> DateUtils.dateTimeToStr(start.plusMinutes(x * step))
      }
      timePart += DateUtils.dateTimeToStr(start.plusMinutes((sparkPartitions - 1) * step)) -> DateUtils.dateTimeToStr(end)
    } else {
      for (x <- 1 to durationMinutes.intValue()) {
        timePart += DateUtils.dateTimeToStr(start.plusMinutes((x - 1))) -> DateUtils.dateTimeToStr(start.plusMinutes(x))
      }
    }

    timePart.map {
      case (start, end) =>
        s"time >= '$start' " + s"AND time < '$end'"
    }.toArray
  }

  /**
    * db 属性设置
    *
    * @return
    */
  private def dbProperties(): Properties = {
    // 设置连接用户&密码
    val prop = new java.util.Properties
    prop.setProperty("user", dbUser)
    prop.setProperty("password", dbPasswd)
    prop.setProperty("driver", dbDriver)
    prop.setProperty("fetchsize", dbFetchsize)
    prop
  }

  /**
    * 并行加载数据的起始时间
    *
    * @return
    */
  private def calculateStartTime(): LocalDateTime = {
    var conn: Connection = null
    var pstmt: PreparedStatement = null
    var rs: ResultSet = null
    try {
      conn = ConnectionUtils.getConnection(dbUrl, dbUser, dbPasswd)
      var sql = "select max(time) from " + dbSinkTable
      var startDateTime: LocalDateTime =
        LocalDateTime.ofInstant(new Timestamp(System.currentTimeMillis()).toInstant, ZoneId.systemDefault())
          .withSecond(0)
          .withNano(0)
      pstmt = conn.prepareStatement(sql)
      rs = pstmt.executeQuery()
      if (rs.next() && rs.getTimestamp(1) != null) {
        // max(time) 去除秒后再加一分钟
        startDateTime =
          LocalDateTime.ofInstant(rs.getTimestamp(1).toInstant, ZoneId.systemDefault())
            .withSecond(0)
            .withNano(0)
            .plusMinutes(1)
      } else {
        sql = "select min(time) from " + dbSourceTable
        pstmt = conn.prepareStatement(sql)
        rs = pstmt.executeQuery()
        if (rs.next() && rs.getTimestamp(1) != null) {
          startDateTime =
            LocalDateTime.ofInstant(rs.getTimestamp(1).toInstant, ZoneId.systemDefault())
              .withSecond(0)
              .withNano(0)
        }
      }
      startDateTime
    } finally {
      ConnectionUtils.closeResource(conn, pstmt, rs)
    }
  }

  /**
    * 并行加载数据的结束时间
    *
    * @return
    */
  private def calculateEndTime() = {
    var conn: Connection = null
    var pstmt: PreparedStatement = null
    var rs: ResultSet = null
    try {
      conn = ConnectionUtils.getConnection(dbUrl, dbUser, dbPasswd)
      var sql = "select max(time) from " + dbSourceTable
      var end: Timestamp = new Timestamp(System.currentTimeMillis())
      pstmt = conn.prepareStatement(sql)
      rs = pstmt.executeQuery()
      if (rs.next() && rs.getTimestamp(1) != null) {
        end = rs.getTimestamp(1)
      }
      LocalDateTime.ofInstant(end.toInstant, ZoneId.systemDefault())
        .withSecond(0)
        .withNano(0)
    } finally {
      ConnectionUtils.closeResource(conn, pstmt, rs)
    }
  }

  def main(args: Array[String]): Unit = {

    // db 配置
    val prop = dbProperties()
    // 并行加载数据的 起始时间
    val start = calculateStartTime()
    // 并行加载数据的 结束时间
    val end = calculateEndTime()
    // 并行时间数组
    val timeArray = predicates(start, end)

    val beginTime = System.currentTimeMillis()
    // 根据时间从 timingX 数据源加载数据
    if (timeArray.length > 0) {
      // spark
      val spark = SparkSession
        .builder()
        //.master(sparkMaster)
        .appName(sparkAppName)
        .getOrCreate()

      val timingXDF = spark.read
        .jdbc(dbUrl, dbSourceTable, timeArray, prop)
      LOG.info("======> timingXDF.rdd.partitions.size = {}", timingXDF.rdd.partitions.length)

      // 将加载的数据保存到 t_timing_x 中
      timingXDF.write
        .mode("append")
        .format("jdbc")
        .options(
          Map("driver" -> dbDriver,
            "url" -> dbUrl,
            "dbtable" -> dbSinkTable,
            "user" -> dbUser,
            "password" -> dbPasswd,
            "batchsize" -> dbBatchsize)
        )
        .save()

      spark.stop()
    }
    LOG.info("======> ExtractByTimeRangeJob speed time {}s", (System.currentTimeMillis() - beginTime) / 1000)

  }

}
