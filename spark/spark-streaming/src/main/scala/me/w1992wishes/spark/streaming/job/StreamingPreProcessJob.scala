package me.w1992wishes.spark.streaming.job

import java.sql.{Connection, PreparedStatement, Timestamp}
import java.time.LocalDateTime

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializeFilter
import me.w1992wishes.common.domain.EventFace
import me.w1992wishes.my.common.util.{ConnectionPool, KafkaSender}
import me.w1992wishes.spark.streaming.ability.CalculateFeatureQuality
import me.w1992wishes.spark.streaming.config.{CommandLineArgs, ConfigArgs}
import me.w1992wishes.spark.streaming.core.StreamingJob
import org.apache.commons.lang3.StringUtils
import org.apache.spark.TaskContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD

import scala.collection.JavaConversions
import scala.collection.mutable.ArrayBuffer

/**
  * @author w1992wishes 2019/4/25 19:59
  */
class StreamingPreProcessJob(config: ConfigArgs, command: CommandLineArgs)
  extends StreamingJob(config: ConfigArgs, command: CommandLineArgs) with CalculateFeatureQuality {

  /**
    * 这里是具体执行业务逻辑地方
    *
    * @param eventFaceRdd RDD
    */
  override def doAction(eventFaceRdd: RDD[EventFace],
                        kafkaSender: Broadcast[KafkaSender[String, String]]): Unit = {
    eventFaceRdd
      .map(eventFace => {
        eventFace.setFeatureQuality(
          calculateFeatureQuality(eventFace.getFeatureInfo, eventFace.getQualityInfo))
        eventFace.setSaveTime(Timestamp.valueOf(LocalDateTime.now().withNano(0)))
        eventFace
      }).foreachPartition(iter => partitionFunc(iter, kafkaSender))
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
    val sendKafkaRdd: ArrayBuffer[EventFace] = eventFaces.filter(_.getFeatureQuality.compareTo(-1.0f) >= 0)
    sendKafkaRdd.foreach(eventFace =>
      kafkaSender.value.send(config.kafkaDwdTopic, JSON.toJSONString(eventFace, new Array[SerializeFilter](0))))
    println(s"****** End send kafka message, ${sendKafkaRdd.size} totally ******")

    if (config.saveDbEnable) {
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
    ps.setString(20, eventFace.getLandMarkInfo)
    ps.setLong(21, eventFace.getSourceId)
    ps.setInt(22, eventFace.getSourceType)
    ps.setString(23, eventFace.getSite)
    ps.setTimestamp(24, eventFace.getTime)
    ps.setTimestamp(25, Timestamp.valueOf(eventFace.getCreateTime.toLocalDateTime.withNano(0)))
    ps.setFloat(26, eventFace.getFeatureQuality)
    ps.setTimestamp(27, eventFace.getSaveTime)
    ps.setString(28, eventFace.getColumn1)
    ps.setString(29, eventFace.getColumn2)
    ps.setString(30, eventFace.getColumn3)
  }

  private def saveToDB(eventFaces: ArrayBuffer[EventFace]): Unit = {
    var conn: Connection = null
    var ps: PreparedStatement = null
    try {
      conn = ConnectionPool(config.dbcpProperties)
      conn.setAutoCommit(false)
      ps = conn.prepareStatement(writeSql())
      var amount = 0
      var i = 0
      for (eventFace <- eventFaces) {
        if (i >= config.dwdBatchSize) {
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

  private def writeSql(): String = {
    val holders = StringUtils.join(JavaConversions.seqAsJavaList((1 to 30).map(_ => "?")), ",")
    s"insert into ${config.dwdTable} (sys_code, thumbnail_id, thumbnail_url, image_id, image_url, " +
      "feature_info, algo_version, gender_info, age_info, hairstyle_info, " +
      "hat_info, glasses_info, race_info, mask_info, skin_info, " +
      "pose_info, quality_info, target_rect, target_rect_float, land_mark_info, " +
      "source_id, source_type, site, time, create_time, " +
      "feature_quality, save_time, column1, column2, column3) " + s"VALUES($holders)"
  }
}

object StreamingPreProcessJob {

  def apply(config: ConfigArgs, command: CommandLineArgs): StreamingPreProcessJob =
    new StreamingPreProcessJob(config, command)

  def main(args: Array[String]): Unit = {

    // 配置参数和命令行参数
    val config: ConfigArgs = ConfigArgs()
    val command: CommandLineArgs = new CommandLineArgs(args)

    val job: StreamingPreProcessJob = StreamingPreProcessJob(config, command)
    job.processDStreamFromKafkaOffset()

    job.getSparkStream.start()
    job.getSparkStream.awaitTermination()
  }
}
