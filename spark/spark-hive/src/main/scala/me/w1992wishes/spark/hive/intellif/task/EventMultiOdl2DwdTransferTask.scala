package me.w1992wishes.spark.hive.intellif.task

import java.sql.Timestamp

import ch.hsr.geohash.GeoHash
import com.alibaba.fastjson.JSON
import me.w1992wishes.common.util.DateUtil
import me.w1992wishes.spark.hive.intellif.param.EventMultiOdl2DwdTransferCLParam
import org.apache.commons.lang3.StringUtils
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * 多维事件抽取， imsi/imei/mac/car 公用该 task
  *
  * @author w1992wishes 2020/4/28 14:16
  */
object EventMultiOdl2DwdTransferTask {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    // 命令行参数
    val clParam = EventMultiOdl2DwdTransferCLParam(args)
    val bizCode = clParam.bizCode
    val dt = clParam.dt
    val geoLength = Math.min(12, clParam.geoLength)
    val sourceTableDir = s"/user/flume/event_multi_$bizCode"
    val sourceDataDir = s"$sourceTableDir/$dt"
    val sinkTableDir = s"/user/hive/warehouse/dwd_${bizCode}_event_multi/event"
    val sinkDataDir = s"$sinkTableDir/$dt"
    val sourceTable = s"${bizCode}_odl.odl_${bizCode}_event_multi"
    val sinkTable = s"${bizCode}_dwd.dwd_${bizCode}_event_multi"

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

    import spark.implicits._
    import spark.sql

    // 加载 odl 层原始 car 数据
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_odl")
    sql(s"USE ${bizCode}_odl")
    sql(s"CREATE EXTERNAL TABLE IF NOT EXISTS $sourceTable (contents string) partitioned by (dt string) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\001' STORED AS TEXTFILE LOCATION 'hdfs:$sourceTableDir'")
    sql(s"ALTER TABLE $sourceTable DROP IF EXISTS PARTITION(dt= '$dt')")
    sql(s"ALTER TABLE $sourceTable ADD PARTITION(dt= '$dt') location '$sourceDataDir'")

    val contentsDF = sql(s"SELECT contents FROM $sourceTable WHERE dt='$dt'")
    contentsDF.show(false)

    // 定义 scheme
    import org.apache.spark.sql.types._
    val schema = new StructType()
      .add("bizCode", StringType)
      .add("id", StringType)
      .add("dataType", StringType)
      .add("dataCode", StringType)
      .add("time", TimestampType)
      .add("location", StringType)
      .add("guid", StringType)
      .add("createTime", TimestampType)
      .add("modifyTime", TimestampType)
      .add("sysCode", StringType)
      .add("props", StringType)

    // 从 odl 加载新增事件
    val eventMultiDF = spark.read.schema(schema).json(contentsDF.as[String]).na.drop(Array("time", "dataCode", "dataType"))
    eventMultiDF.printSchema()
    eventMultiDF.show(false)

    eventMultiDF.map(row => {
      val biz_code = bizCode
      val id = row.getAs[String]("id")
      val data_code = row.getAs[String]("dataCode")
      val time = row.getAs[Timestamp]("time")
      val coll_dt = DateUtil.dateTimeToStr(time.toLocalDateTime, DateUtil.DF_YMD)
      val location = row.getAs[String]("location")
      val geo_hash = calGeoHash(location, geoLength)
      val guid = row.getAs[String]("guid")
      val create_time = row.getAs[Timestamp]("createTime")
      val modify_time = row.getAs[Timestamp]("modifyTime")
      val sys_code = if (StringUtils.isEmpty(row.getAs[String]("sysCode"))) "Dynamic" else row.getAs[String]("sysCode")
      val props = row.getAs[String]("props")
      val data_type = row.getAs[String]("dataType").toUpperCase
      DwdEventMulti(biz_code, id, data_code, time, coll_dt, location, geo_hash, guid, create_time, modify_time, sys_code, props, data_type)
    })
      .repartition($"data_type")
      .write
      .mode(SaveMode.Overwrite)
      .partitionBy("data_type")
      .format("parquet")
      .save(s"hdfs://nameservice1$sinkDataDir")

    // load
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_dwd")
    sql(s"USE ${bizCode}_dwd")
    sql(
      s"""
         | CREATE EXTERNAL TABLE IF NOT EXISTS $sinkTable (
         |   biz_code string,
         |   id string,
         |   data_code string,
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
         | LOCATION 'hdfs://nameservice1$sinkTableDir'
           """.stripMargin)

    sql(s"ALTER TABLE $sinkTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='CAR')")
    sql(s"ALTER TABLE $sinkTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='MAC')")
    sql(s"ALTER TABLE $sinkTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='IMSI')")
    sql(s"ALTER TABLE $sinkTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='IMEI')")
    sql(s"ALTER TABLE $sinkTable ADD PARTITION(dt= '$dt', data_type ='CAR') location '$sinkDataDir/data_type=CAR'")
    sql(s"ALTER TABLE $sinkTable ADD PARTITION(dt= '$dt', data_type ='MAC') location '$sinkDataDir/data_type=MAC'")
    sql(s"ALTER TABLE $sinkTable ADD PARTITION(dt= '$dt', data_type ='IMSI') location '$sinkDataDir/data_type=IMSI'")
    sql(s"ALTER TABLE $sinkTable ADD PARTITION(dt= '$dt', data_type ='IMEI') location '$sinkDataDir/data_type=IMEI'")

    sql(s"SELECT * FROM $sinkTable WHERE dt = '$dt' and data_type ='CAR'").show()
    sql(s"SELECT * FROM $sinkTable WHERE dt = '$dt' and data_type ='MAC'").show()
    sql(s"SELECT * FROM $sinkTable WHERE dt = '$dt' and data_type ='IMSI'").show()
    sql(s"SELECT * FROM $sinkTable WHERE dt = '$dt' and data_type ='IMEI'").show()

    spark.stop()

  }

  case class DwdEventMulti(biz_code: String, id: String, data_code: String, time: Timestamp, coll_dt: String, location: String, geo_hash: String, guid: String, create_time: Timestamp, modify_time: Timestamp, sys_code: String, props: String, data_type: String)

  def calGeoHash(location: String, geoLength: Int): String = {
    try {
      val json = JSON.parseObject(location)
      val lat = json.getDouble("latitude")
      val lon = json.getDouble("longitude")
      if (lat == null || lon == null) {
        ""
      } else {
        GeoHash.withCharacterPrecision(lat, lon, geoLength).toBase32
      }
    } catch {
      case _: Exception => ""
    }
  }

}
