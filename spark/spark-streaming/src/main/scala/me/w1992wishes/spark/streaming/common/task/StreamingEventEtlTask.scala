package me.w1992wishes.spark.streaming.common.task

import java.sql.{Connection, PreparedStatement, Timestamp}
import java.util.{Date, UUID}

import ch.hsr.geohash.GeoHash
import com.alibaba.fastjson.{JSON, JSONObject}
import me.w1992wishes.common.util.{ConnectionPool, DateUtils}
import me.w1992wishes.spark.streaming.common.ability.DbcpSupportAbility
import me.w1992wishes.spark.streaming.common.config.{EventEtlArguments, StreamingConfig, TaskArguments}
import me.w1992wishes.spark.streaming.common.core.StreamingTask
import me.w1992wishes.spark.streaming.common.domain.{MultiData, TrackEventInfo}
import org.apache.commons.lang3.StringUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.kafka010.HasOffsetRanges

import scala.collection.mutable.ArrayBuffer

/**
  * @author w1992wishes 2019/11/22 10:41
  */
class StreamingEventEtlTask(taskArguments: EventEtlArguments, streamingConfig: StreamingConfig)
  extends StreamingTask(taskArguments: TaskArguments, streamingConfig: StreamingConfig) with DbcpSupportAbility {

  def convertToEvent(data: JSONObject, dataType: String): Array[TrackEventInfo] = {
    val trackEvents = new ArrayBuffer[TrackEventInfo]()

    val trackEvent = new TrackEventInfo

    val time = DateUtils.strToDateNormal(data.getString("time"))
    trackEvent.setTime(time)
    trackEvent.setDt(DateUtils.dateYMDToStr(time))
    trackEvent.setProps(data.toJSONString)

    val location = new JSONObject()
    val latitude = data.getDouble("latitude")
    val longitude = data.getDouble("longitude")
    if (latitude != null && longitude != null) {
      trackEvent.setGeoHash(GeoHash.withCharacterPrecision(latitude, longitude, 12).toBase32)
    }
    location.put("latitude", latitude)
    location.put("longitude", longitude)
    trackEvent.setLocation(location.toJSONString)

    trackEvent.setId(UUID.randomUUID().toString)

    trackEvent.setDataType(dataType)
    trackEvent.setCreateTime(new Date())
    trackEvent.setBizCode(streamingConfig.getString("bizCode", "bigdata"))
    trackEvent.setAid(data.getString(dataType))

    trackEvents += trackEvent

    if (StringUtils.isNotEmpty(data.getString("imei"))) {
      val imei = trackEvent.clone().asInstanceOf[TrackEventInfo]
      imei.setDataType("imei")
      imei.setId(UUID.randomUUID().toString)
      trackEvents += imei
    }

    trackEvents.toArray
  }

  def transfer(multiDataRdd: RDD[MultiData]): RDD[TrackEventInfo] = {
    multiDataRdd.filter(multiData => "insert".equals(multiData.getOperation))
      .map(multiData => {
        val trackEvents = new ArrayBuffer[TrackEventInfo]
        for (data <- multiData.getDatas) {
          val trackEventArray = convertToEvent(data, multiData.getType)
          trackEvents ++= trackEventArray
        }
        trackEvents.toArray
      })
      .flatMap(trackEventInfos => {
        for (trackEventInfo <- trackEventInfos) yield trackEventInfo
      })
  }

  def doAction(trackEventRdd: RDD[TrackEventInfo]): Unit = {
    trackEventRdd.foreachPartition(iter => partitionFunc(iter))
  }

  def partitionFunc(iter: Iterator[TrackEventInfo]): Unit = {

    val macs = new ArrayBuffer[TrackEventInfo]()
    val imsis = new ArrayBuffer[TrackEventInfo]()
    val cars = new ArrayBuffer[TrackEventInfo]()
    val imeis = new ArrayBuffer[TrackEventInfo]()

    iter.foreach(trackEvent => {
      trackEvent.getDataType.toLowerCase match {
        case "car" => cars += trackEvent
        case "mac" => macs += trackEvent
        case "imsi" => imsis += trackEvent
        case "imei" => imeis += trackEvent
        case _ => trackEvent
      }
    })

    save(macs.toArray, "mac")
    save(imsis.toArray, "imsi")
    save(cars.toArray, "car")
    save(imeis.toArray, "imei")
  }

  private def save(events: Array[TrackEventInfo], prefix: String): Unit = {
    if (events.isEmpty) {
      return
    }
    var conn: Connection = null
    var ps: PreparedStatement = null
    val dbcpProperties = initDbcpProperties(streamingConfig, prefix)
    val table = streamingConfig.getString(s"$prefix.table", s"bdl_bigdata_event_$prefix")
    try {
      conn = ConnectionPool(dbcpProperties)
      conn.setAutoCommit(false)
      ps = conn.prepareStatement(writeSql(table))
      var i = 0
      for (trackEvent <- events) {
        if (i >= 5000) {
          i = 0
          ps.executeBatch()
          conn.commit()
        }
        i += 1
        fillPreparedStatement(ps, trackEvent)
        ps.addBatch()
      }
      ps.executeBatch()
      conn.commit()
    } finally {
      ConnectionPool.closeResource(conn, ps, rs = null)
    }
  }

  private def fillPreparedStatement(ps: PreparedStatement, trackEvent: TrackEventInfo): Unit = {
    ps.setString(1, trackEvent.getDataType)
    ps.setString(2, trackEvent.getId)
    ps.setString(3, trackEvent.getAid)
    ps.setString(4, trackEvent.getBizCode)
    ps.setTimestamp(5, new Timestamp(trackEvent.getTime.getTime))
    ps.setString(6, trackEvent.getDt)
    ps.setString(7, trackEvent.getProps)
    ps.setTimestamp(8, new Timestamp(trackEvent.getCreateTime.getTime))
    ps.setString(9, trackEvent.getLocation)
    ps.setString(10, trackEvent.getGeoHash)
  }

  def writeSql(table: String): String = {
    s"insert into $table (data_type, id, aid, biz_code, time, " +
      "dt, props, create_time, location, geo_hash) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
  }

}

object StreamingEventEtlTask {

  def main(args: Array[String]): Unit = {
    val eventArguments: EventEtlArguments = EventEtlArguments(args.toList)

    val streamingConfig: StreamingConfig = StreamingConfig(eventArguments.confName)

    val task = StreamingEventEtlTask(eventArguments, streamingConfig)

    val stream = task.createStream()

    // 消费数据
    stream
      .foreachRDD(kafkaRdd => {
        if (!kafkaRdd.isEmpty()) {
          val offsetRanges = kafkaRdd.asInstanceOf[HasOffsetRanges].offsetRanges
          println(s"****** Start processing RDD data ******")
          /*task.kafkaOffset.updateOffset(offsetRanges)*/

          var multiDataRdd = kafkaRdd.map(json => JSON.parseObject(json.value(), classOf[MultiData]))

          val trackEventRdd = task.transfer(multiDataRdd.repartition(eventArguments.partitions))

          task.doAction(trackEventRdd)

          println("****** End processing RDD data ******")

          task.kafkaOffset.commitOffset(offsetRanges)
        }
      })

    task.getStreamingContext.start()
    task.addCloseServer()
    task.getStreamingContext.awaitTermination()
  }

  def apply(eventArguments: EventEtlArguments, streamingConfig: StreamingConfig): StreamingEventEtlTask =
    new StreamingEventEtlTask(eventArguments, streamingConfig)

}

