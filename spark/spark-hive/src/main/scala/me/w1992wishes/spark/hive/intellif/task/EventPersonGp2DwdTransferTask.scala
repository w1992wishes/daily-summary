package me.w1992wishes.spark.hive.intellif.task

import java.sql.Timestamp
import java.time.temporal.ChronoUnit

import ch.hsr.geohash.GeoHash
import com.alibaba.fastjson.JSONObject
import me.w1992wishes.common.util.{DateUtil, PropertiesTool}
import me.w1992wishes.spark.hive.intellif.param.EventPersonGp2DwdTransferCLParam
import org.apache.commons.lang3.StringUtils
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * 从天图已打完标签的人脸事件表中转换格式适配天谱多维，类型定位 person
  *
  * @author w1992wishes 2020/4/24 10:20
  */
object EventPersonGp2DwdTransferTask {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    // 命令行参数
    val clParam = EventPersonGp2DwdTransferCLParam(args)
    val propsTool = PropertiesTool(clParam.confName)

    val startTime = DateUtil.strToDateTime(clParam.dt.concat("000000"), DateUtil.DF_NORMAL_NO_LINE).truncatedTo(ChronoUnit.DAYS)
    val endTime = startTime.plusDays(1)
    val startTimeStr = if (StringUtils.isNotEmpty(clParam.startTime)) DateUtil.dateTimeToStr(DateUtil.strToDateTime(clParam.startTime, DateUtil.DF_NORMAL_NO_LINE)) else DateUtil.dateTimeToStr(startTime)
    val endTimeStr = if (StringUtils.isNotEmpty(clParam.endTime)) DateUtil.dateTimeToStr(DateUtil.strToDateTime(clParam.endTime, DateUtil.DF_NORMAL_NO_LINE)) else DateUtil.dateTimeToStr(endTime)

    println(startTimeStr)
    println(endTimeStr)

    // 命令行参数
    val dt = clParam.dt
    println(dt)
    val bizCode = clParam.bizCode
    val partitions = clParam.numPartitions
    val geoLength = Math.min(12, clParam.geoLength)
    val eventDwdTable = s"${bizCode}_dwd.dwd_${bizCode}_event_multi"
    val eventDwdTableDir = s"/user/hive/warehouse/dwd_${bizCode}_event_multi/event"
    val eventDwdDataDir = s"$eventDwdTableDir/$dt/data_type=PERSON"

    // spark 参数设置
    val conf = new SparkConf()
      .setIfMissing("spark.master", "local[16]")
      .set("spark.sql.adaptive.enabled", "true")
      .set("spark.sql.adaptive.shuffle.targetPostShuffleInputSize", "67108864b")
      .set("spark.sql.adaptive.join.enabled", "true")
      .set("spark.sql.autoBroadcastJoinThreshold", "20971520")
      .set("spark.sql.broadcastTimeout", "600000ms")
      .set("spark.hadoopRDD.ignoreEmptySplits", "true")
      .set("spark.sql.warehouse.dir", "hdfs://nameservice1/user/hive/warehouse")
      .set("spark.sql.catalogImplementation", "hive")
      .set("spark.sql.shuffle.partitions", s"$partitions")

    val spark = SparkSession
      .builder()
      .appName(getClass.getSimpleName)
      .enableHiveSupport() // 启用 hive
      .config(conf)
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    import spark.sql

    val df = Range(0, partitions)
      .map(index => {
        val dbtable =
          s"""(
             | SELECT
             |   aid,
             |   thumbnail_id,
             |   thumbnail_url,
             |   image_id,
             |   image_url,
             |   source_id,
             |   source_type,
             |   site,
             |   time,
             |   create_time,
             |   column1
             | FROM ${propsTool.getString("source.table")}
             | WHERE ${getParalleledCondition(partitions)(index)} AND create_time > '$startTimeStr' AND create_time <= '$endTimeStr') AS t_tmp_$index""".stripMargin
        println(dbtable)
        spark
          .read
          .format("jdbc")
          .option("driver", propsTool.getString("source.driver"))
          .option("url", propsTool.getString("source.url"))
          .option("dbtable", dbtable)
          .option("user", propsTool.getString("source.user"))
          .option("password", propsTool.getString("source.pass"))
          .option("fetchsize", propsTool.getInt("source.fetchsize"))
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
    println("加载人脸事件")
    df.show()

    // 加载 摄像头
    val cameraDF = sql(
      s"""
         | SELECT
         |  camera_id AS source_id,
         |  lat,
         |  lon
         | FROM ${bizCode}_mid.mid_${bizCode}_event_camera
       """.stripMargin)
    println("加载摄像头")
    cameraDF.show()

    // 转换所需格式
    val eventFaceWithGeoDF = df
      .join(org.apache.spark.sql.functions.broadcast(cameraDF), Seq("source_id"), "left")
      .na.drop(Array("lat", "lon", "time", "aid"))
      .map {
        row =>
          val lat = row.getAs[Double]("lat")
          val lon = row.getAs[Double]("lon")
          val thumbnail_url = row.getAs[String]("thumbnail_url")
          val image_id = row.getAs[String]("image_id")
          val image_url = row.getAs[String]("image_url")
          val source_id = row.getAs[String]("source_id")
          val source_type = row.getAs[String]("source_type")
          val site = row.getAs[String]("site")

          val biz_code = bizCode
          val id = row.getAs[String]("thumbnail_id")
          val data_code = row.getAs[String]("aid")
          val time = row.getAs[Timestamp]("time")
          val coll_dt = DateUtil.dateTimeToStr(time.toLocalDateTime, DateUtil.DF_YMD)
          val location = formatLocation(lat, lon)
          val geo_hash = calculateGeoHash(lat, lon, geoLength)
          val guid = row.getAs[String]("column1")
          val create_time = row.getAs[Timestamp]("create_time")
          val modify_time = null
          val sys_code = "SkyNet"
          val props = formatProps(id, thumbnail_url, image_id, image_url, source_id, source_type, site)
          DwdEventPerson(biz_code, id, data_code, time, coll_dt, location, geo_hash, guid, create_time, modify_time, sys_code, props)
      }
    println("转换后的明细数据")
    eventFaceWithGeoDF.show(false)

    // 写入文件
    eventFaceWithGeoDF.write.mode(SaveMode.Overwrite).parquet(s"hdfs://nameservice1/$eventDwdDataDir")

    // load
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_dwd")
    sql(s"USE ${bizCode}_dwd")
    sql(
      s"""
         | CREATE EXTERNAL TABLE IF NOT EXISTS $eventDwdTable (
         |   biz_code string,
         |   id string,
         |   data_code string,
         |   time timestamp,
         |   coll_dt string,
         |   location string,
         |   geo_hash string,
         |   guid string,
         |   create_time timestamp,
         |   modify_time timestamp,
         |   sys_code string,
         |   props string
         |  )
         | PARTITIONED BY (dt string comment "按天分区", data_type string comment "按类型分区")
         | STORED AS PARQUET
         | LOCATION 'hdfs://nameservice1$eventDwdTableDir'
           """.stripMargin)
    sql(s"ALTER TABLE $eventDwdTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='PERSON')")
    sql(s"ALTER TABLE $eventDwdTable ADD PARTITION(dt= '$dt', data_type ='PERSON') location '$eventDwdDataDir'")
    println("加载明细数据")
    sql(s"SELECT * FROM $eventDwdTable WHERE dt = '$dt' and data_type ='PERSON'").show()

    spark.stop()
  }

  private def calculateGeoHash(lat: Double, lon: Double, geoLength: Int = 12): String = {
    GeoHash.withCharacterPrecision(lat, lon, geoLength).toBase32
  }

  private def formatLocation(lat: Double, lon: Double): String = {
    val json = new JSONObject()
    json.put("latitude", lat)
    json.put("longitude", lon)
    json.toJSONString
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

  protected def getParalleledCondition(realPartitions: Int): Array[String] = {
    // 转换为 spark sql where 字句中的查询条件
    Range(0, realPartitions).map(partition => s"CAST(thumbnail_id as numeric) % $realPartitions = $partition").toArray
  }

  case class DwdEventPerson(biz_code: String, id: String, data_code: String, time: Timestamp, coll_dt: String, location: String, geo_hash: String, guid: String, create_time: Timestamp, modify_time: Timestamp, sys_code: String, props: String)

}