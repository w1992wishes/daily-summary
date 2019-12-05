package me.w1992wishes.spark.sql.`case`.task

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.{Date, UUID}

import ch.hsr.geohash.GeoHash
import com.alibaba.fastjson.JSONObject
import me.w1992wishes.common.util.DateUtil
import me.w1992wishes.spark.sql.`case`.config.{PersonEventBatchEtlArgsTool, PropertiesTool}
import org.apache.spark.sql.functions.{max, min}
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * @author w1992wishes 2019/11/25 15:01
  */
class PersonEventBatchEtlTask(propsTool: PropertiesTool, eventType: String = "person") extends BatchEtlTask(propsTool: PropertiesTool) {

  private def getParalleledCondition(realPartitions: Int, time: Option[Timestamp]): Array[String] = {
    time match {
      case None => Range(0, realPartitions).map(partition => s"CAST(hash_code(thumbnail_id) as numeric) % $realPartitions = $partition").toArray
      case Some(timestamp) => Range(0, realPartitions).map(partition => s"create_time > '$timestamp' and CAST(hash_code(thumbnail_id) as numeric) % $realPartitions = $partition").toArray
    }
  }

  private def dbtable(sourceTable: String, index: Int, paralleledCondition: Array[String]): String = {
    s"(SELECT aid, sys_code biz_code, thumbnail_id, thumbnail_url, image_id, " +
      s"image_url, source_id, source_type, site, time, create_time " +
      s"FROM $sourceTable WHERE ${paralleledCondition(index)}) AS t_tmp_$index"
  }

  private def getEvents(spark: SparkSession, partitions: Int) = {
    createHistoryTable()
    val start = getStartTime()
    val paralleledCondition = getParalleledCondition(partitions, start)
    Range(0, partitions)
      .map(index => {
        spark
          .read
          .format("jdbc")
          .option("driver", propsTool.getString("source.driver"))
          .option("url", propsTool.getString("source.url"))
          .option("dbtable", dbtable(propsTool.getString("source.table"), index, paralleledCondition))
          .option("user", propsTool.getString("source.user"))
          .option("password", propsTool.getString("source.password"))
          .option("fetchsize", propsTool.getInt("source.fetchsize"))
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
  }

  private def getCameras(spark: SparkSession) = {
    spark
      .read
      .format("jdbc")
      .option("driver", propsTool.getString("db.camera.driver"))
      .option("url", propsTool.getString("db.camera.url"))
      .option("dbtable", cameraTable(propsTool.getString("db.camera.table")))
      .option("user", propsTool.getString("db.camera.user"))
      .option("password", propsTool.getString("db.camera.password"))
      .option("fetchsize", propsTool.getInt("db.camera.fetchsize"))
      .load()
  }

  private def cameraTable(table: String): String = {
    s"(SELECT camera_id as source_id, lat, lon FROM $table) AS t_tmp"
  }
}

object PersonEventBatchEtlTask {

  def main(args: Array[String]): Unit = {

    val argsTool = new PersonEventBatchEtlArgsTool(args)
    val propsTool = PropertiesTool(argsTool.confName)
    val task = PersonEventBatchEtlTask(propsTool)

    val spark = SparkSession.builder()
      //.master("local[20]")
      .appName(getClass.getSimpleName)
      .config("spark.sql.shuffle.partitions", argsTool.shufflePartitions)
      .config("spark.debug.maxToStringFields", "100")
      .getOrCreate()

    val ds = task.getEvents(spark, argsTool.partitions)
    ds.cache()

    val cameras = task.getCameras(spark)

    val eventsWithGeo = ds
      .join(org.apache.spark.sql.functions.broadcast(cameras), Seq("source_id"), "left")
      .na.drop(Array("lat", "lon", "time", "aid"))
    eventsWithGeo.createOrReplaceTempView("t_event_geo")

    spark.udf.register("makeGeoHash", makeGeoHash _)
    spark.udf.register("makeId", makeId _)
    spark.udf.register("makeDt", makeDt _)
    spark.udf.register("makeProps", makeProps _)
    spark.udf.register("makeLocation", makeLocation _)
    spark.udf.register("makeCreateTime", makeCreateTime _)

    val bizCode = propsTool.getString("biz.code", "bigdata")
    // import spark.implicits._
    val personEvents = spark.sql(s"select 'person' as data_type, aid, makeId() as id, '$bizCode' as biz_code, time, " +
      s"makeDt(time) as dt, makeProps(thumbnail_id, thumbnail_url, image_id, image_url, source_id, source_type) as props, " +
      s"makeCreateTime() as create_time, makeLocation(lat, lon) as location, makeGeoHash(lat, lon) as geo_hash " +
      s"from t_event_geo")

    personEvents
      .write
      .mode(SaveMode.Append)
      .format("jdbc")
      .option("driver", propsTool.getString("sink.driver"))
      .option("url", propsTool.getString("sink.url"))
      .option("dbtable", propsTool.getString("sink.table"))
      .option("user", propsTool.getString("sink.user"))
      .option("password", propsTool.getString("sink.password"))
      .option("batchsize", propsTool.getInt("sink.batchsize"))
      .save()

    val createTimeColumn = ds.col("create_time")
    val time = ds.agg(min(createTimeColumn), max(createTimeColumn)).head.getAs[Timestamp](1)
    task.updateStartTime(time = time)
    ds.unpersist()

    spark.stop()
  }

  private def makeCreateTime: Timestamp = Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))

  private def makeId: String = {
    UUID.randomUUID().toString
  }

  private def makeDt(timestamp: Timestamp): String = {
    DateUtil.dateYMDToStr(new Date(timestamp.getTime))
  }

  private def makeProps(thumbnail_id: String, thumbnail_url: String, image_id: String, image_url: String, source_id: String, source_type: String): String = {
    val json = new JSONObject()
    json.put("thumbnail_id", thumbnail_id)
    json.put("thumbnail_url", thumbnail_url)
    json.put("image_id", image_id)
    json.put("image_url", image_url)
    json.put("source_id", source_id)
    json.put("source_type", source_type)
    json.toJSONString
  }

  private def makeGeoHash(lat: Double, lon: Double): String = {
    GeoHash.withCharacterPrecision(lat, lon, 12).toBase32
  }

  private def makeLocation(lat: Double, lon: Double): String = {
    val json = new JSONObject()
    json.put("latitude", lat)
    json.put("longitude", lon)
    json.toJSONString
  }

  def apply(propsTool: PropertiesTool): PersonEventBatchEtlTask = new PersonEventBatchEtlTask(propsTool)

}