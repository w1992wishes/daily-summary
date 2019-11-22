package me.w1992wishes.spark.streaming.common.core

import cn.hutool.crypto.SecureUtil
import me.w1992wishes.spark.streaming.common.config.{StreamingConfig, TaskArguments}
import me.w1992wishes.spark.streaming.common.http.CloseHttpServer
import me.w1992wishes.spark.streaming.common.zookeeper.ZkKafkaOffset
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Durations, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

/**
  * @author w1992wishes 2019/5/20 16:19
  */
abstract class StreamingTask(taskArguments: TaskArguments, streamingConfig: StreamingConfig) extends Serializable {

  @transient val ssc: StreamingContext = initialStreamingContext()

  val topicStr: String = streamingConfig.getValidProperty("kafka.input.topic")
  val groupId: String = streamingConfig.getString("kafka.group.id", "StreamingTask")
  val topics: Array[String] = topicStr.split(",")
  val offsetId: String = SecureUtil.md5(groupId + topics.mkString(","))
  @transient val kafkaOffset = ZkKafkaOffset(ssc.sparkContext.getConf, offsetId)

  def getStreamingContext: StreamingContext = ssc

  def initialConsumerKafkaConf(): Map[String, Object] = {
    // consumer
    val groupId = streamingConfig.getString("kafka.group.id", "StreamingSyncTask")
    val kfkServers = streamingConfig.getValidProperty("kafka.bootstrap.servers")
    val receiveBufferBytes = streamingConfig.getInt("kafka.receive.buffer.bytes", 102400)
    val maxPartitionFetchBytes = streamingConfig.getInt("kafka.max.partition.fetch.bytes", 5252880)
    val autoOffsetReset = streamingConfig.getString("kafka.auto.offset.reset", "earliest")
    val enableAutoCommit = streamingConfig.getBoolean("kafka.enable.auto.commit", value = false)
    val kafkaConf = Map[String, Object](
      "bootstrap.servers" -> kfkServers,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> groupId,
      "receive.buffer.bytes" -> (receiveBufferBytes: java.lang.Integer),
      "max.partition.fetch.bytes" -> (maxPartitionFetchBytes: java.lang.Integer),
      "auto.offset.reset" -> autoOffsetReset,
      "enable.auto.commit" -> (enableAutoCommit: java.lang.Boolean)
    )
    kafkaConf
  }

  def initialStreamingContext(): StreamingContext = {
    val streamingTaskName = streamingConfig.getString("streaming.task.name", "StreamingTask")
    val zkServer = streamingConfig.getValidProperty("streaming.zk.servers")
    val conf = new SparkConf()
      .setAppName(streamingTaskName)
      .set("spark.io.compression.codec", "snappy")
      .set("kafka.zk.hosts", zkServer)
      // 设置每秒每个分区最大获取日志数,控制处理数据量，保证数据均匀处理
      .set("spark.streaming.kafka.maxRatePerPartition", "2000")
      // 启动优雅关闭服务
      .set("spark.streaming.stopGracefullyOnShutdown", "true")
      // Spark Streaming 重启后Kafka数据堆积调优
      .set("spark.streaming.backpressure.enabled", "true") // 激活反压功能
      .set("spark.streaming.backpressure.initialRate", "5000") // 启动反压功能后，读取的最大数据量
      .setIfMissing("spark.master", "local[2]")
    conf.set("kafka.zk.hosts", zkServer)
    val sc = new SparkContext(conf)
    sc.setLogLevel("WARN")
    val ssc = new StreamingContext(sc, Durations.seconds(taskArguments.batchDuration))
    ssc
  }

  def createStream(): InputDStream[ConsumerRecord[String, String]] = {
    val consumerKafkaConf = initialConsumerKafkaConf()
    var stream: InputDStream[ConsumerRecord[String, String]] = null
    val customOffset: Map[TopicPartition, Long] = kafkaOffset.getOffset
    if (customOffset.isEmpty) {
      stream = KafkaUtils.createDirectStream[String, String](ssc,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[String, String](topics, consumerKafkaConf))
    } else {
      stream = KafkaUtils.createDirectStream[String, String](ssc,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Assign[String, String](customOffset.keySet, consumerKafkaConf, customOffset))
    }
    stream
  }

  def addCloseServer(): Unit = {
    CloseHttpServer.daemonHttpServer(streamingConfig.getInt("streaming.task.close.port", 19999), ssc)
  }

}
