package me.w1992wishes.spark.etl.job2

import java.io.BufferedInputStream
import java.sql.{Connection, PreparedStatement, Timestamp}
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
  private[this] def predicates(start: LocalDateTime, end: LocalDateTime) = {

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
  def dbProperties() = {
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
    * @param spark
    * @return
    */
  def calculateStartTime(spark: SparkSession) = {
    val prop = dbProperties()

    // 获取已经处理的最大抓拍时间
    var dataFrame = spark.read
      .jdbc(dbUrl, dbSinkTable, prop)
    dataFrame.createOrReplaceTempView("t_timing_x_preprocess")
    var startDF = spark.sql("select max(time) from t_timing_x_preprocess")

    // 为空则说明系统还没有加载数据
    if (startDF.rdd.first().get(0) == null) {
      dataFrame = spark.read
        .jdbc(dbUrl, dbSourceTable, prop)
      dataFrame.createOrReplaceTempView("t_timing_x")
      startDF = spark.sql("select min(time) from t_timing_x")
    }

    startDF
      .rdd
      .map(row => {
        LocalDateTime.ofInstant(row.getTimestamp(0).toInstant, ZoneId.systemDefault()).withSecond(0).withNano(0)
      })
      .first()
  }

  /**
    * 并行加载数据的结束时间
    *
    * @param spark
    * @return
    */
  def calculateEndTime(spark: SparkSession) = {
    val prop = dbProperties()

    val dataFrame = spark
      .read
      .jdbc(dbUrl, dbSourceTable, prop)
    dataFrame.createOrReplaceTempView("t_timing_x")

    spark.sql("select max(time) from t_timing_x")
      .rdd
      .map(row => {
        LocalDateTime.ofInstant(row.getTimestamp(0).toInstant, ZoneId.systemDefault()).withSecond(0).withNano(0)
      })
      .first()
  }

  /**
    * 分区执行批量插入
    *
    * @param iterator
    */
  def insertDataFunc(iterator: Iterator[Row]) = {
    var conn: Connection = null
    var pstmt: PreparedStatement = null
    val sql = "insert into " + dbSinkTable + "(fid, cid, gid, person_id, created, updated, face_id, face_url," +
      "feature, from_image_id, gender, accessories, age, quality, race, source_id, locus, source_type, time, version," +
      "pose, filtertag) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
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
        pstmt.setLong(1, row.getAs[Long]("fid"))
        pstmt.setLong(2, row.getAs[Long]("cid"))
        pstmt.setLong(3, row.getAs[Long]("gid"))
        pstmt.setLong(4, row.getAs[Long]("person_id"))
        pstmt.setTimestamp(5, row.getAs[Timestamp]("created"))
        pstmt.setTimestamp(6, row.getAs[Timestamp]("updated"))
        pstmt.setString(7, row.getAs[String]("face_id"))
        pstmt.setString(8, row.getAs[String]("face_url"))
        pstmt.setBytes(9, row.getAs[Array[Byte]]("feature"))
        pstmt.setString(10, row.getAs[String]("from_image_id"))
        pstmt.setShort(11, row.getAs[Int]("gender").toShort)
        pstmt.setShort(12, row.getAs[Int]("accessories").toShort)
        pstmt.setShort(13, row.getAs[Int]("age").toShort)
        pstmt.setInt(14, row.getAs[Int]("quality"))
        pstmt.setShort(15, row.getAs[Int]("race").toShort)
        pstmt.setLong(16, row.getAs[Long]("source_id"))
        pstmt.setString(17, row.getAs[String]("locus"))
        pstmt.setInt(18, row.getAs[Int]("source_type"))
        pstmt.setTimestamp(19, row.getAs[Timestamp]("time"))
        pstmt.setInt(20, row.getAs[Int]("version"))
        pstmt.setString(21, row.getAs[String]("pose"))
        pstmt.setShort(22, try {
          PreprocessUtils.getFilterFlag(JSON.parseArray(row.getAs[String]("pose"), classOf[XPose]).get(0), row getAs[Int] "quality")
        } catch {
          case _ => 0
        })
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
    // spark
    val spark = SparkSession
      .builder()
      //.master(sparkMaster)
      .appName(sparkAppName)
      .getOrCreate()

    // db 配置
    val prop = dbProperties()
    // 并行加载数据的 起始时间
    val start = calculateStartTime(spark)
    // 并行加载数据的 结束时间
    val end = calculateEndTime(spark)

    val beginTime = System.currentTimeMillis()
    // 根据时间从 t_timing_x 加载数据
    val jdbcDF = spark.read
      .jdbc(dbUrl, dbSourceTable, predicates(start, end), prop)
    LOG.info("======> jdbcDF.rdd.partitions.size = {} \n", jdbcDF.rdd.partitions.length)

    // 将加载的数据保存到 预处理后的数据表 中
    jdbcDF.foreachPartition(partitionIter => insertDataFunc(partitionIter))

    LOG.info("======> PreprocessJob speed time {}s", (System.currentTimeMillis() - beginTime) / 1000)
    spark.stop()
  }
}
