package me.w1992wishes.spark.streaming.preprocess.config

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

  val dwdUrl: String = properties.getProperty("dwd.url")
  val dwdDriver: String = properties.getProperty("dwd.driver")
  val dwdUser: String = properties.getProperty("dwd.user")
  val dwdPasswd: String = properties.getProperty("dwd.passwd")
  val dwdBatchsize: Int = properties.getProperty("dwd.batchsize").toInt
  val dwdTable: String = properties.getProperty("dwd.table")

  // job 不同配置属性
  val sparkAppName: String = properties.getProperty("streaming.preProcess.name")

  val clusterQualityThreshold: Float = properties.getProperty("preProcess.clusterQualityThreshold").toFloat
  val classQualityThreshold: Float = properties.getProperty("preProcess.classQualityThreshold").toFloat

  val clusterPitchThreshold: Float = properties.getProperty("preProcess.clusterPitchThreshold").toFloat
  val clusterRollThreshold: Float = properties.getProperty("preProcess.clusterRollThreshold").toFloat
  val clusterYawThreshold: Float = properties.getProperty("preProcess.clusterYawThreshold").toFloat
  val classPitchThreshold: Float = properties.getProperty("preProcess.classPitchThreshold").toFloat
  val classRollThreshold: Float = properties.getProperty("preProcess.classRollThreshold").toFloat
  val classYawThreshold: Float = properties.getProperty("preProcess.classYawThreshold").toFloat

  // kafka
  val kafkaServers: String = properties.getProperty("kafka.bootstrap.servers")
  val kafkaKeySerializer: String = properties.getProperty("kafka.key.serializer")
  val kafkaValueSerializer: String = properties.getProperty("kafka.value.serializer")
  val kafkaKeyDeserializer: String = properties.getProperty("kafka.key.deserializer")
  val kafkaValueDeserializer: String = properties.getProperty("kafka.value.deserializer")
  val kafkaGroupId: String = properties.getProperty("kafka.StreamingPreProcess.group.id")
  val kafkaAutoOffsetReset: String = properties.getProperty("kafka.auto.offset.reset")
  val kafkaEnableAutoCommit: Boolean = properties.getProperty("kafka.enable.auto.commit").toBoolean
  val kafkaOdlTopic: String = properties.getProperty("kafka.odl.topic")
  val kafkaDwdTopic: String = properties.getProperty("kafka.dwd.topic")

}
