package me.w1992wishes.spark.hive.intellif.task

import java.util.UUID

import me.w1992wishes.common.util.PropertiesTool
import me.w1992wishes.spark.hive.intellif.param.CommandLineParam
import org.apache.commons.lang3.StringUtils
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * @author w1992wishes 2020/6/28 17:43
  */
object StaticDeviceDwd2MidArchiveTask {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    // 命令行参数
    val clParam = CommandLineParam(args)
    val propsTool = PropertiesTool(clParam.getConfName)
    // 解析参数
    val dt = clParam.getDt
    val dataType = clParam.getDataType
    // table
    val deviceDwdTable = propsTool.getString("StaticDeviceDwd2MidArchiveTask.deviceDwdTable")
    val deviceMidTable = propsTool.getString("StaticDeviceDwd2MidArchiveTask.deviceMidTable")

    val types = Array("DEVICE", "CAMERA")
    if (StringUtils.isEmpty(dataType) || !types.contains(dataType)) {
      System.err.println("dataType is need be DEVICE|CAMERA.")
      System.exit(1)
    }

    val conf = new SparkConf()
      .setIfMissing("spark.master", "local[16]")
      .set("spark.hadoopRDD.ignoreEmptySplits", "true")
      .set("spark.sql.catalogImplementation", "hive")
      .set("spark.io.compression.codec", "snappy")
      .set("hive.exec.dynamic.partition.mode", "nonstrict")
      .set("hive.exec.dynamic.partition", "true")
    val spark = SparkSession
      .builder()
      .config(conf)
      .appName(getClass.getSimpleName)
      .getOrCreate()
    spark.sparkContext.setLogLevel("WARN")

    import spark.sql
    import org.apache.spark.sql.functions._
    import spark.implicits._
    // 加载 mid 层所有设备信息
    val midDeviceDF = sql(
      s"""
         | SELECT
         |   biz_code,
         |   aid,
         |   data_code,
         |   status,
         |   create_time,
         |   modify_time,
         |   sys_code,
         |   props,
         |   data_type
         | FROM $deviceMidTable
         | WHERE data_type ='$dataType'
      """.stripMargin)

      .withColumn("priority", get_json_object($"props", "$.priority"))
    println("mid device")
    midDeviceDF.show()

    // 加载 dwd 层所有设备信息
    val dwdDeviceDF = sql(
      s"""
         | SELECT
         |   biz_code,
         |   null AS aid,
         |   data_code,
         |   status,
         |   create_time,
         |   modify_time,
         |   sys_code,
         |   props,
         |   data_type
         | FROM $deviceDwdTable
         | WHERE dt = '$dt' AND data_type = '$dataType'
      """.stripMargin)
      .withColumn("priority", get_json_object($"props", "$.priority"))
    println("dwd device")
    dwdDeviceDF.show()

    println("start merge")

    // union
    val allDeviceDF = midDeviceDF.union(dwdDeviceDF)
    println("after union")
    allDeviceDF.show()
    allDeviceDF.createOrReplaceTempView("allDeviceDF")

    // 合并后取优先级最高的一条
    val priorityDeviceDF = sql(
      """
        | SELECT * FROM
        | (
        |   SELECT *, ROW_NUMBER() OVER(PARTITION BY data_code ORDER BY priority DESC) as num
        |   FROM allDeviceDF
        | ) t
        | WHERE t.num = 1
      """.stripMargin)

    // 合并后没有 aid 的分配 aid
    def idGenerator(aid: String): String = {
      if (StringUtils.isEmpty(aid)) {
        UUID.randomUUID().toString.replace("-", "")
      } else {
        aid
      }
    }
    import org.apache.spark.sql.functions._
    val idGeneratorUDF = udf(idGenerator _)
    val archivedDeviceDF = priorityDeviceDF
      .withColumn("aid", idGeneratorUDF($"aid")) // 给未分配 aid 的分配 aid

    println("merge finished")

    archivedDeviceDF.show()
    archivedDeviceDF.createOrReplaceTempView("archivedDeviceDF")

    // 全量覆盖
    sql(
      s"""
         | SELECT
         |   biz_code,
         |   aid,
         |   data_code,
         |   status,
         |   create_time,
         |   CAST(UNIX_TIMESTAMP('12/31/9999', 'MM/dd/yyyy') AS TIMESTAMP) AS modify_time,
         |   sys_code,
         |   props,
         |   data_type
         | FROM archivedDeviceDF
       """.stripMargin)
      .write.mode(SaveMode.Overwrite).format("parquet").insertInto(deviceMidTable)

    println("end")
  }

}
