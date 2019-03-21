package me.w1992wishes.spark.etl.core

import java.sql.Timestamp

import com.alibaba.fastjson.JSON
import me.w1992wishes.spark.etl.config.ConfigArgs
import me.w1992wishes.spark.etl.model.{AgeInfo, PoseInfo}
import me.w1992wishes.spark.etl.util.{Constants, PoseInfoUtils}
import org.apache.commons.lang.ArrayUtils
import org.apache.commons.lang3.StringUtils

/**
  * @author w1992wishes 2019/3/1 20:14
  */
trait CalculateQualityFunc extends Serializable{

  /**
    * 计算特征值质量
    *
    * @param config      配置属性
    * @param featureInfo 特征值信息
    * @param poseInfo    角度信息
    * @param qualityInfo 质量信息
    * @param time        抓拍时间
    * @param ageInfo     年龄信息
    * @return
    */
  def calculateFeatureQuality(config: ConfigArgs)
                             (featureInfo: Array[Byte], poseInfo: String, qualityInfo: Float, time: Timestamp, ageInfo: String): Float = {

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
      * @param poseInfo 角度
      * @param quality  质量分值
      * @param time     抓拍时间
      * @return 特征值质量
      */
    def calculateFeatureQuality(poseInfo: PoseInfo)(quality: Float)(time: Timestamp)(ageInfo: String): Float = {
      var featureQuality = .0f
      if (ageFilter(ageInfo)) {
        if (clusterQualityFilter(quality) && clusterPoseFilter(poseInfo) && timeFilter(time)) {
          featureQuality = quality * calculatePoseWeight(poseInfo)
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

    // 年龄过滤
    def ageFilter(ageInfo: String): Boolean = {
      if (!config.filterAgeEnable) {
        try {
          JSON.parseObject(ageInfo, classOf[AgeInfo]).getValue > config.realAgeThreshold
        } catch {
          case e: Throwable =>
            println(s"======> parse json to AgeInfo POJO failure! Exception is $e")
            false
        }
      } else {
        true
      }
    }

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

    def clusterPoseFilter(poseInfo: PoseInfo): Boolean = PoseInfoUtils.inAngle(poseInfo, config.clusterPitchThreshold, config.clusterRollThreshold, config.clusterYawThreshold)

    def classPoseFilter(poseInfo: PoseInfo): Boolean = PoseInfoUtils.inAngle(poseInfo, config.classPitchThreshold, config.classRollThreshold, config.classYawThreshold)

    def clusterQualityFilter(quality: Float): Boolean = quality >= config.clusterQualityThreshold

    def classQualityFilter(quality: Float): Boolean = quality >= config.classQualityThreshold

    var poseInfoModel: PoseInfo = null
    var featureQuality = 0.0f
    if (StringUtils.isNotEmpty(poseInfo) && ArrayUtils.isNotEmpty(featureInfo) && time != null) {
      try {
        poseInfoModel = JSON.parseObject(poseInfo, classOf[PoseInfo])
        featureQuality = calculateFeatureQuality(poseInfoModel)(qualityInfo)(time)(ageInfo)
      } catch {
        case _: Throwable =>
          featureQuality = Constants.USELESS_QUALITY
          println(s"======> json parse to XPose failure, json is $poseInfo")
      }
    } else {
      featureQuality = Constants.USELESS_QUALITY
    }
    featureQuality.formatted("%.4f").toFloat
  }

}
