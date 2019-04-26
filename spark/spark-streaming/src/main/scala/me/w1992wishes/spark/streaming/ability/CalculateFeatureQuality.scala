package me.w1992wishes.spark.streaming.ability

import org.apache.commons.lang3.ArrayUtils

/**
  * @author w1992wishes 2019/3/1 20:14
  */
trait CalculateFeatureQuality extends Serializable {

  /**
    * 计算特征值质量
    *
    * @param featureInfo 特征值信息
    * @param qualityInfo 质量信息
    * @return
    */
  def calculateFeatureQuality(featureInfo: Array[Byte], qualityInfo: Float): Float = {

    def clusterQualityFilter(quality: Float): Boolean = quality.compareTo(0.91f) >= 0

    def classQualityFilter(quality: Float): Boolean = quality.compareTo(0.30f) >= 0

    /**
      * 计算特征值质量
      *
      * @param quality 质量分值
      * @return 特征质量
      */
    def calculateFeatureQuality(quality: Float): Float = {
      var featureQuality = .0f
      if (clusterQualityFilter(quality)) {
        featureQuality = quality // good
      } else if (classQualityFilter(quality)) {
        featureQuality = -1.0f // bad
      } else {
        featureQuality = -2.0f // useless
      }
      featureQuality
    }

    var featureQuality = 0.0f
    if (ArrayUtils.isNotEmpty(featureInfo)) {
      featureQuality = calculateFeatureQuality(qualityInfo)
    } else {
      featureQuality = -2.0f
    }
    featureQuality.formatted("%.4f").toFloat
  }

}
