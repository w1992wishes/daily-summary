package me.w1992wishes.spark.etl.util

import me.w1992wishes.spark.etl.model.XPose


/**
  * 预处理工具类
  *
  * @author w1992wishes 2018/10/17 14:30
  */
object PreprocessUtils {

  /**
    * 定义三个预处理后的类别
    *
    * @param xPose
    * @param quality
    * @return
    */
  def getFilterFlag(xPose: XPose, quality: Float, qualityStandard: Float): Float = {
    var featureQuality = .0f
    if (quality < qualityStandard || XPoseUtils.isBadAngle(xPose)) {
      featureQuality = -1.0f
    } else {
      featureQuality = quality * (1 - XPoseUtils.calculatePoseWeight(xPose));
    }
    featureQuality.formatted("%.2f").toFloat
  }

  /**
    * 定义三个预处理后的类别
    *
    * @param quality
    * @return
    */
  def getFilterFlag(quality: Float, qualityStandard: Float): Float = {
    val featureQuality = if (quality < qualityStandard) -1.0f else quality
    featureQuality.formatted("%.2f").toFloat
  }
}
