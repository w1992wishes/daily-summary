package me.w1992wishes.spark.hive.intellif.task

import java.sql.Timestamp
import java.util.UUID

import ch.hsr.geohash.GeoHash
import com.alibaba.fastjson.JSONObject
import me.w1992wishes.spark.hive.intellif.param.EventPersonEtlCLParam
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Row, SparkSession}

/**
  * 从天图已打完标签的人脸事件表中转换格式适配天谱多维，类型定位 person
  *
  * @author w1992wishes 2020/4/24 10:20
  */
object EventPersonEtlTask {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    // 命令行参数
    val clParam = EventPersonEtlCLParam(args)
    val bizCode = clParam.bizCode
    val date = clParam.date
    val isCoalesce = clParam.isCoalesce
    val coalescePartitions = clParam.coalescePartitions
    val dataDir = s"/user/hive/warehouse/event/person"
    val sourceTable = s"${bizCode}_dim.dim_${bizCode}_event_face_5030"
    val sinkTable = s"${bizCode}_dim.dim_${bizCode}_event_person"

    // spark 参数设置
    val conf = new SparkConf()
    conf.set("spark.sql.adaptive.enabled", "true")
    conf.set("spark.sql.adaptive.shuffle.targetPostShuffleInputSize", "67108864b")
    conf.set("spark.sql.adaptive.join.enabled", "true")
    conf.set("spark.sql.autoBroadcastJoinThreshold", "20971520")
    conf.set("spark.sql.broadcastTimeout", "600000ms")
    conf.set("spark.hadoopRDD.ignoreEmptySplits", "true")

    val spark = SparkSession
      .builder()
      .appName(getClass.getSimpleName)
      .enableHiveSupport() // 启用 hive
      //.master("local")
      .config(conf)
      .getOrCreate()

    import spark.implicits._
    import spark.sql

    // 加载 event face 数据
    sql(s"USE ${bizCode}_dim")
    val eventFaceDF = sql(
      s"""
         | SELECT
         |  aid,
         |  thumbnail_id,
         |  thumbnail_url,
         |  image_id,
         |  image_url,
         |  source_id,
         |  source_type,
         |  site,
         |  time
         | FROM $sourceTable
         | WHERE date = $date
      """.stripMargin)
    eventFaceDF.show()

    // 加载 摄像头
    sql(s"USE ${bizCode}_odl")
    val cameraDF = sql(
      s"""
         | SELECT
         |  camera_id AS source_id,
         |  lat,
         |  lon
         | FROM odl_${bizCode}_event_camera
       """.stripMargin)
    cameraDF.show()

    // 转换所需格式
    val eventFaceWithGeoDF = eventFaceDF
      .join(org.apache.spark.sql.functions.broadcast(cameraDF), Seq("source_id"), "left")
      .na.drop(Array("lat", "lon", "time", "aid"))
      .map {
        case Row(aid: String, thumbnail_id: String, thumbnail_url: String, image_id: String, image_url: String,
        source_id: String, source_type: String, site: String, time: Timestamp, lat: Double, lon: Double) =>
          EventPerson("person", generateId, aid, bizCode, time, formatProps(thumbnail_id, thumbnail_url, image_id, image_url, source_id, source_type, site),
            formatLocation(lat, lon), calculateGeoHash(lat, lon))
      }
    eventFaceWithGeoDF.show()

    // 写入文件
    val path = new Path(dataDir)
    val hadoopConf = spark.sparkContext.hadoopConfiguration
    val hdfs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
    if (hdfs.exists(path)) hdfs.delete(path, true)

    if (isCoalesce)
      eventFaceWithGeoDF.coalesce(coalescePartitions).write.parquet(dataDir)
    else
      eventFaceWithGeoDF.write.parquet(dataDir)

    // load
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_dim")
    sql(s"USE ${bizCode}_dim")
    sql(
      s"""
         | CREATE TABLE IF NOT EXISTS $sinkTable (
         |   data_type string,
         |   id string,
         |   aid string,
         |   biz_code string,
         |   time timestamp,
         |   props string,
         |   location string,
         |   geo_hash string
         |  )
         | PARTITIONED BY (date string)
         | STORED AS PARQUET
         """.stripMargin)
    sql(s"LOAD DATA INPATH '$dataDir' OVERWRITE INTO TABLE $sinkTable PARTITION (date='$date')")

    sql(s"SELECT * FROM $sinkTable WHERE date = '$date'").show()

    spark.stop()
  }

  private def calculateGeoHash(lat: Double, lon: Double): String = {
    GeoHash.withCharacterPrecision(lat, lon, 12).toBase32
  }

  private def formatLocation(lat: Double, lon: Double): String = {
    val json = new JSONObject()
    json.put("latitude", lat)
    json.put("longitude", lon)
    json.toJSONString
  }

  private def generateId: String = {
    UUID.randomUUID().toString
  }

  private def formatProps(thumbnail_id: String, thumbnail_url: String, image_id: String, image_url: String, source_id: String, source_type: String, site: String): String = {
    val json = new JSONObject()
    json.put("thumbnail_id", thumbnail_id)
    json.put("thumbnail_url", thumbnail_url)
    json.put("image_id", image_id)
    json.put("image_url", image_url)
    json.put("source_id", source_id)
    json.put("source_type", source_type)
    json.put("site", site)
    json.toJSONString
  }

  case class EventPerson(data_type: String, id: String, aid: String, biz_code: String, time: Timestamp, props: String, location: String, geo_hash: String)

}