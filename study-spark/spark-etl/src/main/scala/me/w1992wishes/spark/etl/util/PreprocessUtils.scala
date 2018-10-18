package me.w1992wishes.spark.etl.util

import me.w1992wishes.spark.etl.job2.XPose

/**
  * 预处理工具类
  *
  * @author w1992wishes 2018/10/17 14:30
  */
object PreprocessUtils {

  private object FilterFlag extends Enumeration {
    // 这行是可选的，类型别名，在使用import语句的时候比较方便，建议加上
    type FilterFlag = Value
    // 枚举的定义
    val ONE = Value(1) // 可建档可以归档
    val TWO = Value(2) // 不可建档可以归档
    val THREE = Value(3) // 不可建档不可以归档
  }

  /**
    * 定义三个预处理后的类别
    *
    * @param xPose
    * @param quality
    * @return
    */
  def getFilterFlag(xPose: XPose, quality: Integer) = {
    var filterFlag = 0
    if (0 <= quality) {
      filterFlag = if (XPoseUtils.isFineAngle(xPose)) FilterFlag.ONE.id else FilterFlag.TWO.id
    } else {
      filterFlag = FilterFlag.THREE.id
    }
    filterFlag.toShort
  }

}
