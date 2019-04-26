package me.w1992wishes.spark.streaming.core

import java.util.regex.Pattern

import cn.hutool.crypto.SecureUtil
import me.w1992wishes.my.common.ability.Log
import me.w1992wishes.spark.streaming.config.{CommandLineArgs, ConfigArgs}
import me.w1992wishes.spark.streaming.zookeeper.ZkKafkaOffset
import org.apache.commons.lang3.StringUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * Streaming 工厂类
  *
  * @author w1992wishes 2019/4/25 15:56
  */
abstract class StreamingFactory(val config: ConfigArgs, val args: CommandLineArgs) extends Log{

  private var ssc: StreamingContext = _

  private var appName: String = "default_streaming_app"

  init()

  /**
    * 初始化 StreamingContext
    */
  def init(): Unit = {
    if (StringUtils.isNotEmpty(config.properties.getProperty("streaming.app.name"))) appName =
      config.properties.getProperty("streaming.app.name")

    // 创建 sparkConf
    var sparkConf: SparkConf = new SparkConf()
    if (args.isLocal) {
      sparkConf.setMaster("local[4]")
    } else {
      sparkConf.set("spark.master", config.sparkMaster)
    }
    sparkConf
      .setAppName(appName)
      .set("spark.streaming.kafka.maxRatePerPartition", config.maxRatePerPartition) //每个分区每秒最大接收条数

    //创建 SparkSession
    val sparkSession: SparkSession = SparkSession.builder.config(sparkConf).getOrCreate
    // 创建StreamingContext
    ssc = new StreamingContext(sparkSession.sparkContext, Seconds(args.batchDuration))
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
    * @param kafkaRdd RDD
    */
  def doAction(kafkaRdd: RDD[ConsumerRecord[String, String]])

  /**
    *
    */
  def processDStreamFromKafkaOffset(): Unit = {
    logInfo("****** Start receiving kafka messages kafka ****** :({})", appName)

    // 从配置文件读取 kafka 配置信息
    val groupId = config.kafkaGroupId
    val kfkServers = config.kafkaServers
    val topicStr = config.kafkaTopicName

    //设置kafka连接参数
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

    // 从 Zookeeper 中获取kafka topic 的offsets信息，Key为：${zkRoot}/${topic}:${partition}
    // 根据 groupId 和 topics 获取 offset
    val topics = topicStr.split(",")
    val offsetId = SecureUtil.md5(groupId + topics.mkString(","))
    val kafkaOffset = ZkKafkaOffset(ssc.sparkContext.getConf, offsetId)
    kafkaOffset.initOffset()
    val customOffset: Map[TopicPartition, Long] = kafkaOffset.getOffset

    // 创建数据流
    var stream: InputDStream[ConsumerRecord[String, String]] = null
    if (topicStr.contains("*")) {
      logInfo("****** Read kafka topics: {} using regular matching ******", topicStr)
      stream = KafkaUtils.createDirectStream[String, String](ssc,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.SubscribePattern[String, String](Pattern.compile(topicStr), kafkaConf, customOffset))
    }
    else {
      logInfo("****** The kafka topic: {} to read ******", topicStr)
      stream = KafkaUtils.createDirectStream[String, String](ssc,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[String, String](topics, kafkaConf, customOffset))
    }

    // 消费数据
    stream.foreachRDD(kafkaRdd => {
      // 消息消费前，更新 offset 信息
      val offsetRanges = kafkaRdd.asInstanceOf[HasOffsetRanges].offsetRanges
      kafkaOffset.updateOffset(offsetRanges)

      //region 处理详情数据
      logInfo("****** Start processing RDD data ******")
      doAction(kafkaRdd)

      // 消息消费结束，提交 offset 信息
      kafkaOffset.commitOffset(offsetRanges)
    })
    ssc.start()
    ssc.awaitTermination()
  }
}
