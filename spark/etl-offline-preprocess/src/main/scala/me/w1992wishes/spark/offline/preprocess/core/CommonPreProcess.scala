package me.w1992wishes.spark.offline.preprocess.core

import java.sql.Timestamp
import java.time.LocalDateTime

import me.w1992wishes.spark.offline.preprocess.config.{CommandLineArgs, ConfigArgs}
import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * 预处理 抽象类
  *
  * @author w1992wishes 2019/2/26 13:53
  */
abstract class CommonPreProcess(commandLineArgs: CommandLineArgs) extends PreProcess with CalculateQualityFunc {

  // 配置属性
  protected val config: ConfigArgs = new ConfigArgs(commandLineArgs.confName)

  // 预处理前的表
  protected val preProcessTable: String = getPreProcessTable

  // 预处理后的表
  protected val preProcessedTable: String = getPreProcessedTable

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
    // 调用 spark 并行预处理
    val partitions = commandLineArgs.partitions
    if (partitions <= 0) {
      println("****** partition can not smaller than 0. ******")
      System.exit(1)
    }
    // 获取分区查询条件
    val paralleledCondition = getParalleledCondition(partitions)
    invokeSpark(partitions, paralleledCondition)
  }

  /**
    * spark 并行执行
    *
    * @param partitions          分区数
    * @param paralleledCondition 分区条件
    */
  protected def invokeSpark(partitions: Int, paralleledCondition: Array[String]): Unit = {
    // spark dataframe to pojo
    // val FaceEventEncoder = Encoders.bean(classOf[FaceEvent])
    // 自定义新增 feature_quality 列
    val addFeatureQualityColFunc: ((Array[Byte], String, Float) => Float) = (faceFeature: Array[Byte], pose: String, quality: Float) =>
      calculateFeatureQuality(config)(faceFeature, pose, quality)
    val addFeatureQualityCol = udf(addFeatureQualityColFunc)
    val addTimeColFunc: (Timestamp => Timestamp) = _ => Timestamp.valueOf(LocalDateTime.now().withNano(0))
    val addTimeCol = udf(addTimeColFunc)

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
          .option("dbtable", s"(SELECT * FROM $preProcessTable WHERE ${paralleledCondition(index)}) AS t_tmp_$index")
          .option("user", config.sourceUser)
          .option("password", config.sourcePasswd)
          .option("fetchsize", config.sourceFetchsize)
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
      .withColumn("feature_quality",
        addFeatureQualityCol(col("feature_info"), col("pose_info"), col("quality_info")))
      .withColumn("create_time", addTimeCol(col("create_time")))
      .withColumn("save_time", addTimeCol(col("create_time")))
      //.repartition(partitions, col("thumbnail_id"))
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
    * 获取 spark 并行查询的条件数组
    *
    * @param realPartitions 实际分区数
    * @return
    */
  protected def getParalleledCondition(realPartitions: Int): Array[String] = {

    // 转换为 spark sql where 字句中的查询条件
    Range(0, realPartitions).map (partition => s"CAST(thumbnail_id as numeric) % $realPartitions = $partition").toArray
  }

}