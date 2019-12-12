package me.w1992wishes.spark.sql.hive

import java.time.LocalDateTime

import ch.hsr.geohash.GeoHash
import me.w1992wishes.common.util.DateUtils
import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, udf}

import scala.util.Random

/**
  * @author w1992wishes 2019/12/12 15:47
  */
class SparkSqlMapJoinToHive {
  def main(args: Array[String]): Unit = {

    println("开始")

    val partitions = args(0).toInt

    val spark = SparkSession
      .builder()
      .enableHiveSupport()
      //.master("local[20]")
      .appName("ArchiveToHive")
      .getOrCreate()

    val events = getDataFromDB(spark, partitions)
    val cameras = getCameraFromHive(spark)

    val events_geohash = events.join(org.apache.spark.sql.functions.broadcast(cameras), events("source_id") === cameras("id"), "left")

    save(events_geohash, spark)

    spark.close()

  }

  def getDataFromDB(spark: SparkSession, partitions: Int): DataFrame = {
    val hiveDt = udf((str: String) => DateUtils.dateTimeToYMDStr(LocalDateTime.now()))

    /**
      * 获取 spark 并行查询的条件数组
      *
      * @param realPartitions 实际分区数
      * @return
      */
    def getParalleledCondition(realPartitions: Int): Array[String] = {

      // 转换为 spark sql where 字句中的查询条件
      Range(0, realPartitions).map(partition => s"CAST(thumbnail_id as numeric) % $realPartitions = $partition"
      ).toArray
    }

    val condition = getParalleledCondition(partitions)
    // 并行处理
    Range(0, partitions)
      .map(index => {
        spark
          .read
          .format("jdbc")
          .option("driver", "")
          .option("url", "")
          .option("dbtable", s"(SELECT * FROM t_source_table WHERE ${condition(index)}) AS t_tmp_$index")
          .option("user", "")
          .option("password", "")
          .option("fetchsize", "")
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
      .withColumn("dt", hiveDt(col("thumbnail_id")))
      .drop(col("save_time"))
  }

  private def getCameraFromHive(spark: SparkSession): DataFrame = {

    def getGeoHash(geo: String): String = {
      if (StringUtils.isNotEmpty(geo)) {
        val temp = geo.replace("POINT(", "").replace(")", "").split(" ")
        val lon = temp(0).toDouble
        val lat = temp(1).toDouble
        GeoHash.withCharacterPrecision(lat, lon, 12).toBase32
      }
      ""
    }

    spark.udf.register("getGeoHash", getGeoHash _)
    spark.sql("select id, getGeoHash(geo) as geo_hash from bigdata_dim.camera_info")
  }

  private def save(events_geohash: DataFrame, spark: SparkSession): Unit = {

    val sinkTable = "t_sink_table"

    spark.sql("show databases").show(10)

    spark.sql("use bigdata_dim")

    // 启用最终数据输出压缩
    // hive.exec.compress.output 是hive-site.xml中的配置参数
    // mapred.output.compress 是hdfs-site.xml的配置参数
    spark.sql("set spark.hadoop.hive.exec.compress.output=true")
    spark.sql("set mapred.output.compress=true")

    // 设置压缩Reduce类型输出
    spark.sql("set mapred.output.compression.codec=com.hadoop.compression.lzo.LzoCodec")
    spark.sql("set io.seqfile.compression.type=BLOCK")
    spark.sql("set io.compression.codecs=com.hadoop.compression.lzo.LzoCodec")

    spark.sql(s"create table IF NOT EXISTS $sinkTable" +
      "(adi string, sys_code string, thumbnail_id string, thumbnail_url string, image_id string, image_url string, " +
      "feature_info binary, algo_version int, gender_info string, age_info string, hairstyle_info string, " +
      "hat_info string, glasses_info string, race_info string, mask_info string, skin_info string, " +
      "pose_info string, quality_info float, target_rect string, target_rect_float string, land_mark_info string," +
      "target_thumbnail_rect string, source_id string, source_type string, site string, geo_hash string, feature_quality float, " +
      "time timestamp, create_time timestamp, column1 string, column2 string," +
      "column3 string, field1 string) PARTITIONED BY (dt string) " +
      "STORED AS SEQUENCEFILE")

    spark.sql("set spark.hadoop.hive.exec.dynamic.partition.mode = nonstrict")
    spark.sql("set spark.hadoop.hive.exec.dynamic.partition = true")

    val lats = Array[Double](22.5, 22.8, 23.0, 23.7, 23.5, 24.0, 24.22, 24.5, 25.33, 25.0, 25.78, 25.53, 26.0, 26.5, 27.0)
    val lons = Array[Double](115.5, 115.76, 116.0, 116.23, 116.5, 116.78, 117.0, 117.88, 117.5, 118.0, 118.23, 118.55, 118.89, 118.90, 119.03)
    def nulltoDefault(value: String) = {
      if (StringUtils.isEmpty(value)) {
        GeoHash.withCharacterPrecision(lats(Random.nextInt(15)), lons(Random.nextInt(15)), 12).toBase32
      } else {
        value
      }
    }

    events_geohash.createOrReplaceTempView("event_geohash_table")

    spark.udf.register("nulltoDefault", nulltoDefault _)
    spark.sql(s"INSERT INTO TABLE $sinkTable PARTITION(dt='${DateUtils.dateTimeToYMDStr(LocalDateTime.now())}') " +
      s"SELECT aid, sys_code, thumbnail_id, thumbnail_url, image_id, image_url, " +
      s"feature_info, algo_version, gender_info, age_info, hairstyle_info, " +
      s"hat_info, glasses_info, race_info, mask_info, skin_info, " +
      s"pose_info, quality_info, target_rect, target_rect_float, land_mark_info," +
      s"target_thumbnail_rect, source_id, source_type, site, nulltoDefault(geo_hash) as geo_hash, feature_quality, " +
      "time, create_time, column1, column2, column3, field1 from event_geohash_table")
  }
}
