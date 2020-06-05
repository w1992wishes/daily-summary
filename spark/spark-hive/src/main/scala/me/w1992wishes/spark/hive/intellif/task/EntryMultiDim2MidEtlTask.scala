package me.w1992wishes.spark.hive.intellif.task

import me.w1992wishes.spark.hive.intellif.param.EntryMultiDim2MidEtlCLParam
import org.apache.commons.lang3.StringUtils
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * 多维实体etl，car/imei/imsi/mac 等公用该 task
  *
  * @author w1992wishes 2019/11/25 15:01
  */
object EntryMultiDim2MidEtlTask {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    val clParam = new EntryMultiDim2MidEtlCLParam(args)

    // 参数
    val dataType = clParam.dataType // 必传
    val bizCode = clParam.bizCode
    val dt = clParam.dt
    val debug = clParam.debug
    val shufflePartitions = clParam.shufflePartitions
    val sourceTable = s"${bizCode}_dim.dim_${bizCode}_event_multi"
    val sinkTable = s"${bizCode}_mid.mid_${bizCode}_entry_multi"
    val entryTableDir = "/user/hive/warehouse/entry/multi" // 实体保存位置
    val entryDataDir = s"$entryTableDir/data_type=$dataType" // 实体保存位置

    if (StringUtils.isEmpty(dataType) ||
      !(dataType.equals("car") || dataType.equals("mac") || dataType.equals("imsi") || dataType.equals("imei") || dataType.equals("person"))) {
      System.err.println("dataType is need be car|max|imsi|imei|person.")
      System.exit(1)
    }

    // spark 参数设置
    val conf = new SparkConf()
      .setIfMissing("spark.master", "local[8]")
      .set("spark.sql.adaptive.enabled", "true")
      .set("spark.sql.adaptive.shuffle.targetPostShuffleInputSize", "67108864b")
      .set("spark.sql.adaptive.join.enabled", "true")
      .set("spark.sql.autoBroadcastJoinThreshold", "20971520")
      .set("spark.sql.broadcastTimeout", "600000ms")
      .set("spark.hadoopRDD.ignoreEmptySplits", "true")
      .set("spark.debug.maxToStringFields", "100")
      .set("spark.sql.warehouse.dir", "hdfs://nameservice1/sql/metadata/hive")
      .set("spark.sql.catalogImplementation", "hive")
      .set("spark.sql.shuffle.partitions", s"$shufflePartitions")

    // 初始化 spark
    val spark = SparkSession.builder()
      .appName(getClass.getSimpleName)
      .enableHiveSupport() // 启用 hive
      .config(conf)
      .getOrCreate()

    import spark.sql

    // 实体表抽取全量实体 id
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_mid")
    sql(s"USE ${bizCode}_mid")
    sql(
      s"""
         | CREATE TABLE IF NOT EXISTS $sinkTable
         |  (aid string,
         |  biz_code string,
         |  props string,
         |  create_time timestamp,
         |  modify_time timestamp
         |  )
         | PARTITIONED BY (data_type string comment "按类型分区")
         | STORED AS PARQUET
         | LOCATION 'hdfs://nameservice1$entryTableDir'
      """.stripMargin)
    val entryAidsDF = sql(
      s"""
         | SELECT
         |   aid
         | FROM $sinkTable
         | WHERE data_type = '$dataType'
       """.stripMargin
    )
    entryAidsDF.show(false)
    if (debug) println(s"${dataType}实体总量：${entryAidsDF.count()}")

    // 事件表根据分区提取事件
    spark.sql(s"USE ${bizCode}_dim")
    val eventDF = spark.sql(
      s"""
         | SELECT
         |  aid,
         |  biz_code,
         |  props
         | FROM $sourceTable
         | WHERE dt = $dt AND data_type = '$dataType'
       """.stripMargin)
      .na.drop(cols = Array("aid")) // 非空
      .dropDuplicates("aid") // 去重
    eventDF.show(false)

    // 取差集，为新增的实体档案 id
    val addedAidDF = eventDF.select("aid").except(entryAidsDF.select("aid"))
    // 补足新增实体的其他属性
    val addedEntryDF = eventDF.join(org.apache.spark.sql.functions.broadcast(addedAidDF), "aid")
    addedEntryDF.createOrReplaceTempView("addedEntry")
    // 简单做个转换，添加时间字段
    val waitAddedEntryDF = sql(
      s"""
         | SELECT
         |  aid,
         |  biz_code,
         |  props,
         |  current_timestamp() as create_time,
         |  current_timestamp() as modify_time
         | FROM addedEntry
       """.stripMargin
    )
    waitAddedEntryDF.show()
    if (debug) println(s"待新增${dataType}实体总量：${waitAddedEntryDF.count()}")

    // 保存写入文件中
    waitAddedEntryDF.write.mode(SaveMode.Append).parquet(s"hdfs://nameservice1$entryDataDir")

    // load
    sql(s"ALTER TABLE $sinkTable DROP IF EXISTS PARTITION(data_type ='$dataType')")
    sql(s"ALTER TABLE $sinkTable ADD PARTITION(data_type ='$dataType') location '$entryDataDir'")

    val entryDF = spark.sql(s"SELECT * FROM $sinkTable where data_type = '$dataType'")
    entryDF.show(false)
    if (debug) println(s"新增后${dataType}实体总量：${entryDF.count()}")

    spark.stop()
  }
}