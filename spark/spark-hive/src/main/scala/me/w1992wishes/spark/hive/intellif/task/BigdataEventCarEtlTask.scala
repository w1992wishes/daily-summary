package me.w1992wishes.spark.hive.intellif.task

import java.sql.Timestamp
import java.time.LocalDateTime

import ch.hsr.geohash.GeoHash
import com.alibaba.fastjson.{JSON, JSONObject}
import me.w1992wishes.spark.hive.intellif.param.BigdataEventCarCLParam
import me.w1992wishes.spark.hive.intellif.util.DateUtil
import org.apache.commons.lang3.StringUtils
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

import scala.collection.mutable.ArrayBuffer

/**
  * @author w1992wishes 2020/4/24 10:20
  */
object BigdataEventCarEtlTask {

  def main(args: Array[String]): Unit = {
    val params = BigdataEventCarCLParam(args)

    // spark 参数设置
    val conf = new SparkConf()
    conf.set("spark.sql.adaptive.enabled", "true")
    conf.set("spark.sql.adaptive.shuffle.targetPostShuffleInputSize", "67108864b")
    conf.set("spark.sql.adaptive.join.enabled", "true")
    conf.set("spark.sql.autoBroadcastJoinThreshold", "20971520")

    val spark = SparkSession
      .builder()
      .appName(getClass.getSimpleName)
      .enableHiveSupport() // 启用 hive
      //.master("local")
      .config(conf)
      .getOrCreate()

    import spark.implicits._
    import spark.sql

    // 加载 odl 层原始 car 数据
    val bizCode = params.bizCode
    val date = params.date
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_odl")
    sql(s"USE ${bizCode}_odl")
    sql(s"CREATE EXTERNAL TABLE IF NOT EXISTS odl_${bizCode}_event_target (contents string) partitioned by (date string) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\001' STORED AS TEXTFILE LOCATION 'hdfs:/user/flume'")
    sql(s"ALTER TABLE odl_${bizCode}_event_target DROP IF EXISTS PARTITION(date= '$date')")
    sql(s"ALTER TABLE odl_${bizCode}_event_target ADD PARTITION(date= '$date') location '/user/flume/$date'")

    val contentsDF = sql(s"SELECT contents FROM odl_${bizCode}_event_target WHERE date='$date'")
    contentsDF.show()

    // 转为 json 方便读取
    val jsonRDD = contentsDF.rdd.map {
      row => JSON.parseObject(row.getAs[String]("contents"))
    }.filter(json => json != null && "insert".equalsIgnoreCase(json.getString("operation")))
      .map(json => {
        val jsonArray = json.getJSONArray("datas")
        val jsonObjects = new ArrayBuffer[JSONObject]
        for (i <- 0 until jsonArray.size()) {
          jsonObjects += jsonArray.getJSONObject(i)
        }
        jsonObjects.toArray
      })
      .flatMap(jsonArray => {
        for (json <- jsonArray) yield json
      })

    // 非空才进行下一步
    if (!jsonRDD.isEmpty()) {
      // 转为 EventCar
      val eventCarRDD = jsonRDD.filter(json => "car".equalsIgnoreCase(json.getString("targetType")))
        .map(json => {
          EventCar("car", json.getString("tid"), json.getString("plateNumber"), biz_code = params.bizCode, json.getTimestamp("time"), json.toJSONString, json.getString("sourceId"))
        })
      eventCarRDD.toDF().show()

      // 摄像头
      sql(s"USE ${bizCode}_odl")
      val cameraDF = sql(s"SELECT camera_id AS source_id, lat, lon FROM odl_${bizCode}_event_camera")

      // join
      val eventCarDF = eventCarRDD.toDF()
        .join(org.apache.spark.sql.functions.broadcast(cameraDF), Seq("source_id"), "left")
        .na.drop(Array("lat", "lon", "time", "aid"))
      eventCarDF.show()
      eventCarDF.createOrReplaceTempView("carEvent")
      spark.udf.register("calculateGeoHash", calculateGeoHash _)
      spark.udf.register("formatLocation", formatLocation _)

      val eventCarWithGeoDF = spark.sql(
        """
          | SELECT data_type, id, aid, biz_code, time, props, formatLocation(lat, lon) as location, calculateGeoHash(lat, lon) as geo_hash
          |   FROM carEvent
        """.stripMargin
      )
      eventCarWithGeoDF.show()

      // join 后写入文件
      val dataDir = s"/user/hive/warehouse/event/car/$date"
      val path = new Path(dataDir)
      val hadoopConf = spark.sparkContext.hadoopConfiguration
      val hdfs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
      if (hdfs.exists(path)) hdfs.delete(path, true)
      eventCarWithGeoDF.coalesce(params.coalescePartitions).write.parquet(dataDir)

      sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_dim")
      sql(s"USE ${bizCode}_dim")
      sql(s"CREATE EXTERNAL TABLE IF NOT EXISTS dim_${bizCode}_event_car (data_type string, id string, aid string, biz_code string, time timestamp, props string, location string, geo_hash string) PARTITIONED BY (date string) STORED AS PARQUET LOCATION 'hdfs:/user/hive/warehouse/event/car'")
      sql(s"ALTER TABLE dim_${bizCode}_event_car DROP IF EXISTS PARTITION(date= '$date')")
      sql(s"ALTER TABLE dim_${bizCode}_event_car ADD PARTITION(date= '$date') location '$dataDir'")
      sql(s"SELECT * FROM dim_${bizCode}_event_car").show()

      spark.stop()
    }

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

  case class EventCar(data_type: String, id: String, aid: String, biz_code: String = "bigdata", time: Timestamp, props: String, source_id: String)
}