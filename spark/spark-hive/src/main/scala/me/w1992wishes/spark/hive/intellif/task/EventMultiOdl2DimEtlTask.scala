package me.w1992wishes.spark.hive.intellif.task

import com.alibaba.fastjson.JSON
import me.w1992wishes.spark.hive.intellif.param.EventMultiOdl2DimEtlCLParam
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * 多维事件抽取， imsi/imei/mac/car 公用该 task
  *
  * @author w1992wishes 2020/4/28 14:16
  */
object EventMultiOdl2DimEtlTask {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    // 命令行参数
    val clParam = EventMultiOdl2DimEtlCLParam(args)
    val bizCode = clParam.bizCode
    val dt = clParam.dt
    val geoLength = Math.min(12, clParam.geoLength)
    val sourceTableDir = "/user/flume/event/multi"
    val sourceDataDir = s"$sourceTableDir/$dt"
    val sinkTableDir = "/user/hive/warehouse/event/multi"
    val sinkDataDir = s"$sinkTableDir/$dt"
    val sourceTable = s"${bizCode}_odl.odl_${bizCode}_event_multi"
    val sinkTable = s"${bizCode}_dim.dim_${bizCode}_event_multi"

    // spark 参数设置
    val conf = new SparkConf()
      .setIfMissing("spark.master", "local[8]")
      .set("spark.sql.adaptive.enabled", "true")
      .set("spark.sql.adaptive.shuffle.targetPostShuffleInputSize", "67108864b")
      .set("spark.sql.adaptive.join.enabled", "true")
      .set("spark.sql.autoBroadcastJoinThreshold", "20971520")
      .set("spark.sql.broadcastTimeout", "600000ms")
      .set("spark.hadoopRDD.ignoreEmptySplits", "true")
      .set("spark.sql.warehouse.dir", "hdfs://nameservice1/sql/metadata/hive")
      .set("spark.sql.catalogImplementation", "hive")
    val spark = SparkSession
      .builder()
      .config(conf)
      .appName(getClass.getSimpleName)
      .getOrCreate()

    import spark.implicits._
    import spark.sql

    // 加载 odl 层原始 car 数据
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_odl")
    sql(s"USE ${bizCode}_odl")
    sql(s"CREATE EXTERNAL TABLE IF NOT EXISTS $sourceTable (contents string) partitioned by (dt string) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\001' STORED AS TEXTFILE LOCATION 'hdfs:$sourceTableDir'")
    sql(s"ALTER TABLE $sourceTable DROP IF EXISTS PARTITION(dt= '$dt')")
    sql(s"ALTER TABLE $sourceTable ADD PARTITION(dt= '$dt') location '$sourceDataDir'")

    val contentsDF = sql(s"SELECT contents FROM $sourceTable WHERE dt='$dt'")
    contentsDF.show()

    // 定义 scheme
    import org.apache.spark.sql.types._
    val schema = new StructType()
      .add("dataType", StringType)
      .add("id", StringType)
      .add("aid", StringType)
      .add("bizCode", StringType)
      .add("time", TimestampType)
      .add("props", StringType)
      .add("location", StringType)
      .add("geoHash", StringType)
      .add("createTime", TimestampType)

    // 转为 EventMultiInfo
    val eventMultiDF = spark.read.schema(schema).json(contentsDF.as[String])
    eventMultiDF.printSchema()
    eventMultiDF.show()

    import org.apache.spark.sql.functions.udf
    val guidCode = (props: String) => {
      val json = JSON.parseObject(props)
      json.getString("faceRelationId")
    }
    val addGuidCol = udf(guidCode)
    val geoCode = (location: String) => {
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
    val addGeoCol = udf(geoCode)

    eventMultiDF
      .na.drop(Array("time", "aid", "location"))
      .withColumnRenamed("bizCode", "biz_code")
      .withColumnRenamed("createTime", "create_time")
      .withColumnRenamed("dataType", "data_type")
      .withColumn("geo_hash", addGeoCol(eventMultiDF.col("location")))
      .withColumn("guid", addGuidCol(eventMultiDF.col("props")))
      .drop("geoHash")
      .repartition($"data_type")
      .write
      .mode(SaveMode.Overwrite)
      .partitionBy("data_type")
      .format("parquet")
      .save(s"hdfs://nameservice1$sinkDataDir")

    // load
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_dim")
    sql(s"USE ${bizCode}_dim")
    sql(
      s"""
         | CREATE EXTERNAL TABLE IF NOT EXISTS $sinkTable (
         |   id string,
         |   aid string,
         |   biz_code string,
         |   time timestamp,
         |   props string,
         |   location string,
         |   create_time timestamp,
         |   geo_hash string,
         |   guid string
         |  )
         | PARTITIONED BY (dt string comment "按天分区", data_type string comment "按类型分区")
         | STORED AS PARQUET
         | LOCATION 'hdfs://nameservice1$sinkTableDir'
           """.stripMargin)

    sql(s"ALTER TABLE $sinkTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='car')")
    sql(s"ALTER TABLE $sinkTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='mac')")
    sql(s"ALTER TABLE $sinkTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='imsi')")
    sql(s"ALTER TABLE $sinkTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='imei')")
    sql(s"ALTER TABLE $sinkTable ADD PARTITION(dt= '$dt', data_type ='car') location '$sinkDataDir/data_type=car'")
    sql(s"ALTER TABLE $sinkTable ADD PARTITION(dt= '$dt', data_type ='mac') location '$sinkDataDir/data_type=mac'")
    sql(s"ALTER TABLE $sinkTable ADD PARTITION(dt= '$dt', data_type ='imsi') location '$sinkDataDir/data_type=imsi'")
    sql(s"ALTER TABLE $sinkTable ADD PARTITION(dt= '$dt', data_type ='imei') location '$sinkDataDir/data_type=imei'")

    sql(s"SELECT * FROM $sinkTable WHERE dt = '$dt' and data_type ='car'").show()
    sql(s"SELECT * FROM $sinkTable WHERE dt = '$dt' and data_type ='mac'").show()
    sql(s"SELECT * FROM $sinkTable WHERE dt = '$dt' and data_type ='imsi'").show()
    sql(s"SELECT * FROM $sinkTable WHERE dt = '$dt' and data_type ='imei'").show()

    spark.stop()

  }
}
