package me.w1992wishes.spark.sql.`case`.task

import java.sql.Timestamp
import java.util.{Date, UUID}

import ch.hsr.geohash.GeoHash
import com.alibaba.fastjson.JSONObject
import me.w1992wishes.common.util.DateUtil
import me.w1992wishes.spark.sql.`case`.config.{PersonEventBatchEtlArgsTool, PropertiesTool}
import org.apache.commons.lang3.StringUtils
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
          .option("password", propsTool.getString("source.passwd"))
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
    s"(SELECT id, geo_string FROM $table WHERE geo_string like 'POINT%') AS t_tmp"
  }
}

object PersonEventBatchEtlTask {

  def main(args: Array[String]): Unit = {

    val argsTool = new PersonEventBatchEtlArgsTool(args)
    val propsTool = PropertiesTool(argsTool.confName)
    val task = PersonEventBatchEtlTask(propsTool)

    val spark = SparkSession.builder()
      .master("local[20]")
      .appName(getClass.getSimpleName)
      .config("spark.sql.shuffle.partitions", argsTool.shufflePartitions)
      .config("spark.debug.maxToStringFields", "100")
      .getOrCreate()

    val ds = task.getEvents(spark, argsTool.partitions)
    ds.createOrReplaceTempView("t_person_event")
    spark.sqlContext.cacheTable("t_person_event")

    val cameras = task.getCameras(spark)
    cameras.createOrReplaceTempView("t_camera")

    spark.udf.register("makeGeoHash", makeGeoHash _)
    spark.udf.register("makeId", makeId _)
    spark.udf.register("makeDt", makeDt _)
    spark.udf.register("makeProps", makeProps _)
    spark.udf.register("makeLocation", makeLocation _)
    spark.udf.register("makeCreateTime", makeCreateTime _)
    spark.udf.register("makeBizCode", makeBizCode _)

    // import spark.implicits._
    val personEvents = spark.sql(s"select 'person' as data_type, a.aid, makeId() as id, makeBizCode(a.biz_code) as biz_code, a.time, " +
      s"makeDt(a.time) as dt, makeProps(a.thumbnail_id, a.thumbnail_url, a.image_id, a.image_url, a.source_id, a.source_type) as props, " +
      s"makeCreateTime() as create_time, makeLocation(b.geo_string) as location, makeGeoHash(b.geo_string) as geo_hash " +
      s"from t_person_event a left join t_camera b on a.source_id = b.id")

    personEvents
      .write
      .mode(SaveMode.Append)
      .format("jdbc")
      .option("driver", propsTool.getString("sink.driver"))
      .option("url", propsTool.getString("sink.url"))
      .option("dbtable", propsTool.getString("sink.table"))
      .option("user", propsTool.getString("sink.user"))
      .option("password", propsTool.getString("sink.passwd"))
      .option("batchsize", propsTool.getInt("sink.batchsize"))
      .save()

    val time = spark.sql("select max(create_time) max_time from t_person_event").head().getAs[Timestamp]("max_time")
    task.updateStartTime(time = time)

    spark.sqlContext.uncacheTable("t_person_event")
    spark.stop()

  }

  private def makeBizCode(bizCode: String): String = {
    if (StringUtils.isEmpty(bizCode) || bizCode.startsWith("DeepEye")) {
      "bigdata"
    } else {
      bizCode
    }
  }

  private def makeCreateTime: Timestamp = {
    new Timestamp(new Date().getTime)
  }

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

  private def makeGeoHash(site: String): String = {
    if (StringUtils.isNotEmpty(site) && site.startsWith("POINT")) {
      val temp = site.replace("POINT(", "").replace(")", "").split(" ")
      val lon = temp(0).toDouble
      val lat = temp(1).toDouble
      GeoHash.withCharacterPrecision(lat, lon, 12).toBase32
    } else {
      ""
    }
  }

  private def makeLocation(site: String): String = {
    if (StringUtils.isNotEmpty(site) && site.startsWith("POINT")) {
      val temp = site.replace("POINT(", "").replace(")", "").split(" ")
      val lon = temp(0).toDouble
      val lat = temp(1).toDouble
      val json = new JSONObject()
      json.put("latitude", lat)
      json.put("longitude", lon)
      json.toJSONString
    } else {
      ""
    }
  }

  def apply(propsTool: PropertiesTool): PersonEventBatchEtlTask = new PersonEventBatchEtlTask(propsTool)

}