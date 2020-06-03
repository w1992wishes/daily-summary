package me.w1992wishes.spark.hive.intellif.task

import me.w1992wishes.spark.hive.intellif.param.EventMultiEtlCLParam
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * 多维事件抽取， imsi/imei/mac 公用该 task，car 因为格式不一致，单独用 EventCarEtlTask
  *
  * @author w1992wishes 2020/4/28 14:16
  */
object EventMultiEtlTask {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    // 命令行参数
    val clParam = EventMultiEtlCLParam(args)
    val bizCode = clParam.bizCode
    val dt = clParam.dt
    val sourceTableDir = "/user/flume/event/multi"
    val sourceDataDir = s"$sourceTableDir/$dt"
    val sinkTableDir = "/user/hive/warehouse/event/multi"
    val sinkDataDir = s"$sinkTableDir/$dt"
    val sourceTable = s"${bizCode}_odl.odl_${bizCode}_event_multi"
    val sinkTable = s"${bizCode}_dim.dim_${bizCode}_event_multi"

    // spark 参数设置
    val conf = new SparkConf()
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
      .master("local[16]")
      .appName(getClass.getSimpleName)
      .getOrCreate()

    import spark.sql
    import spark.implicits._

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

    eventMultiDF
      .withColumnRenamed("bizCode", "biz_code")
      .withColumnRenamed("geoHash", "geo_hash")
      .withColumnRenamed("createTime", "create_time")
      .repartition($"dataType")
      .write
      .mode(SaveMode.Overwrite)
      .partitionBy("dataType")
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
         |   geo_hash string,
         |   create_time timestamp
         |  )
         | PARTITIONED BY (dt string comment "按天分区", data_type string comment "按类型分区")
         | STORED AS PARQUET
         | LOCATION 'hdfs:$sinkTableDir'
           """.stripMargin)

    sql(s"ALTER TABLE $sinkTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='car')")
    sql(s"ALTER TABLE $sinkTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='mac')")
    sql(s"ALTER TABLE $sinkTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='imsi')")
    sql(s"ALTER TABLE $sinkTable DROP IF EXISTS PARTITION(dt= '$dt', data_type ='imei')")
    sql(s"ALTER TABLE $sinkTable ADD PARTITION(dt= '$dt', data_type ='car') location '$sinkDataDir/dataType=car'")
    sql(s"ALTER TABLE $sinkTable ADD PARTITION(dt= '$dt', data_type ='mac') location '$sinkDataDir/dataType=mac'")
    sql(s"ALTER TABLE $sinkTable ADD PARTITION(dt= '$dt', data_type ='imsi') location '$sinkDataDir/dataType=imsi'")
    sql(s"ALTER TABLE $sinkTable ADD PARTITION(dt= '$dt', data_type ='imei') location '$sinkDataDir/dataType=imei'")

    sql(s"SELECT * FROM $sinkTable WHERE dt = '$dt' and data_type ='car'").show()
    sql(s"SELECT * FROM $sinkTable WHERE dt = '$dt' and data_type ='mac'").show()
    sql(s"SELECT * FROM $sinkTable WHERE dt = '$dt' and data_type ='imsi'").show()
    sql(s"SELECT * FROM $sinkTable WHERE dt = '$dt' and data_type ='imei'").show()

    spark.stop()

  }
}