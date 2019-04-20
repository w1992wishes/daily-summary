package me.w1992wishes.spark.streaming.preprocess.util

/**
  * @author w1992wishes 2019/2/28 12:08
  */
object Constants {

  final val USELESS_QUALITY: Float = -3.0f  // 字段缺失或者字段格式不对归为该类

  final val UNCLASS_QUALITY: Float = -2.0f // 计算出的不可归档不可聚档归为该类

  final val CLASS_QUALITY: Float = -1.0f // 计算出的可归档不可聚档归为该类

  final val MINIMUM_CLUSTER_QUALITY: Float = 0.0f // 计算出的可聚档数据 feature_quality 的下限（大于该值）

  final val GP_PARTITION_STR: String = "_1_prt_p" // 拼接分区表的字符

}
