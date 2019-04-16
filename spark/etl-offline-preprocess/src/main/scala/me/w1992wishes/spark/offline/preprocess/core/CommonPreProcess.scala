package me.w1992wishes.spark.offline.preprocess.core

import java.sql.Timestamp
import java.time.{Duration, LocalDateTime}

import me.w1992wishes.spark.offline.preprocess.config.{CommandLineArgs, ConfigArgs}
import me.w1992wishes.common.util.DateUtils
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

  // 预处理前的表
  protected val preProcessTable: String = getPreProcessTable

  // 预处理后的表
  protected val preProcessedTable: String = getPreProcessedTable

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
    val (startTime, endTime) = (LocalDateTime.parse(startTimeStr, DateUtils.DF_NORMAL), LocalDateTime.parse(endTimeStr, DateUtils.DF_NORMAL))
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
    // val FaceEventEncoder = Encoders.bean(classOf[FaceEvent])
    // 自定义新增 feature_quality 列
    val addFeatureQualityColFunc: ((Array[Byte], String, Float) => Float) = (faceFeature: Array[Byte], pose: String, quality: Float) =>
      calculateFeatureQuality(config)(faceFeature, pose, quality)
    val addFeatureQualityCol = udf(addFeatureQualityColFunc)
    val addSaveTimeColFunc: (Timestamp => Timestamp) = _ => Timestamp.valueOf(LocalDateTime.now().withNano(0))
    val addSaveTimeCol = udf(addSaveTimeColFunc)

    // 创建 spark
    var spark: SparkSession = null
    if (commandLineArgs.isLocal) {
      spark = SparkSession
        .builder()
        .master("local")
        .appName(config.sparkAppName)
        .getOrCreate()
    } else {
      spark = SparkSession
        .builder()
        //.master("local")
        .appName(config.sparkAppName)
        .getOrCreate()
    }

    // 并行处理
    Range(0, partitions)
      .map(index => {
        spark
          .read
          .format("jdbc")
          .option("driver", config.sourceDriver)
          .option("url", config.sourceUrl)
          .option("dbtable", s"(SELECT * FROM $preProcessTable WHERE ${timeConditions(index)}) AS t_tmp_$index")
          .option("user", config.sourceUser)
          .option("password", config.sourcePasswd)
          .option("fetchsize", config.sourceFetchsize)
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
      .withColumn("feature_quality",
        addFeatureQualityCol(col("feature_info"), col("pose_info"), col("quality_info")))
      .withColumn("save_time",
        addSaveTimeCol(col("create_time")))
      .repartition(partitions, col("thumbnail_id"))
      .write
      .mode(getSaveMode)
      .format("jdbc")
      .option("driver", config.sinkDriver)
      .option("url", config.sinkUrl)
      .option("dbtable", preProcessedTable)
      .option("user", config.sinkUser)
      .option("password", config.sinkPasswd)
      .option("batchsize", config.sinkBatchsize)
      .save()

    spark.stop()
  }

  /**
    * spark sql save mode
    *
    * @return
    */
  def getSaveMode: SaveMode

  /**
    * 获取预处理开始时间和结束时间
    *
    * @param startTimeStr 开始时间字符 yyyyMMddHHmmss
    * @param endTimeStr   结束时间字符 yyyyMMddHHmmss
    * @return
    */
  def getTimeScope(startTimeStr: String, endTimeStr: String): (String, String)

  // 预处理前的表
  def getPreProcessTable: String =
    if (StringUtils.isNotEmpty(commandLineArgs.preProcessTable))
      commandLineArgs.preProcessTable
    else
      config.sourceTable

  // 预处理后的表
  def getPreProcessedTable: String =
    if (StringUtils.isNotEmpty(commandLineArgs.preProcessedTable))
      commandLineArgs.preProcessedTable
    else
      config.sinkTable

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
    // 最大分区数 最大分区数按照 minutesPerPartition（默认5分钟）划分，防止间隔内数据量很小却开很多的 task 去加载数据
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
    val step = if (realPartitions > 1) durationSeconds / (realPartitions - 1) else 1
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