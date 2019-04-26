package me.w1992wishes.spark.streaming.core

import cn.hutool.crypto.SecureUtil
import cn.hutool.log.StaticLog
import com.alibaba.fastjson.JSON
import me.w1992wishes.common.domain.EventFace
import me.w1992wishes.my.common.util.KafkaSender
import me.w1992wishes.spark.streaming.config.{CommandLineArgs, ConfigArgs}
import me.w1992wishes.spark.streaming.zookeeper.ZkKafkaOffset
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Durations, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Streaming 工厂类
  *
  * @author w1992wishes 2019/4/25 15:56
  */
abstract class StreamingJob[T](val config: ConfigArgs,
                               val command: CommandLineArgs) extends Serializable {

  private var ssc: StreamingContext = _

  init()

  /**
    * 初始化 StreamingContext
    */
  def init(): Unit = {
    val conf = new SparkConf().setAppName(config.streamingAppName).setIfMissing("spark.master", "local[2]")

    // streaming 相关配置
    conf.set("spark.streaming.stopGracefullyOnShutdown", "true")
    conf.set("spark.streaming.backpressure.enabled", "true")
    conf.set("spark.streaming.backpressure.initialRate", "1000")

    // 自定义参数，设置 zookeeper 连接信息
    conf.set("kafka.zk.hosts", config.zookeeperServers)

    // 创建 StreamingContext
    val sc = new SparkContext(conf)
    sc.setLogLevel("WARN")
    ssc = new StreamingContext(sc, Durations.seconds(command.batchDuration))
  }

  /**
    * 返回 StreamingContext
    *
    * @return
    */
  def getSparkStream: StreamingContext = ssc

  /**
    * 这里是具体执行业务逻辑地方
    *
    * @param eventFaceRdd RDD
    */
  def doAction(eventFaceRdd: RDD[EventFace], kafkaSender: Broadcast[KafkaSender[String, String]])

  /**
    *
    */
  def processDStreamFromKafkaOffset(): Unit = {
    println("****** Start receiving kafka messages kafka ****** :({})", config.streamingAppName)

    val groupId = config.kafkaGroupId
    val kfkServers = config.kafkaServers
    val topicStr = config.kafkaOdlTopic
    val topics = topicStr.split(",")

    //设置 streaming kafka 连接参数
    val kafkaConf = Map[String, Object](
      "bootstrap.servers" -> kfkServers,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> groupId,
      "receive.buffer.bytes" -> (102400: java.lang.Integer),
      "max.partition.fetch.bytes" -> (5252880: java.lang.Integer),
      "auto.offset.reset" -> "earliest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    // // 根据 groupId 和 topics 获取 offset
    val offsetId = SecureUtil.md5(groupId + topics.mkString(","))
    val kafkaOffset = ZkKafkaOffset(ssc.sparkContext.getConf, offsetId)
    kafkaOffset.initOffset()
    val customOffset: Map[TopicPartition, Long] = kafkaOffset.getOffset

    // 创建数据流
    var stream: InputDStream[ConsumerRecord[String, String]] = null
    // 尚未保存 offset 到 zk
    if (customOffset.isEmpty) {
      StaticLog.warn(s"****** 待读取的 kafka 主题：$topicStr ****** ")
      stream = KafkaUtils.createDirectStream[String, String](ssc,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[String, String](topics, kafkaConf))
    } else {
      StaticLog.warn(s"****** 待读取的 kafka 主题：$topicStr ****** ")
      stream = KafkaUtils.createDirectStream[String, String](ssc,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Assign[String, String](customOffset.keySet, kafkaConf, customOffset))
    }

    // 广播 KafkaSender（采用懒加载，因为 KafkaProducer 不能序列化）
    val kafkaSender: Broadcast[KafkaSender[String, String]] = {
      val kafkaProducerConf = Map[String, Object](
        "bootstrap.servers" -> config.kafkaServers,
        "key.serializer" -> classOf[StringSerializer],
        "value.serializer" -> classOf[StringSerializer],
        "client.id" -> "dwd_event_face_output",
        "acks" -> config.properties.getProperty("kafka.acks"),
        "retries" -> config.properties.getProperty("kafka.retries"),
        "batch.size" -> config.properties.getProperty("kafka.batch.size"),
        "linger.ms" -> config.properties.getProperty("kafka.linger.ms"),
        "buffer.memory" -> config.properties.getProperty("kafka.buffer.memory")
      )
      println("****** kafka producer init done! ******")
      ssc.sparkContext.broadcast(KafkaSender[String, String](kafkaProducerConf))
    }

    // 消费数据
    stream.foreachRDD(kafkaRdd => {
      // 消息消费前，更新 offset 信息
      val offsetRanges = kafkaRdd.asInstanceOf[HasOffsetRanges].offsetRanges
      kafkaOffset.updateOffset(offsetRanges)

      println("****** Start processing RDD data ******")
      // 转为 eventFaceRdd 便于分区， ConsumerRecord 不能进行 shuffle，因为没有序列化
      val eventFaceRdd = kafkaRdd.map(json => JSON.parseObject(json.value(), classOf[EventFace]))

      doAction(eventFaceRdd.repartition(command.partitions), kafkaSender)

      // 消息消费结束，提交 offset 信息
      kafkaOffset.commitOffset(offsetRanges)
    })
  }
}
