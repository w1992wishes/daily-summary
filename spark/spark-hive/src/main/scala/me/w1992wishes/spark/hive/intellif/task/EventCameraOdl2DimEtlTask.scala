package me.w1992wishes.spark.hive.intellif.task

import me.w1992wishes.spark.hive.intellif.param.EventCameraOdl2DimEtlCLParam
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Row, SaveMode, SparkSession}

/**
  * 摄像头提取任务，摄像头是小数量数据
  *
  * @author w1992wishes 2020/3/9 14:42.
  */
object EventCameraOdl2DimEtlTask {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    val clParam = new EventCameraOdl2DimEtlCLParam(args)
    val bizCode = clParam.bizCode
    val sourceTable = s"${bizCode}_odl.odl_${bizCode}_event_camera"
    val sinkTable = s"${bizCode}_dim.dim_${bizCode}_event_camera"

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
    val spark = SparkSession.builder()
      .config(conf)
      .appName(getClass.getSimpleName)
      .enableHiveSupport() // 启用 hive
      .getOrCreate()

    import spark.implicits._
    import spark.sql

    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_odl")
    sql(s"USE ${bizCode}_odl")
    val cameraDF = sql(s"SELECT id, name, ip, geo_string FROM $sourceTable")
    cameraDF.show(false)
    cameraDF.printSchema()

    val parseCameraDF = cameraDF
      .na.drop(Array("geo_string"))
      .map{case Row(id: Long, name: String, ip: String, geo_string: String) =>
        val (lat, lon) = calLatLon(geo_string)
        Camera(String.valueOf(id), name, ip, lat, lon)
      }

    // 全量覆盖
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_dim")
    sql(s"USE ${bizCode}_dim")
    sql(
      s"""
         | CREATE TABLE IF NOT EXISTS $sinkTable (
         |   camera_id string,
         |   name string,
         |   ip string,
         |   lat double,
         |   lon double)
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

  case class Camera(camera_id: String, name: String, ip: String, lat: Double, lon: Double)

  def calLatLon(geo: String): (Double, Double) = {
    // POINT(114.124500 22.515400)
    val latLon = geo.toLowerCase().replace("point(", "").replace(")", "").split(" ")
    // lat lon
    (latLon(1).toDouble, latLon(0).toDouble)
  }
}




