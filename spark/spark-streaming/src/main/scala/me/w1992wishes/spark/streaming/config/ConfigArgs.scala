package me.w1992wishes.spark.streaming.config

import java.io.FileInputStream
import java.util.Properties

/**
  * 配置文件参数
  *
  * @author w1992wishes 2019/1/14 10:37
  */
class ConfigArgs extends Serializable{

  // submit 通过 --files 上传
  val filePath = "config.properties"
  val properties = new Properties()
  properties.load(new FileInputStream(filePath))

  // streaming app
  val streamingClosePort: Int = properties.getProperty("streaming.app.close.port").toInt
  val streamingBatchDuration: Int = properties.getProperty("streaming.batch.duration").toInt

  // kafka
  val kafkaServers: String = properties.getProperty("kafka.bootstrap.servers")
  val kafkaKeySerializer: String = properties.getProperty("kafka.key.serializer")
  val kafkaValueSerializer: String = properties.getProperty("kafka.value.serializer")
  val kafkaKeyDeserializer: String = properties.getProperty("kafka.key.deserializer")
  val kafkaValueDeserializer: String = properties.getProperty("kafka.value.deserializer")
  val kafkaGroupId: String = properties.getProperty("kafka.StreamingPreProcess.group.id")
  val kafkaAutoOffsetReset: String = properties.getProperty("kafka.auto.offset.reset")
  val kafkaEnableAutoCommit: Boolean = properties.getProperty("kafka.enable.auto.commit").toBoolean
  val kafkaTopicName: String = properties.getProperty("kafka.topic.name")

  // zookeeper
  val zookeeperServers: String = properties.getProperty("zookeeper.servers")

  // spark
  val maxRatePerPartition: String = properties.getProperty("spark.streaming.kafka.maxRatePerPartition")
  val sparkMaster: String = properties.getProperty("spark.master")
  
}

object ConfigArgs{
  def apply(): ConfigArgs = new ConfigArgs()
}
