package me.w1992wishes.spark.etl.core

import java.sql.Timestamp
import java.time.{Duration, LocalDateTime}

import me.w1992wishes.spark.etl.config.{CommandLineArgs, ConfigArgs}
import me.w1992wishes.spark.etl.util.DateUtils
import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.{SaveMode, SparkSession}

import scala.collection.mutable.ArrayBuffer

/**
  * 预处理 抽象类
  *
  * @author w1992wishes 2019/2/26 13:53
  */
abstract class CommonPreProcess(commandLineArgs: CommandLineArgs) extends PreProcess with CalculateQualityFunc {

  // 配置属性
  protected val config: ConfigArgs = new ConfigArgs

  // 本次处理的开始时间和结束时间
  protected val (startTimeStr, endTimeStr) = getTimeScope(commandLineArgs.preProcessStartTime, commandLineArgs.preProcessEndTime)

  /**
    * 全预处理过程流程
    */
  def wholePreProcess(): Unit = {
    preProcessBefore()
    preProcess()
    preProcessPost()
  }

  /**
    * 预处理过程
    */
  def preProcess(): Unit = {
    // 转为 LocalDateTime
    val (startTime, endTime) = (LocalDateTime.parse(startTimeStr, DateUtils.dfDefault), LocalDateTime.parse(endTimeStr, DateUtils.dfDefault))
    // 调用 spark 并行预处理
    // 获取实际分区
    val partitions = getPartitions(startTime, endTime)(commandLineArgs.partitions, commandLineArgs.minutesPerPartition)
    // 获取分区查询条件
    val timeConditions = getParalleledTimes(startTime, endTime, partitions)
    invokeSpark(partitions, timeConditions)
  }

  /**
    * spark 并行执行
    *
    * @param partitions     分区数
    * @param timeConditions 分区条件
    */
  protected def invokeSpark(partitions: Int, timeConditions: Array[String]): Unit = {
    // spark dataframe to pojo
    //val FaceEventEncoder = Encoders.bean(classOf[FaceEvent])
    // 自定义新增 feature_quality 列
    val addFeatureQualityColFunc: ((Array[Byte], String, Float, Timestamp, String) => Float) = (faceFeature: Array[Byte], pose: String, quality: Float, time: Timestamp, ageInfo: String) =>
      calculateFeatureQuality(config)(faceFeature, pose, quality, time, ageInfo)
    val addFeatureQualityCol = udf(addFeatureQualityColFunc)

    // 创建 spark
    val spark = SparkSession
      .builder()
      //.master("local")
      .appName(config.sparkAppName)
      .getOrCreate()

    // 并行处理
    Range(0, partitions)
      .map(index => {
        spark
          .read
          .format("jdbc")
          .option("driver", config.dbDriver)
          .option("url", config.dbUrl)
          .option("dbtable", s"(SELECT * FROM ${config.sourceTable} WHERE ${timeConditions(index)}) AS t_tmp_$index")
          .option("user", config.dbUser)
          .option("password", config.dbPasswd)
          .option("fetchsize", config.dbFetchsize)
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
      .drop("id")
      .withColumn("feature_quality", addFeatureQualityCol(col("feature_info"), col("pose_info"),
        col("quality_info"), col("time"), col("age_info")))
      .repartition(partitions, col("thumbnail_id"))
      .write
      .mode(SaveMode.Append)
      .format("jdbc")
      .option("driver", config.dbDriver)
      .option("url", config.dbUrl)
      .option("dbtable", config.sinkTable)
      .option("user", config.dbUser)
      .option("password", config.dbPasswd)
      .option("batchsize", config.dbBatchsize)
      .save()

    spark.stop()
  }

  /**
    * 获取预处理开始时间和结束时间
    *
    * @param startTimeStr 开始时间字符 yyyyMMddHHmmss
    * @param endTimeStr   结束时间字符 yyyyMMddHHmmss
    * @return
    */
  private def getTimeScope(startTimeStr: String, endTimeStr: String): (String, String) = {

    val start = if (StringUtils.isEmpty(startTimeStr))
    // 当前日期的前一天的 0时0分0秒
      DateUtils.dateTimeToStr(LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0))
    else {
      DateUtils.dateTimeToStr(LocalDateTime.parse(startTimeStr, DateUtils.dfNoLine))
    }

    val end = if (StringUtils.isEmpty(endTimeStr))
    // 当前日期的 0时0分0秒
      DateUtils.dateTimeToStr(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0))
    else
      DateUtils.dateTimeToStr(LocalDateTime.parse(endTimeStr, DateUtils.dfNoLine))

    (start, end)
  }

  /**
    * 计算真实分区数
    *
    * @param startTime           开始时间
    * @param endTime             结束时间
    * @param partitions          设置的分区数
    * @param minutesPerPartition 每个分区最少查询的时间间隔，单位分钟
    * @return
    */
  protected def getPartitions(startTime: LocalDateTime, endTime: LocalDateTime)(partitions: Int, minutesPerPartition: Int): Int = {
    // 查询的开始时间和结束时间的间隔秒数
    val durationSeconds: Long = Duration.between(startTime, endTime).toMillis / 1000
    // 最大分区数 最大分区数按照 minutesPerPartition（默认30分钟）划分，防止间隔内数据量很小却开很多的 task 去加载数据
    val maxPartitions: Int = (durationSeconds / (minutesPerPartition * 60)).intValue() + 1

    if (partitions > maxPartitions) maxPartitions else partitions
  }

  /**
    * 获取 spark 并行查询的时间条件数组
    *
    * @param startTime      并行的开始时间
    * @param endTime        并行的结束时间
    * @param realPartitions 实际分区数
    * @return
    */
  protected def getParalleledTimes(startTime: LocalDateTime, endTime: LocalDateTime, realPartitions: Int): Array[String] = {

    val timePart = ArrayBuffer[(String, String)]()

    // 查询的开始时间和结束时间的间隔秒数
    val durationSeconds: Long = Duration.between(startTime, endTime).toMillis / 1000
    // 计算每个分区实际提取数据的时间间隔
    val step = durationSeconds / (realPartitions - 1)
    for (x <- 1 until realPartitions) {
      timePart += DateUtils.dateTimeToStr(startTime.plusSeconds((x - 1) * step)) -> DateUtils.dateTimeToStr(startTime.plusSeconds(x * step))
    }
    timePart += DateUtils.dateTimeToStr(startTime.plusSeconds((realPartitions - 1) * step)) -> DateUtils.dateTimeToStr(endTime)

    // 转换为 spark sql where 字句中的查询条件
    timePart.map {
      case (start, end) =>
        s"create_time > '$start' AND create_time <= '$end'"
    }.toArray
  }
}