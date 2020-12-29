package me.w1992wishes.structured.streaming.intellif.helper

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

}
