package me.w1992wishes.spark.etl.core

import java.sql.{Connection, Timestamp}
import java.time.{Duration, LocalDateTime}

import com.alibaba.fastjson.JSON
import me.w1992wishes.spark.etl.config.{CommandLineArgs, ConfigArgs}
import me.w1992wishes.spark.etl.model.{AgeInfo, PoseInfo}
import me.w1992wishes.spark.etl.util.{ConnectionUtils, Constants, DateUtils, PoseInfoUtils}
import org.apache.commons.lang.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.{SaveMode, SparkSession}

import scala.collection.mutable.ArrayBuffer

/**
  * 预处理 抽象类
  *
  * @author w1992wishes 2019/2/26 13:53
  */
abstract class PreProcess(commandLineArgs: CommandLineArgs) extends Serializable {

  protected val config: ConfigArgs = new ConfigArgs

  /**
    * 预处理过程
    */
  def preProcess(): Unit = {

    // 1.先准备开始时间、结束时间，统一格式为 yyyy-MM-dd HH:mm:ss
    val (startTimeStr, endTimeStr) = getTimeScope(commandLineArgs.preProcessStartTime, commandLineArgs.preProcessEndTime)
    // 转为 LocalDateTime
    val (startTime, endTime) = (LocalDateTime.parse(startTimeStr, DateUtils.dfDefault), LocalDateTime.parse(endTimeStr, DateUtils.dfDefault))

    // 2.先清除数据，防止数据重复
    clearDatas(startTimeStr, endTimeStr)

    // 3.调用 spark 并行预处理
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
  def invokeSpark(partitions: Int, timeConditions: Array[String]): Unit = {
    // spark dataframe to pojo
    //val FaceEventEncoder = Encoders.bean(classOf[FaceEvent])
    // 自定义新增 feature_quality 列
    val addFeatureQualityColFunc: ((Array[Byte], String, Float, Timestamp, String) => Float) = (faceFeature: Array[Byte], pose: String, quality: Float, time: Timestamp, ageInfo: String) =>
      getFeatureQuality(faceFeature, pose, quality, time, ageInfo)
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
          .option("dbtable", s"(SELECT * FROM ${config.preProcessTable} WHERE ${timeConditions(index)}) AS t_tmp_$index")
          .option("user", config.dbUser)
          .option("password", config.dbPasswd)
          .option("fetchsize", config.dbFetchsize)
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
      .withColumn("feature_quality", addFeatureQualityCol(col("feature_info"), col("pose_info"),
        col("quality_info"), col("time"), col("age_info")))
      .repartition(partitions, col("thumbnail_id"))
      .write
      .mode(SaveMode.Append)
      .format("jdbc")
      .option("driver", config.dbDriver)
      .option("url", config.dbUrl)
      .option("dbtable", config.preProcessedTable)
      .option("user", config.dbUser)
      .option("password", config.dbPasswd)
      .option("batchsize", config.dbBatchsize)
      .save()

    spark.stop()
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


  /**
    * 获取预处理开始时间和结束时间
    *
    * @param startTimeStr 开始时间字符 yyyyMMddHHmmss
    * @param endTimeStr   结束时间字符 yyyyMMddHHmmss
    * @return
    */
  protected def getTimeScope(startTimeStr: String, endTimeStr: String): (String, String) = {

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
    * 清除数据
    *
    * @param timeRange 起始时间 Tupple
    */
  protected def clearDatas(timeRange: (String, String)): Unit = {
    var conn: Connection = null
    try {
      conn = ConnectionUtils.getConnection(config.dbUrl, config.dbUser, config.dbPasswd)
      clearDataByTimeRange(timeRange, config.preProcessedTable, conn)
    } finally {
      ConnectionUtils.closeConnection(conn)
    }
  }


  /**
    * 根据时间范围清除预处理表中的数据
    *
    * @param sequence 起始时间 Tupple
    * @param table    表名
    * @param conn     数据库连接
    * @return
    */
  def clearDataByTimeRange(sequence: (String, String), table: String, conn: Connection)

  def getFeatureQuality(faceFeature: Array[Byte], pose: String, quality: Float, time: Timestamp, ageInfo: String): Float = {

    /**
      * 计算可聚档图片角度权重
      *
      * @param poseInfo 角度
      * @return
      */
    def calculatePoseWeight(poseInfo: PoseInfo): Float = {
      1 - (Math.abs(poseInfo.getPitch) / config.clusterPitchThreshold
        + Math.abs(poseInfo.getRoll) / config.clusterRollThreshold
        + Math.abs(poseInfo.getYaw) / config.clusterYawThreshold) / 3
    }

    /**
      * 计算特征值质量
      *
      * @param poseInfo   角度
      * @param quality 质量分值
      * @param time    抓拍时间
      * @return 特征值质量
      */
    def calculateFeatureQuality(poseInfo: PoseInfo)(quality: Float)(time: Timestamp)(ageInfo: String): Float = {
      var featureQuality = .0f
      if (ageFilter(ageInfo)) {
        if (clusterQualityFilter(quality) && clusterPoseFilter(poseInfo) && timeFilter(time)) {
          featureQuality = quality * calculatePoseWeight(poseInfo).formatted("%.4f").toFloat
        } else if (classQualityFilter(quality) && classPoseFilter(poseInfo)) {
          featureQuality = Constants.CLASS_QUALITY
        } else {
          featureQuality = Constants.UNCLASS_QUALITY
        }
      } else {
        featureQuality = Constants.CHILDREN_QUALITY
      }
      featureQuality
    }

    def clusterPoseFilter(poseInfo: PoseInfo): Boolean = PoseInfoUtils.inAngle(poseInfo, config.clusterPitchThreshold, config.clusterRollThreshold, config.clusterYawThreshold)

    def classPoseFilter(poseInfo: PoseInfo): Boolean = PoseInfoUtils.inAngle(poseInfo, config.classPitchThreshold, config.classRollThreshold, config.classYawThreshold)

    def clusterQualityFilter(quality: Float): Boolean = quality >= config.clusterQualityThreshold

    def classQualityFilter(quality: Float): Boolean = quality >= config.classQualityThreshold

    // 夜间照片过滤，不能用作聚档
    def timeFilter(time: Timestamp): Boolean = {
      val startTime = config.filterStartTime
      val endTime = config.filterEndTime
      if (config.filterNightEnable && startTime != null && endTime != null) {
        val dateTime = time.toLocalDateTime
        val boo = (dateTime.getHour >= startTime.hour && dateTime.getMinute >= startTime.min && dateTime.getSecond >= startTime.sec) ||
          (dateTime.getHour <= endTime.hour && dateTime.getMinute <= endTime.min && dateTime.getSecond <= endTime.sec)
        !boo
      } else {
        true
      }
    }

    // 年龄过滤
    def ageFilter(ageInfo: String): Boolean = {
      try {
        JSON.parseObject(ageInfo, classOf[AgeInfo]).getValue > config.realAgeThreshold
      } catch {
        case e: Throwable =>
          println(s"======> parse json to AgeInfo POJO failure! Exception is $e")
          false
      }
    }

    var poseInfo: PoseInfo = null
    var featureQuality = 0.0f
    if (StringUtils.isNotEmpty(pose) && ArrayUtils.isNotEmpty(faceFeature) && time != null) {
      try {
        poseInfo = JSON.parseObject(pose, classOf[PoseInfo])
        featureQuality = calculateFeatureQuality(poseInfo)(quality)(time)(ageInfo)
      } catch {
        case _: Throwable =>
          featureQuality = Constants.USELESS_QUALITY
          println(s"======> json parse to XPose failure, json is $pose")
      }
    } else {
      featureQuality = Constants.USELESS_QUALITY
    }
    featureQuality.formatted("%.4f").toFloat
  }
}