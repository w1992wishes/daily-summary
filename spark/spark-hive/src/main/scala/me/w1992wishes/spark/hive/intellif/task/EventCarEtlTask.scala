package me.w1992wishes.spark.hive.intellif.task

import java.sql.Timestamp

import ch.hsr.geohash.GeoHash
import com.alibaba.fastjson.{JSON, JSONObject}
import me.w1992wishes.spark.hive.intellif.param.EventCarEtlCLParam
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

import scala.collection.mutable.ArrayBuffer

/**
  * @author w1992wishes 2020/4/24 10:20
  */
object EventCarEtlTask {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    // 命令行参数
    val clParam = EventCarEtlCLParam(args)
    val bizCode = clParam.bizCode
    val date = clParam.date
    val dataDir = s"/user/hive/warehouse/event/car"
    val sourceTable = s"${bizCode}_odl.odl_${bizCode}_event_target"
    val sinkTable = s"${bizCode}_dim.dim_${bizCode}_event_car"
    val isCoalesce = clParam.isCoalesce
    val coalescePartitions = clParam.coalescePartitions

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

    // 加载 odl 层原始 car 数据
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_odl")
    sql(s"USE ${bizCode}_odl")
    sql(s"CREATE EXTERNAL TABLE IF NOT EXISTS $sourceTable (contents string) partitioned by (date string) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\001' STORED AS TEXTFILE LOCATION 'hdfs:/user/flume'")
    sql(s"ALTER TABLE $sourceTable DROP IF EXISTS PARTITION(date= '$date')")
    sql(s"ALTER TABLE $sourceTable ADD PARTITION(date= '$date') location '/user/flume/$date'")

    val contentsDF = sql(s"SELECT contents FROM $sourceTable WHERE date='$date'")
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
          EventCar("car", json.getString("tid"), json.getString("plateNumber"), biz_code = clParam.bizCode, json.getTimestamp("time"), json.toJSONString, json.getString("sourceId"))
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
          | SELECT
          |   data_type,
          |   id,
          |   aid,
          |   biz_code,
          |   time,
          |   props,
          |   formatLocation(lat, lon) as location,
          |   calculateGeoHash(lat, lon) as geo_hash
          | FROM carEvent
        """.stripMargin
      )
      eventCarWithGeoDF.show()

      // join 后写入文件
      val path = new Path(dataDir)
      val hadoopConf = spark.sparkContext.hadoopConfiguration
      val hdfs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
      if (hdfs.exists(path)) hdfs.delete(path, true)

      // 在现场环境中，odl 的源数据一个分区有 24GB，最后提取出来的事件大小在内存 124MB，压缩到 hdfs 64MB
      // 这种情况如果不 coalesce ，会产生很多小文件，现场一共 391 个 64MB 的源文件，对应产生 391 个 1-2 kb 的小文件
      // 但如果直接 coalesce(1)，则只有在 write 的时候触发 action，导致所有的输入都汇聚到一个 reduce job 处理，24 GB 需要处理很长的时间
      // 所以这里针对具体的数据大小，做一个缓存，同时由 count 触发缓存起来，然后再 coalesce(1)，此时只有 124MB 的输入，很快就搞定。
      // 如果数据量过大，缓存到内存压力很大，则可考虑先写成小文件，后续再合并
      eventCarWithGeoDF.cache()
      println(s"总事件量：${eventCarWithGeoDF.count()}")
      if (isCoalesce)
        eventCarWithGeoDF.coalesce(coalescePartitions).write.parquet(dataDir)
      else
        eventCarWithGeoDF.write.parquet(dataDir)

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