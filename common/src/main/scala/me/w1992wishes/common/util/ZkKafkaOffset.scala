package me.w1992wishes.common.util

import org.I0Itec.zkclient.ZkClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010.OffsetRange

import scala.collection.JavaConverters._


/**
  * Kafka offset zookeeper 保存工具
  *
  * @param getClient 构造 ZkClient
  * @param getZkRoot 构造 ZkRoot
  */
class ZkKafkaOffset(getClient: () => ZkClient, getZkRoot: () => String) extends Serializable {

  // 定义为 lazy 实现了懒汉式的单例模式，解决了序列化问题，方便使用 broadcast
  lazy val zkClient: ZkClient = getClient()
  lazy val zkRoot: String = getZkRoot()

  // 初始化偏移量的 zk 存储路径 zkRoot
  def initOffset(): Unit = {
    if (!zkClient.exists(zkRoot)) {
      zkClient.createPersistent(zkRoot, true)
    }
  }

  // 从 zkRoot 读取偏移量信息
  def getOffset: Map[TopicPartition, Long] = {
    val keys = zkClient.getChildren(zkRoot)
    var initOffsetMap: Map[TopicPartition, Long] = Map()
    if (!keys.isEmpty) {
      for (k: String <- keys.asScala) {
        val ks = k.split("!")
        //zkClient.writeData(zkRoot + "/" + k, 354605L)
        val value: Long = zkClient.readData(zkRoot + "/" + k)
        println(s"****** [get] Topic $ks , Offset $value ******")
        initOffsetMap += (new TopicPartition(ks(0), Integer.parseInt(ks(1))) -> value)
      }
    }
    initOffsetMap
  }

  def getOffset(topics: String, groupId: String): Map[TopicPartition, Long] = {
    val zkRoot = s"/StructuredStream/offset/${Md5.hashMD5(topics.mkString(",") + groupId)}"
    val keys = zkClient.getChildren(zkRoot)
    var initOffsetMap: Map[TopicPartition, Long] = Map()
    if (!keys.isEmpty) {
      for (k: String <- keys.asScala) {
        val ks = k.split("!")
        //zkClient.writeData(zkRoot + "/" + k, 354605L)
        val value: Long = zkClient.readData(zkRoot + "/" + k)
        println(s"****** [get] Topic $ks , Offset $value ******")
        initOffsetMap += (new TopicPartition(ks(0), Integer.parseInt(ks(1))) -> value)
      }
    }
    initOffsetMap
  }

  // 根据单条消息，更新偏移量信息
  def updateOffset(consumeRecord: ConsumerRecord[String, String]): Boolean = {
    val path = zkRoot + "/" + consumeRecord.topic + "!" + consumeRecord.partition
    zkClient.writeData(path, consumeRecord.offset())
    true
  }

  // 消费消息前，批量更新偏移量信息
  def updateOffset(offsetRanges: Array[OffsetRange]): Boolean = {
    for (offset: OffsetRange <- offsetRanges) {
      val path = zkRoot + "/" + offset.topic + "!" + offset.partition
      if (!zkClient.exists(path)) {
        zkClient.createPersistent(path, offset.fromOffset)
      }
      else {
        zkClient.writeData(path, offset.fromOffset)
      }
      println(s"****** [Update] Topic ${offset.topic} Partition ${offset.partition} Offset ${offset.fromOffset} ******")
    }
    true
  }

  // 消费消息后，批量提交偏移量信息
  def commitOffset(offsetRanges: Array[OffsetRange]): Boolean = {
    for (offset: OffsetRange <- offsetRanges) {
      val path = zkRoot + "/" + offset.topic + "!" + offset.partition
      if (!zkClient.exists(path)) {
        zkClient.createPersistent(path, offset.untilOffset)
      }
      else {
        zkClient.writeData(path, offset.untilOffset)
      }
      println(s"****** [Commit] Topic ${offset.topic} Partition ${offset.partition} Offset ${offset.untilOffset} ******")
    }
    true
  }

  override def finalize(): Unit = {
    zkClient.close()
  }
}

object ZkKafkaOffset {
  def apply(zkUrls: String, topics: String, groupId: String): ZkKafkaOffset = {
    val getClient = () => new ZkClient(zkUrls, 30000)
    val getZkRoot = () => s"/StructuredStream/offset/${Md5.hashMD5(topics.mkString(",") + groupId)}"
    val zkKafkaOffset = new ZkKafkaOffset(getClient, getZkRoot)
    zkKafkaOffset.initOffset()
    zkKafkaOffset
  }
}