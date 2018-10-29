package me.w1992wishes.spark.etl.job3

import java.io.BufferedInputStream
import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.{Duration, LocalDateTime, ZoneId}
import java.util.{Date, Properties}

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import com.typesafe.scalalogging.Logger
import me.w1992wishes.spark.etl.util.{ConnectionUtils, DateUtils}
import org.apache.spark.sql.SparkSession

import scala.collection.mutable.ArrayBuffer

/**
  * @author w1992wishes 2018/10/24 11:21
  */
object ExtractForAssociation {
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
  private[this] val dbFaceTable = properties.getProperty("db.face.table")
  private[this] val dbTimeTable = properties.getProperty("db.time.table")
  private[this] val dbTmpTable = properties.getProperty("db.tmp.table")
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
      var sql = "select time from " + dbTimeTable + " where job='ExtractForAssociation'"
      var start: Timestamp = new Timestamp(System.currentTimeMillis())
      pstmt = conn.prepareStatement(sql)
      rs = pstmt.executeQuery()
      if (rs.next()) {
        start = rs.getTimestamp(1)
      } else {
        sql = "select min(time) from " + dbFaceTable
        pstmt = conn.prepareStatement(sql)
        rs = pstmt.executeQuery()
        if (rs.next() && rs.getTimestamp(1) != null) {
          start = rs.getTimestamp(1)
        }
      }
      LocalDateTime.ofInstant(start.toInstant, ZoneId.systemDefault()).withSecond(0).withNano(0)
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
      var sql = "select max(time) from " + dbFaceTable
      var end: Timestamp = new Timestamp(System.currentTimeMillis())
      pstmt = conn.prepareStatement(sql)
      rs = pstmt.executeQuery()
      if (rs.next() && rs.getTimestamp(1) != null) {
        end = rs.getTimestamp(1)
      }
      LocalDateTime.ofInstant(end.toInstant, ZoneId.systemDefault()).withSecond(0).withNano(0)
    } finally {
      ConnectionUtils.closeResource(conn, pstmt, rs)
    }
  }

  /**
    * 更新记录时间
    *
    * @param end
    */
  private def updateTimeRecord(end: Timestamp): Unit = {
    var conn: Connection = null
    var pstmt: PreparedStatement = null
    var rs: ResultSet = null
    try {
      conn = ConnectionUtils.getConnection(dbUrl, dbUser, dbPasswd)
      val sql = "select time from " + dbTimeTable + " where job = 'ExtractForAssociation'"
      pstmt = conn.prepareStatement(sql)
      rs = pstmt.executeQuery()
      if (!rs.next()) {
        val sql = "insert into " + dbTimeTable + "(time, job, create_time, update_time, delete_flag) " +
          "values(?, ?, ?, ?, ?)"
        pstmt = conn.prepareStatement(sql)
        pstmt.setTimestamp(1, end)
        pstmt.setString(2, "ExtractForAssociation")
        pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()))
        pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()))
        pstmt.setInt(5, 0)
        pstmt.execute()
      } else {
        val sql = "update " + dbTimeTable + " set time = ?, update_time = ? where job = 'ExtractForAssociation'"
        pstmt = conn.prepareStatement(sql)
        pstmt.setTimestamp(1, end)
        pstmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()))
        pstmt.execute()
      }
    } catch {
      case e: Exception => {
        LOG.error("======> update time record failure", e)
        try {
          conn.rollback();
        } catch {
          case e: Exception => LOG.error("======> rollback failure", e);
        }
      }
    } finally {
      ConnectionUtils.closeResource(conn, pstmt, rs)
    }
  }

  /**
    * truncate table
    *
    * @return
    */
  private def truncateTable(): Unit = {
    var conn: Connection = null
    var pstmt: PreparedStatement = null
    var rs: ResultSet = null
    try {
      conn = ConnectionUtils.getConnection(dbUrl, dbUser, dbPasswd)
      val sql = "truncate " + dbTmpTable
      pstmt = conn.prepareStatement(sql)
      pstmt.execute()
    } catch {
      case e: Exception => {
        LOG.error("======> truncate table failure", e)
        try {
          conn.rollback();
        } catch {
          case e: Exception => LOG.error("======> rollback failure", e);
        }
      }
    } finally {
      ConnectionUtils.closeResource(conn, pstmt, rs)
    }
  }

  /**
    * 用于映射的实体表的类，只取部分字段
    *
    * @param archiveId
    * @param time
    * @param locus
    */
  private case class TmpArchiveX(archiveId: String, time: Timestamp, locus: String) {
    def getArchiveId = archiveId

    def getTime = time

    def getLocus = locus
  }

  /**
    * 档案中间存量表 对应 实体类
    *
    * @param archive_id
    * @param face_json
    * @param create_time
    * @param update_time
    * @param delete_flag
    */
  private case class TmpArchiveFace(archive_id: String, face_json: String, create_time: Timestamp, update_time: Timestamp, delete_flag: Integer)

  def main(args: Array[String]): Unit = {

    // db 配置
    val prop = dbProperties()
    // 并行加载数据的 起始时间
    val start = calculateStartTime()
    // 并行加载数据的 结束时间
    val end = calculateEndTime()
    // 用于并行加载数据的 时间范围数组
    val timeArray = predicates(start, end)

    // 每次运行前先清除之前的数据
    truncateTable()

    val beginTime = System.currentTimeMillis()
    if (timeArray.length > 0) {
      // spark
      val spark = SparkSession
        .builder()
        .master(sparkMaster)
        .appName(sparkAppName)
        .getOrCreate()
      // 根据时间从数据源加载数据
      val jdbcDF = spark.read
        .jdbc(dbUrl, dbFaceTable, timeArray, prop)
      LOG.info("======> jdbcDF.rdd.partitions.size = {}", jdbcDF.rdd.partitions.length)

      import spark.implicits._

      jdbcDF
        .rdd
        .map(row => TmpArchiveX(row.getAs("archive_id"), row.getAs("time"), row.getAs("locus")))
        .groupBy(archive => archive.archiveId)
        .map(group => TmpArchiveFace(group._1,
          JSON.toJSONString(group._2.toArray, SerializerFeature.WriteMapNullValue),
          new Timestamp(System.currentTimeMillis()),
          new Timestamp(System.currentTimeMillis()),
          0)
        )
        .toDF()
        .write
        .mode("append")
        .format("jdbc")
        .options(
          Map("driver" -> dbDriver,
            "url" -> dbUrl,
            "dbtable" -> dbTmpTable,
            "user" -> dbUser,
            "password" -> dbPasswd,
            "batchsize" -> dbBatchsize)
        )
        .save()

      spark.stop()
    }
    LOG.info("======> ExtractForAssociation speed time {}s", (System.currentTimeMillis() - beginTime) / 1000)

    // 更新记录时间
    updateTimeRecord(new Timestamp(Date.from(end.atZone(ZoneId.systemDefault()).toInstant).getTime))
  }
}
