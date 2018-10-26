package me.w1992wishes.spark.etl.job2

import java.io.BufferedInputStream
import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.{Duration, LocalDateTime, ZoneId}
import java.util.Properties

import com.alibaba.fastjson.JSON
import com.typesafe.scalalogging.Logger
import me.w1992wishes.spark.etl.util.{ConnectionUtils, DateUtils, PreprocessUtils}
import org.apache.spark.sql.{Row, SparkSession}

import scala.collection.mutable.ArrayBuffer

/**
  * 对增量数据做一些清洗、过滤、转化等预处理
  *
  * @author w1992wishes 2018/10/16 11:21
  */
object PreprocessJob {

  private[this] val LOG = Logger(this.getClass)

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
    val prop = new Properties
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
  private def calculateEndTime(): LocalDateTime = {
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

  /**
    * 分区执行批量插入
    *
    * @param iterator
    */
  private def insertDataFunc(iterator: Iterator[Row]) = {
    var conn: Connection = null
    var pstmt: PreparedStatement = null
    val sql = "insert into " + dbSinkTable + "(face_id, image_data, face_feature, from_image_id, gender, " +
      "accessories, age, json, quality, race, source_id, source_type, locus, time, version, pose, " +
      "filter_tag, create_time, update_time, delete_flag) " +
      "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    var i = 0
    try {
      conn = ConnectionUtils.getConnection(dbUrl, dbUser, dbPasswd)
      conn.setAutoCommit(false);
      pstmt = conn.prepareStatement(sql)
      iterator.foreach { row => {
        i += 1
        if (i > dbBatchsize.toInt) {
          i = 0
          pstmt.executeBatch();
          pstmt.clearBatch();
        }
        pstmt.setLong(1, row.getAs[Long]("face_id"))
        pstmt.setString(2, row.getAs[String]("image_data"))
        pstmt.setBytes(3, row.getAs[Array[Byte]]("face_feature"))
        pstmt.setLong(4, row.getAs[Long]("from_image_id"))
        pstmt.setInt(5, row.getAs[Int]("gender"))
        pstmt.setInt(6, row.getAs[Int]("accessories"))
        pstmt.setInt(7, row.getAs[Int]("age"))
        pstmt.setString(8, row.getAs[String]("json"))
        pstmt.setInt(9, row.getAs[Int]("quality"))
        pstmt.setInt(10, row.getAs[Int]("race"))
        pstmt.setLong(11, row.getAs[Long]("source_id"))
        pstmt.setInt(12, row.getAs[Int]("source_type"))
        pstmt.setString(13, row.getAs[String]("locus"))
        pstmt.setTimestamp(14, row.getAs[Timestamp]("time"))
        pstmt.setInt(15, row.getAs[Int]("version"))
        pstmt.setString(16, row.getAs[String]("pose"))
        pstmt.setInt(17, try {
          PreprocessUtils.getFilterFlag(
            JSON.parseArray(row.getAs[String]("pose"), classOf[XPose]).get(0),
            row getAs[Int] "quality")
        } catch {
          case _ => 0
        })
        pstmt.setTimestamp(18, row.getAs[Timestamp]("create_time"))
        pstmt.setTimestamp(19, row.getAs[Timestamp]("update_time"))
        pstmt.setInt(20, row.getAs[Int]("delete_flag"))
        pstmt.addBatch();
      }
      }
      pstmt.executeBatch();
      conn.commit();
    } catch {
      case e: Exception => {
        LOG.error("======> insert data failure", e)
        try {
          conn.rollback();
        } catch {
          case e: Exception => LOG.error("======> rollback failure", e);
        }
      }
    } finally {
      ConnectionUtils.closeResource(conn, pstmt, null)
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
    if (timeArray.length > 0) {
      // spark
      val spark = SparkSession
        .builder()
        //.master(sparkMaster)
        .appName(sparkAppName)
        .getOrCreate()

      // 根据时间从 t_timing_x 加载数据
      val jdbcDF = spark.read
        .jdbc(dbUrl, dbSourceTable, timeArray, prop)
      LOG.info("======> jdbcDF.rdd.partitions.size = {}", jdbcDF.rdd.partitions.length)

      // 将加载的数据保存到 预处理后的数据表 中
      jdbcDF.foreachPartition(partitionIter => insertDataFunc(partitionIter))

      spark.stop()
    }
    LOG.info("======> PreprocessJob speed time {}s", (System.currentTimeMillis() - beginTime) / 1000)
  }
}
