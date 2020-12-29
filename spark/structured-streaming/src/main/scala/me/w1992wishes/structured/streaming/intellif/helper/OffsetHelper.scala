package me.w1992wishes.structured.streaming.intellif.helper

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import me.w1992wishes.common.util.{Log, ZkKafkaOffset}
import org.apache.hadoop.fs.Path
import org.apache.kafka.common.TopicPartition
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.execution.streaming.OffsetSeqLog
import org.apache.spark.sql.kafka010.JsonUtilsWrapper
import org.apache.spark.sql.streaming.StreamingQueryListener.{QueryProgressEvent, QueryStartedEvent, QueryTerminatedEvent}
import org.apache.spark.sql.streaming.{SourceProgress, StreamingQueryListener}
import org.apache.spark.streaming.kafka010.OffsetRange

import scala.collection.mutable.ArrayBuffer

/**
  * @author w1992wishes 2020/12/25 9:46
  */
trait OffsetHelper extends Log {

  val objectMapper: ObjectMapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.USE_LONG_FOR_INTS, true)
    .registerModule(DefaultScalaModule)

  def getOffsetFromCheckpoint(spark: SparkSession, batchId: Long): Option[Seq[Map[TopicPartition, Long]]] = {

    val checkpointRoot = ""
    val checkpointDir = new Path(new Path(checkpointRoot), "offsets").toUri.toString
    val offsetSeqLog = new OffsetSeqLog(spark, checkpointDir)

    val endOffset: Option[Seq[Map[TopicPartition, Long]]] = offsetSeqLog.get(batchId).map { endOffset =>
      endOffset.offsets.filter(_.isDefined).map { str =>
        JsonUtilsWrapper.jsonToOffsets(str.get.json)
      }
    }

    endOffset
  }

  // """{"topic1":{"0":23,"1":-2},"topic2":{"0":-2}}"""
  def getOffsetFromZk(zkKafkaOffset: ZkKafkaOffset, topics: String, groupId: String): String = {
    val offsetMap: Map[TopicPartition, Long] = zkKafkaOffset.getOffset(topics, groupId)
    val topicOffset = scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, Long]]()
    for ((topicPartition, offset) <- offsetMap) {
      val partitionOffset = topicOffset.getOrElse(topicPartition.topic(), scala.collection.mutable.Map[String, Long]())
      partitionOffset(topicPartition.partition().toString) = offset
      topicOffset(topicPartition.topic()) = partitionOffset
    }
    objectMapper.writeValueAsString(topicOffset)
  }


  class KafkaOffsetListener(zkKafkaOffset: ZkKafkaOffset) extends StreamingQueryListener {

    def onQueryStarted(event: QueryStartedEvent): Unit = {}

    def onQueryTerminated(event: QueryTerminatedEvent): Unit = {}

    // 提交Offset
    def onQueryProgress(event: QueryProgressEvent): Unit = {
      // 遍历所有Source
      event.progress.sources.foreach((source: SourceProgress) => {
        val objectMapper = new ObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(DeserializationFeature.USE_LONG_FOR_INTS, true)
          .registerModule(DefaultScalaModule)

        val startOffset = objectMapper.readValue(source.startOffset, classOf[Map[String, Map[String, Long]]])
        val endOffset = objectMapper.readValue(source.endOffset, classOf[Map[String, Map[String, Long]]])

        // 遍历Source中的每个Topic
        val offsetRanges: ArrayBuffer[OffsetRange] = new ArrayBuffer[OffsetRange]()
        for ((topic, topicEndOffset) <- endOffset) {
          // 遍历Topic中的每个Partition
          for ((partition, offset) <- topicEndOffset) {
            val topicPartition = new TopicPartition(topic, partition.toInt)
            offsetRanges += OffsetRange(topicPartition, startOffset(topic)(partition), offset)
          }
        }
        zkKafkaOffset.commitOffset(offsetRanges.toArray)
      })
    }
  }

}
