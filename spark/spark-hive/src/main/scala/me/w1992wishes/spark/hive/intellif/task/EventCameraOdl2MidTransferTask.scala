package me.w1992wishes.spark.hive.intellif.task

import me.w1992wishes.spark.hive.intellif.param.EventCameraOdl2MidTransferCLParam
import org.apache.commons.lang3.StringUtils
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * 摄像头提取任务，摄像头是小数量数据
  *
  * @author w1992wishes 2020/3/9 14:42.
  */
object EventCameraOdl2MidTransferTask {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    val clParam = new EventCameraOdl2MidTransferCLParam(args)
    val bizCode = clParam.bizCode
    val sourceTable = s"${bizCode}_odl.odl_${bizCode}_event_camera"
    val sinkTable = s"${bizCode}_mid.mid_${bizCode}_event_camera"

    // 获取全量摄像头
    // spark 参数设置
    val conf = new SparkConf()
      .setIfMissing("spark.master", "local")
      .set("spark.sql.adaptive.enabled", "true")
      .set("spark.sql.adaptive.shuffle.targetPostShuffleInputSize", "67108864b")
      .set("spark.sql.adaptive.join.enabled", "true")
      .set("spark.sql.autoBroadcastJoinThreshold", "20971520")
      .set("spark.sql.broadcastTimeout", "600000ms")
      .set("spark.hadoopRDD.ignoreEmptySplits", "true")
      .set("spark.sql.warehouse.dir","hdfs://nameservice1/user/hive/warehouse")
      .set("spark.sql.catalogImplementation", "hive")
    val spark = SparkSession.builder()
      .config(conf)
      .appName(getClass.getSimpleName)
      .enableHiveSupport() // 启用 hive
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    import spark.implicits._
    import spark.sql

    sql(s"USE ${bizCode}_odl")
    val cameraDF = sql(s"SELECT biz_code, id, name, ip, geo_string, props FROM $sourceTable")
    cameraDF.show(false)
    cameraDF.printSchema()

    val parseCameraDF = cameraDF.map{
      row => {
        val id = row.getAs[Long]("id")
        val name = row.getAs[String]("name")
        val ip = row.getAs[String]("ip")
        val geo_string = row.getAs[String]("geo_string")
        val props = row.getAs[String]("props")
        val (lat, lon) = calLatLon(geo_string)
        Camera(bizCode, String.valueOf(id), name, ip, lat.getOrElse(0.0d), lon.getOrElse(0.0d), "")
      }
    }

    // 全量覆盖
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_mid")
    sql(s"USE ${bizCode}_mid")
    sql(
      s"""
         | CREATE TABLE IF NOT EXISTS $sinkTable (
         |   biz_code string,
         |   camera_id string,
         |   name string,
         |   ip string,
         |   lat double,
         |   lon double,
         |   props string)
         | STORED AS PARQUET
       """.stripMargin)
    parseCameraDF
      .write
      .format("hive")
      .mode(SaveMode.Overwrite)
      .saveAsTable(s"$sinkTable")

    sql(s"SELECT count(camera_id) FROM $sinkTable").show()

    spark.stop()

  }

  case class Camera(biz_code: String, camera_id: String, name: String, ip: String, lat: Double, lon: Double, props: String)

  def calLatLon(geo: String): (Option[Double], Option[Double]) = {
    // POINT(114.124500 22.515400)
    if (StringUtils.isEmpty(geo)) {
      (Option.empty[Double],  Option.empty[Double])
    } else {
      val latLon = geo.toLowerCase().replace("point(", "").replace(")", "").split(" ")
      // lat lon
      (Option(latLon(1).toDouble), Option(latLon(0).toDouble))
    }
  }
}




