package me.w1992wishes.spark.etl.demo

import com.alibaba.fastjson.JSON
import me.w1992wishes.spark.etl.config.ConfigArgs
import me.w1992wishes.spark.etl.model.PoseInfo
import me.w1992wishes.spark.etl.util.PoseInfoUtils
import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * @author w1992wishes 2019/2/26 15:56
  */
object AddColumnExample {
  private val configArgs = new ConfigArgs

  def main(args: Array[String]): Unit = {

    // spark
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("test")
      .getOrCreate()

    val code :((String, Float) => Float) = (pose: String, quality: Float) => getFeatureQuality(pose, quality)

    import org.apache.spark.sql.functions._

    val addCol = udf(code)

    val df = spark
      .read
      .format("jdbc")
      .option("driver", configArgs.dbDriver)
      .option("url", configArgs.dbUrl)
      .option("dbtable", configArgs.preProcessTable)
      .option("user", configArgs.dbUser)
      .option("password", configArgs.dbPasswd)
      .option("fetchsize", configArgs.dbFetchsize)
      .load()

    df.withColumn("feature_quality", addCol(col("pose_info"), col("quality_info")))
      .filter(row => row.getAs[Float]("feature_quality") >= -1.0)
      .write
      .mode(SaveMode.Append)
      .format("jdbc")
      .option("driver", configArgs.dbDriver)
      .option("url", configArgs.dbUrl)
      .option("dbtable", configArgs.preProcessedTable)
      .option("user", configArgs.dbUser)
      .option("password", configArgs.dbPasswd)
      .save()
  }

  private def getFeatureQuality(pose: String, quality: Float): Float = {

    /**
      * 计算可建档图片角度权重
      *
      * @param poseInfo 角度
      * @return
      */
    def calculatePoseWeight(poseInfo: PoseInfo): Float = {
      1 - (Math.abs(poseInfo.getPitch) / configArgs.clusterPitchThreshold
        + Math.abs(poseInfo.getRoll) / configArgs.clusterRollThreshold
        + Math.abs(poseInfo.getYaw) / configArgs.clusterYawThreshold) / 3
    }

    /**
      * 计算特征值质量
      *
      * @param xPose   角度
      * @param quality 质量分值
      * @return 特征值质量
      */
    def calculateFeatureQuality(xPose: PoseInfo)(quality: Float): Float = {
      var featureQuality = .0f
      if (clusterQualityFilter(quality) && clusterPoseFilter(xPose)) {
        featureQuality = quality * calculatePoseWeight(xPose)
      } else if (classQualityFilter(quality) && classPoseFilter(xPose)) {
        featureQuality = -1.0f
      } else {
        featureQuality = -2.0f
      }
      featureQuality
    }

    def clusterPoseFilter(xPose: PoseInfo): Boolean = PoseInfoUtils.inAngle(xPose, configArgs.clusterPitchThreshold, configArgs.clusterRollThreshold, configArgs.clusterYawThreshold)

    def classPoseFilter(xPose: PoseInfo): Boolean = PoseInfoUtils.inAngle(xPose, configArgs.classPitchThreshold, configArgs.classRollThreshold, configArgs.classYawThreshold)

    def clusterQualityFilter(quality: Float): Boolean = quality >= configArgs.clusterQualityThreshold

    def classQualityFilter(quality: Float): Boolean = quality >= configArgs.classQualityThreshold

    var xPose: PoseInfo = null
    var featureQuality = 0.0f
    if (!StringUtils.isEmpty(pose)) {
      try {
        xPose = JSON.parseObject(pose, classOf[PoseInfo])
        featureQuality = calculateFeatureQuality(xPose)(quality)
      } catch {
        case _: Throwable =>
          featureQuality = -2.0f
          println("======> json parse to PoseInfo failure, json is {}", pose)
      }
    } else {
      featureQuality = -2.0f
    }
    featureQuality.formatted("%.2f").toFloat
  }
}
