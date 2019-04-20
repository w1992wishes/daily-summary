package me.w1992wishes.spark.streaming.preprocess

import java.sql.{PreparedStatement, Timestamp}
import java.time.LocalDateTime
import java.util.Properties

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializeFilter
import me.w1992wishes.common.domain.EventFace
import me.w1992wishes.spark.streaming.preprocess.config.{CommandLineArgs, ConfigArgs}
import me.w1992wishes.spark.streaming.preprocess.core.CalculateQualityFunc
import me.w1992wishes.spark.streaming.preprocess.util.{ConnectionUtils, KafkaSender}
import org.apache.commons.lang3.StringUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext, TaskContext}

import scala.collection.JavaConversions

/**
  * partition 0 offsets 172817 182657 182657 -> 191861
  *
  * @author w1992wishes 2019/2/26 13:52
  */
object StreamingPreProcess extends Serializable with CalculateQualityFunc {

  // 配置属性
  private val config: ConfigArgs = new ConfigArgs

  def main(args: Array[String]): Unit = {

    val commandLineArgs = new CommandLineArgs(args)

    // 创建 sparkConf
    var sparkConf: SparkConf = null
    if (commandLineArgs.isLocal) {
      sparkConf = new SparkConf()
        .setMaster("local[2]")
        .setAppName(config.sparkAppName)
    } else {
      sparkConf = new SparkConf()
        .setAppName(config.sparkAppName)
    }
    // 创建sparkContext
    val sc = new SparkContext(sparkConf)
    // 创建StreamingContext
    val ssc = new StreamingContext(sc, Seconds(commandLineArgs.batchDuration))
    // 保证元数据恢复，就是Driver端挂了之后数据仍然可以恢复
    ssc.checkpoint("checkpoint")

    // 配置kafka相关参数
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> config.kafkaServers,
      "key.deserializer" -> config.kafkaKeyDeserializer,
      "value.deserializer" -> config.kafkaValueDeserializer,
      "group.id" -> config.kafkaGroupId,
      "auto.offset.reset" -> config.kafkaAutoOffsetReset
    )
    // 定义topic
    val topics = Set(config.kafkaOdlTopic)

    // 通过 KafkaUtils.createDirectStream接受kafka数据，这里采用是kafka低级api偏移量不受zk管理
    val kafkaInputStream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    val eventFaceData: DStream[EventFace] = kafkaInputStream.map(json => JSON.parseObject(json.value(), classOf[EventFace]))

    eventFaceData
      .repartition(commandLineArgs.partitions)
      .map(eventFace => {
        eventFace.setFeatureQuality(calculateFeatureQuality(config)(eventFace.getFeatureInfo, eventFace.getPoseInfo, eventFace.getQualityInfo))
        eventFace.setSaveTime(Timestamp.valueOf(LocalDateTime.now().withNano(0)))
        eventFace
      })
      .foreachRDD(rdd => rdd.foreachPartition(partitionFunc))

    //开启计算
    ssc.start()
    ssc.awaitTermination()

  }

  private def partitionFunc(iter: Iterator[EventFace]): Unit = {
    if (!iter.hasNext) {
      return
    }

    val properties: Properties = new Properties()
    properties.put("bootstrap.servers", config.kafkaServers)
    properties.put("key.serializer", config.kafkaKeySerializer)
    properties.put("value.serializer", config.kafkaValueSerializer)
    properties.put("client.id", "dwd_event_face_output")
    val sender = KafkaSender.getInstance(properties)
    val conn = ConnectionUtils.getConnection(config.dwdUrl, config.dwdUser, config.dwdPasswd)
    conn.setAutoCommit(false)
    val ps = conn.prepareStatement(writeSql())
    var amount = 0
    var i = 0
    while (iter.hasNext) {
      if (i >= config.dwdBatchsize) {
        i = 0
        ps.executeBatch()
        conn.commit()
      }
      val eventFace = iter.next()
      i += 1
      amount += 1
      fillPreparedStatement(ps, eventFace)
      ps.addBatch()
      sender.send(config.kafkaDwdTopic, JSON.toJSONString(eventFace, new Array[SerializeFilter](0)))
    }
    ps.executeBatch()
    conn.commit()
    println(s"Partition_${TaskContext.get.partitionId} preProcess $amount data totally")
    ConnectionUtils.closeResource(conn, ps, null)
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

  def writeSql(): String = {

    val holders = StringUtils.join(JavaConversions.seqAsJavaList((1 to 30).map(_ => "?")), ",")
    s"insert into ${config.dwdTable} (sys_code, thumbnail_id, thumbnail_url, image_id, image_url, " +
      "feature_info, algo_version, gender_info, age_info, hairstyle_info, " +
      "hat_info, glasses_info, race_info, mask_info, skin_info, " +
      "pose_info, quality_info, target_rect, target_rect_float, land_mark_info, " +
      "source_id, source_type, site, time, create_time, " +
      "feature_quality, save_time, column1, column2, column3) " + s"VALUES($holders)"
  }
}
