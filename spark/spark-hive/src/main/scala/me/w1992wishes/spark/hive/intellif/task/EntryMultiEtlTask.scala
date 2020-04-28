package me.w1992wishes.spark.hive.intellif.task

import me.w1992wishes.spark.hive.intellif.param.EntryMultiEtlCLParam
import org.apache.commons.lang3.StringUtils
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * @author w1992wishes 2019/11/25 15:01
  */
object EntryMultiEtlTask {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    val clParam = new EntryMultiEtlCLParam(args)

    // 参数
    val eventType = clParam.eventType
    val bizCode = clParam.bizCode
    val date = clParam.date
    val debug = clParam.debug
    val isCoalesce = clParam.isCoalesce
    val coalescePartitions = clParam.coalescePartitions
    val sourceTable = s"${bizCode}_dim.dim_${bizCode}_event_$eventType"
    val sinkTable = s"${bizCode}_mid.mid_${bizCode}_entry_$eventType"
    val entryDir = s"/user/hive/warehouse/entry/$eventType" // 实体保存位置

    if (StringUtils.isEmpty(eventType) ||
      !(eventType.equals("car") || eventType.equals("mac") || eventType.equals("imsi") || eventType.equals("imei"))) {
      System.err.println("eventType is need be car|max|imsi|imei.")
      System.exit(1)
    }

    // spark 参数设置
    val conf = new SparkConf()
    conf.set("spark.sql.adaptive.enabled", "true")
    conf.set("spark.sql.adaptive.shuffle.targetPostShuffleInputSize", "67108864b")
    conf.set("spark.sql.adaptive.join.enabled", "true")
    conf.set("spark.sql.autoBroadcastJoinThreshold", "20971520")
    conf.set("spark.sql.broadcastTimeout", "600000ms")
    conf.set("spark.hadoopRDD.ignoreEmptySplits", "true")
    conf.set("spark.debug.maxToStringFields", "100")

    // 初始化 spark
    val spark = SparkSession.builder()
      //.master("local[20]")
      .appName(getClass.getSimpleName)
      .enableHiveSupport() // 启用 hive
      .config("spark.sql.shuffle.partitions", clParam.shufflePartitions)
      .config(conf)
      .getOrCreate()

    // 实体表抽取全量实体 id
    spark.sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_mid")
    spark.sql(s"USE ${bizCode}_mid")
    spark.sql(
      s"""
         | CREATE TABLE IF NOT EXISTS $sinkTable(
         |  aid string,
         |  data_type string,
         |  props string,
         |  create_time timestamp,
         |  modify_time timestamp,
         |  biz_code string
         | )
         | STORED AS PARQUET
      """.stripMargin)
    val entryAidsDF = spark.sql(
      s"""
         | SELECT
         |  aid
         | FROM $sinkTable
       """.stripMargin
    )
    entryAidsDF.show()
    if (debug) println(s"实体总量：${entryAidsDF.count()}")

    // 事件表根据分区提取事件
    spark.sql(s"USE ${bizCode}_dim")
    val eventDF = spark.sql(
      s"""
         | SELECT
         |  data_type,
         |  aid,
         |  biz_code
         | FROM $sourceTable
         | WHERE date = $date
       """.stripMargin)
      .na.drop(cols = Array("aid")) // 非空
      .dropDuplicates("aid") // 去重
    eventDF.show()

    // 取差集，为新增的实体档案 id
    val addedAidDF = eventDF.select("aid").except(entryAidsDF.select("aid"))
    // 补足新增实体的其他属性
    val addedEntryDF = eventDF.join(org.apache.spark.sql.functions.broadcast(addedAidDF), "aid")
    addedEntryDF.createOrReplaceTempView("addedEntry")
    // 简单做个转换，添加时间字段
    val waitAddedEntryDF = spark.sql(
      s"""
         | SELECT
         |  aid,
         |  data_type,
         |  biz_code,
         |  current_timestamp() as create_time
         | FROM addedEntry
       """.stripMargin
    )
    waitAddedEntryDF.show()
    if (debug) println(s"待新增实体总量：${waitAddedEntryDF.count()}")

    // 保存写入文件中
    val path = new Path(entryDir)
    val hadoopConf = spark.sparkContext.hadoopConfiguration
    val hdfs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
    if (hdfs.exists(path)) hdfs.delete(path, true)
    if (isCoalesce)
      waitAddedEntryDF.coalesce(coalescePartitions).write.mode(SaveMode.Append).parquet(entryDir)
    else
      waitAddedEntryDF.write.mode(SaveMode.Append).parquet(entryDir)

    // load
    spark.sql(s"USE ${bizCode}_mid")
    spark.sql(s"LOAD DATA INPATH '$entryDir' INTO TABLE $sinkTable")

    val entryDF = spark.sql(s"SELECT * FROM $sinkTable")
    entryDF.show()
    if (debug) println(s"新增后实体总量：${entryDF.count()}")

    spark.stop()
  }
}