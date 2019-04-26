package me.w1992wishes.spark.streaming.config

import java.io.FileInputStream
import java.util.Properties

/**
  * 配置文件参数
  *
  * @author w1992wishes 2019/1/14 10:37
  */
class ConfigArgs extends Serializable {

  val filePath = "config.properties"
  val properties = new Properties()
  properties.load(new FileInputStream(filePath))

  val streamingClosePort: Int = properties.getProperty("streaming.app.close.port").toInt
  val streamingAppName: String = properties.getProperty("streaming.app.name")

  // producer
  val kafkaServers: String = properties.getProperty("kafka.bootstrap.servers")
  val kafkaKeySerializer: String = properties.getProperty("kafka.key.serializer")
  val kafkaValueSerializer: String = properties.getProperty("kafka.value.serializer")

  // consumer
  val kafkaKeyDeserializer: String = properties.getProperty("kafka.key.deserializer")
  val kafkaValueDeserializer: String = properties.getProperty("kafka.value.deserializer")
  val kafkaGroupId: String = properties.getProperty("kafka.group.id")
  val kafkaOdlTopic: String = properties.getProperty("kafka.odl.topic")
  val kafkaDwdTopic: String = properties.getProperty("kafka.dwd.topic")

  val zookeeperServers: String = properties.getProperty("zookeeper.servers")

  // spark
  val maxRatePerPartition: String = properties.getProperty("spark.streaming.kafka.maxRatePerPartition")
  val sparkMaster: String = properties.getProperty("spark.master")

  val dwdUrl: String = properties.getProperty("dwd.url")
  val dwdDriver: String = properties.getProperty("dwd.driver")
  val dwdUser: String = properties.getProperty("dwd.user")
  val dwdPasswd: String = properties.getProperty("dwd.passwd")
  val dwdBatchSize: Int = properties.getProperty("dwd.batchsize").toInt
  val dwdTable: String = properties.getProperty("dwd.table")

  val clusterQualityThreshold: Float = properties.getProperty("preProcess.clusterQualityThreshold").toFloat
  val classQualityThreshold: Float = properties.getProperty("preProcess.classQualityThreshold").toFloat
  val clusterPitchThreshold: Float = properties.getProperty("preProcess.clusterPitchThreshold").toFloat
  val clusterRollThreshold: Float = properties.getProperty("preProcess.clusterRollThreshold").toFloat
  val clusterYawThreshold: Float = properties.getProperty("preProcess.clusterYawThreshold").toFloat
  val classPitchThreshold: Float = properties.getProperty("preProcess.classPitchThreshold").toFloat
  val classRollThreshold: Float = properties.getProperty("preProcess.classRollThreshold").toFloat
  val classYawThreshold: Float = properties.getProperty("preProcess.classYawThreshold").toFloat

  val saveDbEnable: Boolean = properties.getProperty("save.db.enable").toBoolean

  // pool
  val dbcpProperties: Properties = {
    val props: Properties = new Properties()
    props.put("initialSize", properties.getProperty("executor.pool.initialSize"))
    props.put("maxActive", properties.getProperty("executor.pool.maxActive"))
    props.put("maxIdle", properties.getProperty("executor.pool.maxIdle"))
    props.put("minIdle", properties.getProperty("executor.pool.minIdle"))
    props.put("maxWait", properties.getProperty("executor.pool.maxWait"))
    props.put("validationQuery", properties.getProperty("executor.pool.validationQuery"))
    props.put("testWhileIdle", properties.getProperty("executor.pool.testWhileIdle"))
    props.put("testOnBorrow", properties.getProperty("executor.pool.testOnBorrow"))
    props.put("timeBetweenEvictionRunsMillis", properties.getProperty("executor.pool.timeBetweenEvictionRunsMillis"))
    props.put("minEvictableIdleTimeMillis", properties.getProperty("executor.pool.minEvictableIdleTimeMillis"))
    props.put("numTestsPerEvictionRun", properties.getProperty("executor.pool.numTestsPerEvictionRun"))
    props.put("connectionProperties", properties.getProperty("executor.pool.connectionProperties"))
    props.put("username", dwdUser)
    props.put("password", dwdPasswd)
    props.put("driverClassName", dwdDriver)
    props.put("url", dwdUrl)
    props
  }
}

object ConfigArgs {
  def apply(): ConfigArgs = new ConfigArgs()
}
