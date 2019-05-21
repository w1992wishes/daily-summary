package me.w1992wishes.spark.streaming.ability

import me.w1992wishes.spark.streaming.config.StreamingConfig

/**
  * @author w1992wishes 2019/3/1 20:14
  */
trait FeatureQualityAbility extends Serializable {

  /**
    * 计算特征值质量
    *
    * @param config      配置属性
    * @param featureInfo 特征值信息
    * @param qualityInfo 质量信息
    * @return
    */
  def calculateFeatureQuality(config: StreamingConfig)
                             (featureInfo: Array[Byte], qualityInfo: Float): Float = {


    def clusterQualityFilter(quality: Float): Boolean = quality >= config.getFloat("preProcess.clusterQualityThreshold", 0.79f)

    def classQualityFilter(quality: Float): Boolean = quality >= config.getFloat("preProcess.classQualityThreshold", 0.30f)

    /**
      * 计算特征值质量
      *
      * @param quality  质量分值
      * @return 特征质量
      */
    def calculateFeatureQuality(quality: Float): Float = {
      var featureQuality = .0f
      if (clusterQualityFilter(quality)) {
        featureQuality = quality
      } else if (classQualityFilter(quality)) {
        featureQuality = -1.0f
      } else {
        featureQuality = -2.0f
      }
      featureQuality
    }

    val featureQuality = calculateFeatureQuality(qualityInfo)

    featureQuality.formatted("%.4f").toFloat
  }

}
