package me.w1992wishes.spark.hive.intellif.task

import java.util.UUID

import me.w1992wishes.spark.hive.intellif.param.EventMultiDwd2DimArchiveCLParam
import org.apache.commons.lang3.StringUtils
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * 多维事件归档
  *
  * @author w1992wishes 2020/6/15 19:45
  */
object EventMultiDwd2DimArchiveTask {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    // 命令行参数和默认设置
    val clParam = EventMultiDwd2DimArchiveCLParam(args)
    val bizCode = clParam.bizCode
    val dt = clParam.dt
    val dataType = clParam.dataType
    val debug = clParam.debug
    // 表
    val eventDwdTable = s"${bizCode}_dwd.dwd_${bizCode}_event_multi"
    val eventDimTable = s"${bizCode}_dim.dim_${bizCode}_event_multi"
    val archiveMidTable = s"${bizCode}_mid.mid_${bizCode}_archive_multi"
    val updateArchiveMidTable = s"${bizCode}_mid.mid_${bizCode}_archive_multi_update"
    // location
    val eventDimTableDir = s"/user/hive/warehouse/dim_${bizCode}_event_multi/event"
    val eventDimDataDir = s"$eventDimTableDir/$dt/data_type=$dataType"
    val archiveMidTableDir = s"/user/hive/warehouse/mid_$bizCode}_archive_multi/archive"
    val updateArchiveMidTableDir = s"/user/hive/warehouse/mid_${bizCode}_archive_multi/archive_update"
    val updateArchiveMidDataDir = s"$updateArchiveMidTableDir/data_type=$dataType"

    val types = Array("CAR", "MAC", "IMSI", "IMEI", "PERSON")
    if (StringUtils.isEmpty(dataType) || !types.contains(dataType)) {
      System.err.println("dataType is need be CAR|MAX|IMSI|IMEI|PERSON.")
      System.exit(1)
    }

    // spark 参数设置
    val conf = new SparkConf()
      .setIfMissing("spark.master", "local[16]")
      .set("spark.sql.adaptive.enabled", "true")
      .set("spark.sql.adaptive.shuffle.targetPostShuffleInputSize", "67108864b")
      .set("spark.sql.adaptive.join.enabled", "true")
      .set("spark.sql.autoBroadcastJoinThreshold", "20971520")
      .set("spark.sql.broadcastTimeout", "600000ms")
      .set("spark.hadoopRDD.ignoreEmptySplits", "true")
      .set("spark.sql.warehouse.dir", "hdfs://nameservice1/user/hive/warehouse")
      .set("spark.sql.catalogImplementation", "hive")
    val spark = SparkSession
      .builder()
      .config(conf)
      .appName(getClass.getSimpleName)
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    import spark.sql

    // 1.加载所有有效档案
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_mid")
    sql(s"USE ${bizCode}_mid")
    sql(
      s"""
         | CREATE TABLE IF NOT EXISTS $archiveMidTable(
         |  biz_code string,
         |  aid string,
         |  data_code string,
         |  status tinyint,
         |  create_time timestamp,
         |  modify_time timestamp,
         |  sys_code string,
         |  props string
         |  )
         | PARTITIONED BY (data_type string comment "按类型分区")
         | STORED AS PARQUET
         | LOCATION 'hdfs://nameservice1$archiveMidTableDir'
      """.stripMargin)
    val archiveDF = sql(
      s"""
         | SELECT
         |   aid,
         |   data_code,
         |   status
         | FROM $archiveMidTable
         | WHERE data_type = '$dataType'
       """.stripMargin
    ).filter("status = 0")
      .dropDuplicates("aid")
    println(s"加载${dataType}档案")
    archiveDF.show(false)
    if (debug) println(s"${dataType}档案总量：${archiveDF.count()}")

    // 2.加载分区事件
    spark.sql(s"USE ${bizCode}_dwd")
    val eventDF = spark.sql(
      s"""
         | SELECT
         |   biz_code,
         |   id,
         |   data_code,
         |   time,
         |   coll_dt,
         |   location,
         |   geo_hash,
         |   guid,
         |   create_time,
         |   modify_time,
         |   sys_code,
         |   props
         | FROM $eventDwdTable
         | WHERE dt = $dt AND data_type = '$dataType'
       """.stripMargin)
    println(s"加载${dataType}事件")
    eventDF.show(false)
    if (debug) println(s"${dataType}事件总量：${eventDF.count()}")

    // 3.1、根据 data_code 取交集，这部分是可归档数据，直接分配 aid
    import org.apache.spark.sql.functions.broadcast
    val joinDF = eventDF.join(broadcast(archiveDF), Seq("data_code"), "left")
    // 3.2、缓存
    joinDF.cache()
    // 3.3、已归档事件
    val archivedEventDF = joinDF
      .filter("aid is not null")
      .select("biz_code", "id", "aid", "time", "coll_dt", "location", "geo_hash", "guid", "create_time", "modify_time", "sys_code", "props")
    println(s"${dataType}可直接归档事件")
    archivedEventDF.show(false)
    if (debug) println(s"${dataType}可直接归档事件总量：${eventDF.count()}")

    // 4、未归档数据
    val unArchivedEventDF = joinDF.filter("aid is null")
    println(s"${dataType}需聚档事件")
    unArchivedEventDF.show(false)

    // 去重后的每个 dataCode 即为新档案
    def idGenerator(aid: String): String = {
      UUID.randomUUID().toString.replace("-", "")
    }
    import org.apache.spark.sql.functions._
    //生成udf函数
    val idGeneratorUDF = udf(idGenerator _)
    //加入隐式转换
    import spark.implicits._
    val newArchivedEventDF = unArchivedEventDF
      .dropDuplicates("data_code") // 去重
      .withColumn("aid", idGeneratorUDF($"aid"))
    newArchivedEventDF.createOrReplaceTempView("newArchivedEventDF")

    // 5.1、得到新增档案
    val updateArchieDF = sql(
      s"""
         | SELECT
         |   biz_code,
         |   aid,
         |   data_code,
         |   0 AS status,
         |   now() AS create_time,
         |   CAST(UNIX_TIMESTAMP('12/31/9999', 'MM/dd/yyyy') AS TIMESTAMP) AS modify_time,
         |   sys_code,
         |   props
         | FROM newArchivedEventDF
      """.stripMargin)
    println(s"新增${dataType}档案")
    updateArchieDF.show(false)
    if (debug) println(s"新增${dataType}档案数：${updateArchieDF.count()}")
    sql(s"USE ${bizCode}_mid")
    sql(
      s"""
         | CREATE TABLE IF NOT EXISTS $updateArchiveMidTable(
         |  biz_code string,
         |  aid string,
         |  data_code string,
         |  status tinyint,
         |  create_time timestamp,
         |  modify_time timestamp,
         |  sys_code string,
         |  props string
         |  )
         | PARTITIONED BY (data_type string comment "按类型分区")
         | STORED AS PARQUET
         | LOCATION 'hdfs://nameservice1$updateArchiveMidTableDir'
      """.stripMargin)
    // 5.2、将新增档案保存到单独一张表中，用于方便发送到 es
    updateArchieDF.write.mode(SaveMode.Overwrite).parquet(s"hdfs://nameservice1$updateArchiveMidDataDir")
    sql(s"ALTER TABLE $updateArchiveMidTable DROP IF EXISTS PARTITION(data_type ='$dataType')")
    sql(s"ALTER TABLE $updateArchiveMidTable ADD PARTITION(data_type ='$dataType') LOCATION '$updateArchiveMidDataDir'")
    // 5.3、增量插入档案数仓
    sql(
      s"""
         | INSERT INTO TABLE $archiveMidTable PARTITION(data_type='$dataType')
         | SELECT
         |   biz_code,
         |   aid,
         |   data_code,
         |   status,
         |   create_time,
         |   modify_time,
         |   sys_code,
         |   props
         | FROM $updateArchiveMidTable WHERE data_type = '$dataType'
       """.stripMargin)

    // 6、将所有未归档事件补齐 aid 并同已归档数据 union
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_dim")
    sql(s"USE ${bizCode}_dim")
    sql(
      s"""
         | CREATE EXTERNAL TABLE IF NOT EXISTS $eventDimTable (
         |   biz_code string,
         |   id string,
         |   aid string,
         |   time timestamp,
         |   coll_dt string,
         |   location string,
         |   geo_hash string,
         |   guid string,
         |   create_time timestamp,
         |   modify_time timestamp,
         |   sys_code string,
         |   props string
         |  )
         | PARTITIONED BY (dt string comment "按天分区", data_type string comment "按类型分区")
         | STORED AS PARQUET
         | LOCATION 'hdfs://nameservice1$eventDimTableDir'
           """.stripMargin)
    unArchivedEventDF
      .drop("aid")
      .join(broadcast(updateArchieDF.select("aid", "data_code")), Seq("data_code"), "left")
      .select("biz_code", "id", "aid", "time", "coll_dt", "location", "geo_hash", "guid", "create_time", "modify_time", "sys_code", "props")
      .union(archivedEventDF)
      .write.mode(SaveMode.Overwrite).parquet(s"hdfs://nameservice1$eventDimDataDir")

    joinDF.unpersist()

    sql(s"ALTER TABLE $eventDimTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='$dataType')")
    sql(s"ALTER TABLE $eventDimTable ADD PARTITION(dt= '$dt', data_type ='$dataType') location '$eventDimDataDir'")
    println(s"新增${dataType}归档事件")
    sql(s"SELECT * FROM $eventDimTable WHERE dt = '$dt' and data_type ='$dataType'").show()

    spark.stop()
  }

}
