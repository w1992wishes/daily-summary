package me.w1992wishes.spark.etl.job2

import java.io.FileInputStream
import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.{Duration, LocalDateTime, ZoneId}
import java.util.Properties

import com.alibaba.fastjson.JSON
import com.typesafe.scalalogging.Logger
import me.w1992wishes.spark.etl.util.{ConnectionUtils, DateUtils, PreprocessUtils}
import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.{Row, SparkSession}

import scala.collection.mutable.ArrayBuffer

/**
  * 对增量数据做一些清洗、过滤、转化等预处理
  *
  * @author w1992wishes 2018/10/16 11:21
  */
object PreprocessJob {

  private[this] val LOG = Logger(this.getClass)

  val filePath = "config.properties"
  val properties = new Properties()
  properties.load(new FileInputStream(filePath))

  // spark 配置属性
  private[this] val sparkAppName = properties.getProperty("spark.appName")
  private[this] val sparkPartitions = Integer.parseInt(properties.getProperty("spark.partitions", "112"))

  // greenplum 配置属性
  private[this] val dbUrl = properties.getProperty("db.url")
  private[this] val dbDriver = properties.getProperty("db.driver")
  private[this] val dbUser = properties.getProperty("db.user")
  private[this] val dbPasswd = properties.getProperty("db.passwd")
  private[this] val dbSourceTable = properties.getProperty("db.source.table")
  private[this] val dbSinkTable = properties.getProperty("db.sink.table")
  private[this] val dbSourceFetchsize = properties.getProperty("db.source.fetchsize")
  private[this] val dbSinkBatchsize = properties.getProperty("db.sink.batchsize")

  // 程序配置
  private[this] val appMinPart = properties.getProperty("app.min.part").toInt
  // 质量属性好坏阈值
  private[this] val appStandardQuality = properties.getProperty("app.standard.quality").toFloat

  /**
    * 加载数据的起始时间
    *
    * @return
    */
  private def calculateStartTime(): LocalDateTime = {
    var conn: Connection = null
    var pstmt: PreparedStatement = null
    var rs: ResultSet = null
    try {
      // 先从时间记录表查询
      conn = ConnectionUtils.getConnection(dbUrl, dbUser, dbPasswd)
      var sql = s"select max(time) from $dbSinkTable"
      var start: Timestamp = new Timestamp(System.currentTimeMillis())
      pstmt = conn.prepareStatement(sql)
      rs = pstmt.executeQuery()
      if (rs.next() && rs.getTimestamp(1) != null) {
        start = rs.getTimestamp(1)
        LocalDateTime.ofInstant(start.toInstant, ZoneId.systemDefault())
      } else {
        sql = s"select min(time) from $dbSourceTable"
        pstmt = conn.prepareStatement(sql)
        rs = pstmt.executeQuery()
        if (rs.next() && rs.getTimestamp(1) != null) {
          start = rs.getTimestamp(1)
        }
        // 第一次查询，减去一秒，这样就不会遗留该图片
        LocalDateTime.ofInstant(start.toInstant, ZoneId.systemDefault()).minusSeconds(1)
      }
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
      var endTime: Timestamp = new Timestamp(System.currentTimeMillis())
      val sql = s"select max(time) from $dbSourceTable"
      pstmt = conn.prepareStatement(sql)
      rs = pstmt.executeQuery()
      if (rs.next() && rs.getTimestamp(1) != null) {
        endTime = rs.getTimestamp(1)
      }
      LocalDateTime.ofInstant(endTime.toInstant, ZoneId.systemDefault())
    } finally {
      ConnectionUtils.closeResource(conn, pstmt, rs)
    }
  }

  /**
    * 拆分时间段，用于 spark 并行查询
    *
    * @param start
    * @param end
    * @return
    */
  private[this] def predicates(start: LocalDateTime, end: LocalDateTime): Array[String] = {

    val timePart = ArrayBuffer[(String, String)]()
    // 查询的开始时间和结束时间的间隔秒数
    val durationSeconds: Long = Duration.between(start, end).toMillis / 1000
    // 如果分区数小于 指定的分钟（默认10分钟）划分，就按照指定的分钟划分设置分区，防止间隔内数据量很小却开很多的 task 去加载数据
    val minParts: Long = durationSeconds / (appMinPart * 60)
    if (sparkPartitions.longValue() < minParts) {
      val step = durationSeconds / (sparkPartitions - 1)
      for (x <- 1 until sparkPartitions) {
        timePart += DateUtils.dateTimeToStr(start.plusSeconds((x - 1) * step)) -> DateUtils.dateTimeToStr(start.plusSeconds(x * step))
      }
      timePart += DateUtils.dateTimeToStr(start.plusSeconds((sparkPartitions - 1) * step)) -> DateUtils.dateTimeToStr(end)
    } else if (durationSeconds.!=(0L)) {
      for (x <- 1 until minParts.toInt) {
        timePart += DateUtils.dateTimeToStr(start.plusMinutes((x - 1))) -> DateUtils.dateTimeToStr(start.plusMinutes(x))
      }
      timePart += DateUtils.dateTimeToStr(start.plusMinutes(minParts - 1)) -> DateUtils.dateTimeToStr(end)
    }

    timePart.map {
      case (start, end) =>
        s"time > '$start' AND time <= '$end'"
    }.toArray
  }

  /**
    * 分区执行批量插入
    *
    * @param iterator
    */
  private def insertDataFunc(iterator: Iterator[Row]) = {
    var conn: Connection = null
    var pstmt: PreparedStatement = null
    val sql = s"insert into $dbSinkTable(face_id, image_data, face_feature, from_image_id, " +
      s"gender, accessories, age, json, quality, race, source_id, source_type, locus, time, version, " +
      s"pose, feature_quality, create_time, update_time, delete_flag) " +
      s"values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    var i = 0
    try {
      conn = ConnectionUtils.getConnection(dbUrl, dbUser, dbPasswd)
      conn.setAutoCommit(false);
      pstmt = conn.prepareStatement(sql)
      iterator.foreach(row => {
        i += 1
        if (i > dbSinkBatchsize.toInt) {
          i = 0
          pstmt.executeBatch()
          pstmt.clearBatch()
        }
        pstmt.setLong(1, row.getAs[Long]("face_id"))
        pstmt.setString(2, row.getAs[String]("image_data"))
        pstmt.setBytes(3, row.getAs[Array[Byte]]("face_feature"))
        pstmt.setLong(4, row.getAs[Long]("from_image_id"))
        pstmt.setInt(5, row.getAs[Int]("gender"))
        pstmt.setInt(6, row.getAs[Int]("accessories"))
        pstmt.setInt(7, row.getAs[Int]("age"))
        pstmt.setString(8, row.getAs[String]("json"))
        val quality: Float = {
          row.getAs[Double]("quality").toFloat
        }
        pstmt.setFloat(9, quality)
        pstmt.setInt(10, row.getAs[Int]("race"))
        pstmt.setLong(11, row.getAs[Long]("source_id"))
        pstmt.setInt(12, row.getAs[Int]("source_type"))
        pstmt.setString(13, row.getAs[String]("locus"))
        pstmt.setTimestamp(14, row.getAs[Timestamp]("time"))
        pstmt.setInt(15, row.getAs[Int]("version"))
        val pose = row.getAs[String]("pose")
        pstmt.setString(16, pose)
        var featureQuality = 0.0f
        var xPose: XPose = null
        if (!StringUtils.isEmpty(pose)) {
          try {
            xPose = JSON.parseObject(pose, classOf[XPose])
            featureQuality = PreprocessUtils.getFilterFlag(xPose, quality, appStandardQuality)
          } catch {
            case _ => {
              featureQuality = -1
              LOG.error("======> json parse to XPose failure, json is {}", pose)
            }
          }
        } else {
          featureQuality = PreprocessUtils.getFilterFlag(quality, appStandardQuality)
        }
        pstmt.setFloat(17, featureQuality)
        pstmt.setTimestamp(18, row.getAs[Timestamp]("create_time"))
        pstmt.setTimestamp(19, row.getAs[Timestamp]("update_time"))
        pstmt.setInt(20, row.getAs[Int]("delete_flag"))
        pstmt.addBatch()
      })
      pstmt.executeBatch()
      conn.commit()
    } catch {
      case e: Exception => {
        LOG.error("======> insert data failure", e)
        try {
          conn.rollback()
        } catch {
          case e: Exception => LOG.error("======> rollback failure", e)
        }
      }
    } finally {
      ConnectionUtils.closeResource(conn, pstmt, null)
    }
  }

  def main(args: Array[String]): Unit = {

    // parse args
    val appArgs: AppArgs = new AppArgs(args)

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
        //.master("local")
        .appName(sparkAppName)
        .getOrCreate()

      // db 配置
      val prop = {
        // 设置连接用户&密码
        val prop = new Properties
        prop.setProperty("user", dbUser)
        prop.setProperty("password", dbPasswd)
        prop.setProperty("driver", dbDriver)
        prop.setProperty("fetchsize", dbSourceFetchsize)
        prop
      }
      LOG.info("======> sourceDF.rdd.partitions.size = {}", timeArray.length)
      // 根据时间从 t_timing_x 加载数据
      var sourceDF = spark.read
        .jdbc(dbUrl, dbSourceTable, timeArray, prop)

      // 将加载的数据保存到 预处理后的数据表 中
      if (appArgs.filterNightEnable) {
        val startTime = appArgs.startTime
        val endTime = appArgs.endTime
        val fun: Row => Boolean = {
          row => {
            val time = row.getAs[Timestamp]("time").toLocalDateTime
            val boo = (time.getHour >= startTime.hour && time.getMinute >= startTime.min && time.getSecond >= startTime.sec) ||
              (time.getHour <= endTime.hour && time.getMinute <= endTime.min && time.getSecond <= endTime.sec)
            !boo
          }
        }
        sourceDF.rdd
          .filter(fun)
          .foreachPartition(partitionIter => insertDataFunc(partitionIter))
      } else {
        sourceDF
          .foreachPartition(partitionIter => insertDataFunc(partitionIter))
      }

      LOG.info("======> PreprocessJob speed time {}s", (System.currentTimeMillis() - beginTime) / 1000)

      spark.stop()
    } else {
      LOG.info("======>PreprocessJob time array is empty")
    }
  }
}
