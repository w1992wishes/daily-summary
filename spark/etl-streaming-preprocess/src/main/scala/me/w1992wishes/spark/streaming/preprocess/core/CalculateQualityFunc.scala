package me.w1992wishes.spark.streaming.preprocess.core

import com.alibaba.fastjson.JSON
import me.w1992wishes.spark.streaming.preprocess.config.ConfigArgs
import me.w1992wishes.spark.streaming.preprocess.model.PoseInfo
import me.w1992wishes.spark.streaming.preprocess.util.{Constants, PoseInfoUtils}
import org.apache.commons.lang.ArrayUtils
import org.apache.commons.lang3.StringUtils

/**
  * @author w1992wishes 2019/3/1 20:14
  */
trait CalculateQualityFunc extends Serializable {

  /**
    * 计算特征值质量
    *
    * @param config      配置属性
    * @param featureInfo 特征值信息
    * @param poseInfo    角度信息
    * @param qualityInfo 质量信息
    * @return
    */
  def calculateFeatureQuality(config: ConfigArgs)
                             (featureInfo: Array[Byte], poseInfo: String, qualityInfo: Float): Float = {

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

    def clusterPoseFilter(poseInfo: PoseInfo): Boolean = PoseInfoUtils.inAngle(poseInfo, config.clusterPitchThreshold, config.clusterRollThreshold, config.clusterYawThreshold)

    def classPoseFilter(poseInfo: PoseInfo): Boolean = PoseInfoUtils.inAngle(poseInfo, config.classPitchThreshold, config.classRollThreshold, config.classYawThreshold)

    def clusterQualityFilter(quality: Float): Boolean = quality >= config.clusterQualityThreshold

    def classQualityFilter(quality: Float): Boolean = quality >= config.classQualityThreshold

    /**
      * 计算特征值质量
      *
      * @param poseInfo 角度
      * @param quality  质量分值
      * @return 特征质量
      */
    def calculateFeatureQuality(poseInfo: PoseInfo)(quality: Float): Float = {
      var featureQuality = .0f
      if (clusterQualityFilter(quality) && clusterPoseFilter(poseInfo)) {
        featureQuality = quality * calculatePoseWeight(poseInfo)
      } else if (classQualityFilter(quality) && classPoseFilter(poseInfo)) {
        featureQuality = Constants.CLASS_QUALITY
      } else {
        featureQuality = Constants.UNCLASS_QUALITY
      }
      featureQuality
    }

    var poseInfoModel: PoseInfo = null
    var featureQuality = 0.0f
    if (StringUtils.isNotEmpty(poseInfo) && ArrayUtils.isNotEmpty(featureInfo)) {
      try {
        poseInfoModel = JSON.parseObject(poseInfo, classOf[PoseInfo])
        featureQuality = calculateFeatureQuality(poseInfoModel)(qualityInfo)
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
