package me.w1992wishes.spark.streaming.intellif.stream

import java.time.LocalDateTime

import me.w1992wishes.common.analysis.GPAnalysis
import me.w1992wishes.common.util.{CloseHttpServer, Log, Md5, ZkKafkaOffset}
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
abstract class StreamingTask(config: Map[String, String]) extends GPAnalysis with Log with Serializable {

  @transient val ssc: StreamingContext = initialStreamingContext()

  val topicStr: String = config("kafka.input.topic")
  val groupId: String = config("kafka.group.id")
  val topics: Array[String] = topicStr.split(",")
  val offsetId: String = Md5.hashMD5(groupId + topics.mkString(","))
  @transient val kafkaOffset: ZkKafkaOffset = ZkKafkaOffset(ssc.sparkContext.getConf, offsetId)

  def getStreamingContext: StreamingContext = ssc

  def createStream(): InputDStream[ConsumerRecord[String, String]] = {
    def initialConsumerKafkaConf(): Map[String, Object] = {
      // consumer
      val kafkaGroupId = groupId
      val kfkServers = config("kafka.input.servers")
      val receiveBufferBytes = config.getOrElse("kafka.receive.buffer.bytes", "102400").toInt
      val maxPartitionFetchBytes = config.getOrElse("kafka.max.partition.fetch.bytes", "5252880").toInt
      val autoOffsetReset = config.getOrElse("kafka.auto.offset.reset", "earliest")
      val enableAutoCommit = config.getOrElse("kafka.enable.auto.commit", "false").toBoolean
      val kafkaConf = Map[String, Object](
        "bootstrap.servers" -> kfkServers,
        "key.deserializer" -> classOf[StringDeserializer],
        "value.deserializer" -> classOf[StringDeserializer],
        "group.id" -> kafkaGroupId,
        "receive.buffer.bytes" -> (receiveBufferBytes: java.lang.Integer),
        "max.partition.fetch.bytes" -> (maxPartitionFetchBytes: java.lang.Integer),
        "auto.offset.reset" -> autoOffsetReset,
        "enable.auto.commit" -> (enableAutoCommit: java.lang.Boolean)
      )
      kafkaConf
    }

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

  private def initialStreamingContext(): StreamingContext = {
    val streamingTaskName = config.getOrElse("streaming.task.name", getClass.getSimpleName)
    val zkServer = config("streaming.zk.servers")
    val maxRatePerPartition = config.getOrElse("spark.streaming.kafka.maxRatePerPartition", "2000")
    val conf = new SparkConf()
      .setAppName(streamingTaskName)
      .set("spark.io.compression.codec", "snappy")
      .set("kafka.zk.hosts", zkServer)
      .set("spark.streaming.kafka.maxRatePerPartition", maxRatePerPartition)
      .set("spark.streaming.stopGracefullyOnShutdown", "true") //等待数据处理完后，才停止任务，以免数据丢失
      .set("spark.streaming.backpressure.enabled", "true")
      .set("spark.streaming.backpressure.initialRate", "1000")
      .setIfMissing("spark.master", "local[12]")
    val sc = new SparkContext(conf)
    sc.setLogLevel(config("spark.log.level"))
    val ssc = new StreamingContext(sc, Durations.seconds(config("streaming.task.batch.duration").toLong))
    ssc
  }

  def addCloseServer(): Unit = {
    CloseHttpServer.daemonHttpServer(config.getOrElse("streaming.task.close.port", "29999").toInt, ssc)
  }

  def stopAtTime(time: LocalDateTime): Unit = {
    val intervalMills = 10000 // 每隔10秒扫描一次消息是否存在
    var isStop = false
    while (!isStop) {
      isStop = ssc.awaitTerminationOrTimeout(intervalMills)
      if (!isStop && isAppointedTime(time)) {
        println("try to close app after 1s ......")
        Thread.sleep(1000)
        ssc.stop(stopSparkContext = true, stopGracefully = true)
      } else {
        println("not reach the specified time to close app ......")
      }
    }

    def isAppointedTime(time: LocalDateTime): Boolean = {
      LocalDateTime.now().compareTo(time) >= 0
    }
  }

}
