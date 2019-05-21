package me.w1992wishes.spark.streaming.core

import cn.hutool.crypto.SecureUtil
import me.w1992wishes.spark.streaming.config.{StreamingConfig, TaskArguments}
import me.w1992wishes.spark.streaming.http.CloseHttpServer
import me.w1992wishes.spark.streaming.zookeeper.ZkKafkaOffset
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
