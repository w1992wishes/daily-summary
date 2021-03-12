package me.w1992wishes.common.helper

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2020/12/15 19:39
  */
trait SparkHelper extends Serializable {

  def buildSpark(conf: SparkConf): SparkSession = {
    SparkSession.builder()
      .config(conf)
      .enableHiveSupport()
      .getOrCreate()
  }

  def buildSpark: SparkSession = {
    // spark 参数设置
    val conf = new SparkConf()
      .setIfMissing("spark.master", "local[8]")
      .set("spark.hadoopRDD.ignoreEmptySplits", "true")
      .set("spark.debug.maxToStringFields", "100")
      .set("spark.sql.catalogImplementation", "hive")
      .set("spark.io.compression.codec", "snappy")
      .set("hive.exec.dynamic.partition.mode", "nonstrict")
      .set("hive.exec.dynamic.partition", "true")

    SparkSession
      .builder()
      .config(conf)
      .enableHiveSupport()
      .appName(getClass.getSimpleName)
      .getOrCreate()
  }

}
