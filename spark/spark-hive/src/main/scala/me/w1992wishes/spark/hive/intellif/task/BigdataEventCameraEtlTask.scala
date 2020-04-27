package me.w1992wishes.spark.hive.intellif.task

import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet}

import me.w1992wishes.spark.hive.intellif.param.BigdataEventCameraCLParam
import me.w1992wishes.spark.hive.intellif.util.{DataSourceUtil, PropertiesTool}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

import scala.collection.mutable.ArrayBuffer

/**
  * 摄像头是小数量数据
  *
  * @author w1992wishes 2020/3/9 14:42.
  */
object BigdataEventCameraEtlTask {

  def main(args: Array[String]): Unit = {

    val cameraCLParam = new BigdataEventCameraCLParam(args)
    val propsTool = PropertiesTool(cameraCLParam.confName)
    val bizCode = cameraCLParam.bizCode

    // 获取全量摄像头
    val cameras: Array[Camera] = getCamerasFromDB(propsTool)
    println("wait to import cameras num: " + cameras.length)

    // 全量覆盖
    if (!cameras.isEmpty) {
      // spark 参数设置
      val conf = new SparkConf()
      conf.set("spark.sql.adaptive.enabled", "true") // may be 已经过期
      conf.set("spark.sql.adaptive.shuffle.targetPostShuffleInputSize", "67108864b") // may be 已经过期
      conf.set("spark.sql.adaptive.join.enabled", "true")
      conf.set("spark.sql.autoBroadcastJoinThreshold", "20971520")
      conf.set("spark.sql.broadcastTimeout", "600000ms")
      conf.set("spark.hadoopRDD.ignoreEmptySplits", "true")
      conf.set("spark.hadoop.mapreduce.input.fileinputformat.split.minsize", "67108864")

      val spark = SparkSession.builder()
        // .master("local")
        .appName(getClass.getSimpleName)
        .enableHiveSupport() // 启用 hive
        .config(conf)
        .getOrCreate()

      val cameraDF = spark.createDataFrame(cameras)
      cameraDF.show()

      import spark.sql

      sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_odl")
      sql(s"USE ${bizCode}_odl")
      sql(s"CREATE TABLE IF NOT EXISTS odl_${bizCode}_event_camera (camera_id string, ip string, name string, lat double, lon double) STORED AS PARQUET")

      cameraDF.createOrReplaceTempView("eventCamera")

/*
      sql("set hive.mapred.supports.subdirectories=true")
      sql("set mapreduce.input.fileinputformat.input.dir.recursive=true")
      sql("set mapred.max.split.size=256000000")
      sql("set mapred.min.split.size.per.node=128000000")
      sql("set mapred.min.split.size.per.rack=128000000")
      sql("set hive.hadoop.supports.splittable.combineinputformat=true")
      sql("set hive.exec.compress.output=true")
      sql("set mapred.output.compression.codec=org.apache.hadoop.io.compress.GzipCodec")
      sql("set hive.input.format=org.apache.hadoop.hive.ql.io.CombineHiveInputFormat")

      sql("set hive.merge.mapfiles=true")
      sql("set hive.merge.mapredfiles=true")
      sql("set hive.merge.size.per.task=256000000")
      sql("set hive.merge.smallfiles.avgsize=256000000")

      sql("set hive.groupby.skewindata=true")

      // 保存到 hive 方法 1，网上说该方法可减少小文件，分析原理加测试证明不可靠
      sql(
        s"""
           | INSERT OVERWRITE TABLE ${bizCode}_odl.odl_${bizCode}_event_camera
           |  SELECT * FROM eventCamera
         """.stripMargin
      )*/

      // 保存到 hive 方法 2
      sql("select * from eventCamera")
        .coalesce(cameraCLParam.coalescePartitions) // 根据实际数据量合并，从而控制小文件产生
        .write
        .format("hive")
        .mode(SaveMode.Overwrite)
        .saveAsTable(s"${bizCode}_odl.odl_${bizCode}_event_camera")

      sql(s"SELECT count(camera_id) FROM ${bizCode}_odl.odl_${bizCode}_event_camera").show()

      spark.stop()
    }

  }

  private def getCamerasFromDB(propsTool: PropertiesTool): Array[Camera] = {
    val sql: String = "SELECT id AS camera_id, name, ip, substring_index( substring_index( substring_index( geo_string, '(',- 1 ), ')', 1 ), ' ',- 1 ) lat, substring_index( substring_index( geo_string, '(',- 1 ), ' ', 1 ) lon FROM t_camera_info where geo_string is not null"
    val username = propsTool.getString("source.user")
    val password = propsTool.getString("source.password")
    val url = propsTool.getString("source.url")
    var conn: Connection = null
    var ps: PreparedStatement = null
    var rs: ResultSet = null
    try {
      classOf[com.mysql.jdbc.Driver]
      conn = DriverManager.getConnection(url, username, password)
      ps = conn.prepareStatement(sql)
      rs = ps.executeQuery()
      val cameras: ArrayBuffer[Camera] = new ArrayBuffer[Camera]()
      while (rs.next() ) {
        cameras += Camera(rs.getString("camera_id"), rs.getString("name"), rs.getString("ip"), rs.getString("lat").toDouble, rs.getString("lon").toDouble)
      }
      cameras.toArray
    } finally {
      DataSourceUtil.closeResource(conn, ps, rs)
    }
  }

  case class Camera(camera_id: String, name: String, ip: String, lat: Double, lon: Double)
}




