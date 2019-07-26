package me.w1992wishes.spark.streaming.task

import java.sql.{Connection, PreparedStatement, Timestamp}
import java.time.LocalDateTime

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializeFilter
import me.w1992wishes.common.domain.EventFace
import me.w1992wishes.common.util.{ConnectionPool, KafkaSender}
import me.w1992wishes.common.util.ConnectionPool
import me.w1992wishes.spark.streaming.ability.{DbcpSupportAbility, FeatureQualityAbility}
import me.w1992wishes.spark.streaming.config.{PreProcessTaskArguments, StreamingConfig, TaskArguments}
import me.w1992wishes.spark.streaming.core.StreamingTask
import org.apache.commons.lang3.StringUtils
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.spark.TaskContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.kafka010.HasOffsetRanges

import scala.collection.JavaConversions
import scala.collection.mutable.ArrayBuffer

/**
  * @author w1992wishes 2019/4/25 19:59
  */

class StreamingPreProcessTask(taskArguments: PreProcessTaskArguments, streamingConfig: StreamingConfig)
  extends StreamingTask(taskArguments: TaskArguments, streamingConfig: StreamingConfig)
    with FeatureQualityAbility with DbcpSupportAbility{

  private def broadcastKafkaProducer() = {
    val kfkServers = streamingConfig.getValidProperty("kafka.bootstrap.servers")
    val kafkaAcks = streamingConfig.getString("kafka.acks", "all")
    val kafkaRetries = streamingConfig.getInt("kafka.retries", 3)
    val kafkaBatchSize = streamingConfig.getInt("kafka.batch.size", 20684)
    val kafkaLingerMs = streamingConfig.getInt("kafka.linger.ms", 1)
    val kafkaBufferMemory = streamingConfig.getInt("kafka.buffer.memory", 33554432)
    val kafkaProducerConf = Map[String, Object](
      "bootstrap.servers" -> kfkServers,
      "key.serializer" -> classOf[StringSerializer],
      "value.serializer" -> classOf[StringSerializer],
      "client.id" -> "dwd_event_face_output",
      "acks" -> kafkaAcks,
      "retries" -> (kafkaRetries: java.lang.Integer),
      "batch.size" -> (kafkaBatchSize: java.lang.Integer),
      "linger.ms" -> (kafkaLingerMs: java.lang.Integer),
      "buffer.memory" -> (kafkaBufferMemory: java.lang.Integer)
    )
    println("****** kafka producer init done! ******")
    ssc.sparkContext.broadcast(KafkaSender[String, String](kafkaProducerConf))
  }

  private def doAction(eventFaceRdd: RDD[EventFace],
                       kafkaSender: Broadcast[KafkaSender[String, String]]): Unit = {
    eventFaceRdd
      .map(eventFace => {
        eventFace.setFeatureQuality(
          calculateFeatureQuality(streamingConfig)(eventFace.getFeatureInfo, eventFace.getQualityInfo))
        eventFace.setSaveTime(Timestamp.valueOf(LocalDateTime.now().withNano(0)))
        eventFace
      }).foreachPartition(iter => partitionFunc(iter, kafkaSender))
  }

  private def writeSql(): String = {
    val holders = StringUtils.join(JavaConversions.seqAsJavaList((1 to 31).map(_ => "?")), ",")
    s"insert into ${streamingConfig.getValidProperty("streaming.sink.table")} " +
      "(sys_code, thumbnail_id, thumbnail_url, image_id, image_url, " +
      "feature_info, algo_version, gender_info, age_info, hairstyle_info, " +
      "hat_info, glasses_info, race_info, mask_info, skin_info, " +
      "pose_info, quality_info, target_rect, target_rect_float, target_thumbnail_rect, " +
      "land_mark_info, source_id, source_type, site, time, " +
      "create_time, feature_quality, save_time, column1, column2, " +
      "column3) " +
      s"VALUES($holders)"
  }

  private def partitionFunc(iter: Iterator[EventFace], kafkaSender: Broadcast[KafkaSender[String, String]]): Unit = {
    if (!iter.hasNext) {
      return
    }

    val eventFaces: ArrayBuffer[EventFace] = new ArrayBuffer[EventFace]()
    while (iter.hasNext) {
      eventFaces += iter.next()
    }

    println("****** Start send kafka message ******")
    val filteredEventFaces: ArrayBuffer[EventFace] = eventFaces
        .filter(eventFace => eventFace.getFeatureQuality.compareTo(-1.0f) >= 0 && eventFace.getFeatureQuality.compareTo(0.0f) != 0)
    filteredEventFaces.foreach(eventFace =>
      kafkaSender.value.send(streamingConfig.getValidProperty("kafka.output.topic"), JSON.toJSONString(eventFace, new Array[SerializeFilter](0))))
    println(s"****** End send kafka message, ${filteredEventFaces.size} totally ******")

    if (streamingConfig.getBoolean("save.db.enable", value = false)) {
      println("****** Start save db ******")
      saveToDB(eventFaces)
      println("****** End save db ******")
    }
    println(s"****** Partition_${TaskContext.get.partitionId} preProcess ${eventFaces.length} data totally ******")
  }

  private def fillPreparedStatement(ps: PreparedStatement, eventFace: EventFace): Unit = {
    ps.setString(1, eventFace.getSysCode)
    ps.setString(2, eventFace.getThumbnailId)
    ps.setString(3, eventFace.getThumbnailUrl)
    ps.setString(4, eventFace.getImageId)
    ps.setString(5, eventFace.getImageUrl)
    ps.setBytes(6, eventFace.getFeatureInfo)
    ps.setInt(7, eventFace.getAlgoVersion)
    ps.setString(8, eventFace.getGenderInfo)
    ps.setString(9, eventFace.getAgeInfo)
    ps.setString(10, eventFace.getHairstyleInfo)
    ps.setString(11, eventFace.getHatInfo)
    ps.setString(12, eventFace.getGlassesInfo)
    ps.setString(13, eventFace.getRaceInfo)
    ps.setString(14, eventFace.getMaskInfo)
    ps.setString(15, eventFace.getSkinInfo)
    ps.setString(16, eventFace.getPoseInfo)
    ps.setFloat(17, if (eventFace.getQualityInfo == null) 0f else eventFace.getQualityInfo)
    ps.setString(18, eventFace.getTargetRect)
    ps.setString(19, eventFace.getTargetRectFloat)
    ps.setString(20, eventFace.getTargetThumbnailRect)
    ps.setString(21, eventFace.getLandMarkInfo)
    ps.setLong(22, eventFace.getSourceId)
    ps.setInt(23, eventFace.getSourceType)
    ps.setString(24, eventFace.getSite)
    ps.setTimestamp(25, eventFace.getTime)
    ps.setTimestamp(26, Timestamp.valueOf(LocalDateTime.now().withNano(0)))
    ps.setFloat(27, eventFace.getFeatureQuality)
    ps.setTimestamp(28, eventFace.getSaveTime)
    ps.setString(29, eventFace.getColumn1)
    ps.setString(30, eventFace.getColumn2)
    ps.setString(31, eventFace.getColumn3)
  }

  private def saveToDB(eventFaces: ArrayBuffer[EventFace]): Unit = {
    var conn: Connection = null
    var ps: PreparedStatement = null
    try {
      conn = ConnectionPool(initDbcpProperties(streamingConfig))
      conn.setAutoCommit(false)
      ps = conn.prepareStatement(writeSql())
      var amount = 0
      var i = 0
      for (eventFace <- eventFaces) {
        if (i >= streamingConfig.getInt("streaming.sink.batchsize", 5000)) {
          i = 0
          ps.executeBatch()
          conn.commit()
        }
        i += 1
        amount += 1
        fillPreparedStatement(ps, eventFace)
        ps.addBatch()
      }
      ps.executeBatch()
      conn.commit()
    } finally {
      ConnectionPool.closeResource(conn, ps, rs = null)
    }
  }
}

object StreamingPreProcessTask {

  def main(args: Array[String]): Unit = {

    val taskArguments: PreProcessTaskArguments = PreProcessTaskArguments(args.toList)
    val streamingConfig: StreamingConfig = StreamingConfig(taskArguments.confName)

    val task = StreamingPreProcessTask(taskArguments, streamingConfig)

    val stream = task.createStream()

    val kafkaSender: Broadcast[KafkaSender[String, String]] = task.broadcastKafkaProducer()

    // 消费数据
    stream
      .foreachRDD(kafkaRdd => {
        val offsetRanges = kafkaRdd.asInstanceOf[HasOffsetRanges].offsetRanges
        task.kafkaOffset.updateOffset(offsetRanges)

        println("****** Start processing RDD data ******")
        val eventFaceRdd = kafkaRdd.map(json => JSON.parseObject(json.value(), classOf[EventFace]))
        task.doAction(eventFaceRdd.repartition(taskArguments.partitions), kafkaSender)
        println("****** End processing RDD data ******")

        task.kafkaOffset.commitOffset(offsetRanges)
      })

    task.ssc.start()
    task.addCloseServer()
    task.ssc.awaitTermination()
  }

  def apply(taskArguments: PreProcessTaskArguments, streamingConfig: StreamingConfig): StreamingPreProcessTask =
    new StreamingPreProcessTask(taskArguments, streamingConfig)
}
