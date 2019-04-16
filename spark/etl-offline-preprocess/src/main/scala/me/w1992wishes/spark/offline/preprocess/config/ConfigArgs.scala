package me.w1992wishes.spark.offline.preprocess.config

import java.io.FileInputStream
import java.util.Properties

/**
  * 配置文件参数
  *
  * @author w1992wishes 2019/1/14 10:37
  */
class ConfigArgs extends Serializable{

  val filePath = "system.properties"
  val properties = new Properties()
  properties.load(new FileInputStream(filePath))

  // preProcess 数据源配置
  val sourceUrl: String = properties.getProperty("preProcess.source.url")
  val sourceDriver: String = properties.getProperty("preProcess.source.driver")
  val sourceUser: String = properties.getProperty("preProcess.source.user")
  val sourcePasswd: String = properties.getProperty("preProcess.source.passwd")
  val sourceFetchsize: String = properties.getProperty("preProcess.source.fetchsize")
  val sourceTable: String = properties.getProperty("preProcess.source.table")

  val sinkUrl: String = properties.getProperty("preProcess.sink.url")
  val sinkDriver: String = properties.getProperty("preProcess.sink.driver")
  val sinkUser: String = properties.getProperty("preProcess.sink.user")
  val sinkPasswd: String = properties.getProperty("preProcess.sink.passwd")
  val sinkBatchsize: Int = properties.getProperty("preProcess.sink.batchsize").toInt
  val sinkTable: String = properties.getProperty("preProcess.sink.table")

  // job 不同配置属性
  val sparkAppName: String = properties.getProperty("preProcess.name")

  val clusterQualityThreshold: Float = properties.getProperty("preProcess.clusterQualityThreshold").toFloat
  val classQualityThreshold: Float = properties.getProperty("preProcess.classQualityThreshold").toFloat

  val clusterPitchThreshold: Float = properties.getProperty("preProcess.clusterPitchThreshold").toFloat
  val clusterRollThreshold: Float = properties.getProperty("preProcess.clusterRollThreshold").toFloat
  val clusterYawThreshold: Float = properties.getProperty("preProcess.clusterYawThreshold").toFloat
  val classPitchThreshold: Float = properties.getProperty("preProcess.classPitchThreshold").toFloat
  val classRollThreshold: Float = properties.getProperty("preProcess.classRollThreshold").toFloat
  val classYawThreshold: Float = properties.getProperty("preProcess.classYawThreshold").toFloat

}
