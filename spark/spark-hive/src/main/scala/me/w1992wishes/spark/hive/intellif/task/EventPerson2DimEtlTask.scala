package me.w1992wishes.spark.hive.intellif.task

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import com.alibaba.fastjson.JSONObject
import me.w1992wishes.common.util.{DateUtil, PropertiesTool}
import me.w1992wishes.spark.hive.intellif.param.EventPerson2DimEtlCLParam
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * 从天图已打完标签的人脸事件表中转换格式适配天谱多维，类型定位 person
  *
  * @author w1992wishes 2020/4/24 10:20
  */
object EventPerson2DimEtlTask {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    // 命令行参数
    val clParam = EventPerson2DimEtlCLParam(args)
    val propsTool = PropertiesTool(clParam.confName)

    val startTime = DateUtil.strToDateTime(clParam.dt.concat("000000"), DateUtil.DF_NORMAL_NO_LINE).truncatedTo(ChronoUnit.DAYS)
    val endTime = startTime.plusDays(1)
    val startTimeStr = DateUtil.dateTimeToStr(startTime)
    val endTimeStr = DateUtil.dateTimeToStr(endTime)

    println(startTimeStr)
    println(endTimeStr)

    val dt = clParam.dt
    println(dt)

    val bizCode = clParam.bizCode
    val partitions = clParam.numPartitions
    val geoLength = Math.min(12, clParam.geoLength)
    val tableDir = "/user/hive/warehouse/event/multi"
    val dataDir = s"$tableDir/$dt/data_type=person"
    val sinkTable = s"${bizCode}_dim.dim_${bizCode}_event_multi"

    // spark 参数设置
    val conf = new SparkConf()
      .setIfMissing("spark.master", "local[8]")
      .set("spark.sql.adaptive.enabled", "true")
      .set("spark.sql.adaptive.shuffle.targetPostShuffleInputSize", "67108864b")
      .set("spark.sql.adaptive.join.enabled", "true")
      .set("spark.sql.autoBroadcastJoinThreshold", "20971520")
      .set("spark.sql.broadcastTimeout", "600000ms")
      .set("spark.hadoopRDD.ignoreEmptySplits", "true")
      .set("spark.sql.warehouse.dir", "hdfs://nameservice1/sql/metadata/hive")
      .set("spark.sql.catalogImplementation", "hive")

    val spark = SparkSession
      .builder()
      .appName(getClass.getSimpleName)
      .enableHiveSupport() // 启用 hive
      .config(conf)
      .getOrCreate()

    import spark.sql

    val df = Range(0, partitions)
      .map(index => {
        val dbtable = s"(SELECT aid, thumbnail_id, thumbnail_url, image_id, image_url, source_id, source_type, site, time, column1 FROM ${propsTool.getString("source.table")} WHERE ${getParalleledCondition(partitions)(index)} AND create_time > '$startTimeStr' AND create_time <= '$endTimeStr') AS t_tmp_$index"
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
    df.show()

    // 加载 摄像头
    val cameraDF = sql(
      s"""
         | SELECT
         |  camera_id AS source_id,
         |  lat,
         |  lon
         | FROM ${bizCode}_dim.dim_${bizCode}_event_camera
       """.stripMargin)
    cameraDF.show()

    // 转换所需格式
    val eventFaceWithGeoDF = df
      .join(org.apache.spark.sql.functions.broadcast(cameraDF), Seq("source_id"), "left")
      .na.drop(Array("lat", "lon", "time", "aid"))
      .map {
        row
          /*case Row(source_id: String, aid: String, thumbnail_id: String, thumbnail_url: String, image_id: String, image_url: String,
          source_type: String, site: String, time: Timestamp, column1: String, lat: String, lon: String) */ =>
          EventPerson(row.getAs[String]("thumbnail_id"), row.getAs[String]("aid"), bizCode, row.getAs[Timestamp]("time"),
            formatProps(row.getAs[String]("thumbnail_id"), row.getAs[String]("thumbnail_url"), row.getAs[String]("image_id"), row.getAs[String]("image_url"), row.getAs[String]("source_id"), row.getAs[String]("source_type"), row.getAs[String]("site")),
            formatLocation(row.getAs[Double]("lat"), row.getAs[Double]("lon")), Timestamp.valueOf(LocalDateTime.now()), calculateGeoHash(row.getAs[Double]("lat"), row.getAs[Double]("lon"), geoLength), row.getAs[String]("column1"))
      }
    eventFaceWithGeoDF.show()

    // 写入文件
    eventFaceWithGeoDF.write.mode(SaveMode.Overwrite).parquet(s"hdfs://nameservice1/$dataDir")

    // load
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_dim")
    sql(s"USE ${bizCode}_dim")
    sql(
      s"""
         | CREATE EXTERNAL TABLE IF NOT EXISTS $sinkTable (
         |   id string,
         |   aid string,
         |   biz_code string,
         |   time timestamp,
         |   props string,
         |   location string,
         |   create_time timestamp,
         |   geo_hash string,
         |   guid string
         |  )
         | PARTITIONED BY (dt string comment "按天分区", data_type string comment "按类型分区")
         | STORED AS PARQUET
         | LOCATION 'hdfs:$tableDir'
           """.stripMargin)
    sql(s"ALTER TABLE $sinkTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='person')")
    sql(s"ALTER TABLE $sinkTable ADD PARTITION(dt= '$dt', data_type ='person') location '$dataDir'")

    sql(s"SELECT * FROM $sinkTable WHERE dt = '$dt' and data_type ='person'").show()

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

  case class EventPerson(id: String, aid: String, biz_code: String, time: Timestamp, props: String, location: String, create_time: Timestamp, geo_hash: String, guid: String)

}