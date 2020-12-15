package me.w1992wishes.spark.sql.hive

import java.time.LocalDateTime

import me.w1992wishes.common.util.DateUtil
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

/**
  * @author w1992wishes 2019/12/11 17:15
  */
object SparkSqlToHive {

  def main(args: Array[String]): Unit = {

    val partitions = args(0).toInt

    val spark = SparkSession
      .builder()
      .enableHiveSupport()
      //.master("local")
      .appName("GpToHive")
      .getOrCreate()

    val df = getDataFromDB(spark, partitions)

    println("创建临时表")
    df.createOrReplaceTempView("face_table")

    //saveOne(df, config.sinkTable)
    save(spark)
    spark.close()

  }

  private def saveOne(df: DataFrame, sinkTable: String): Unit = {
    df.write.mode(SaveMode.Overwrite).partitionBy("dt").saveAsTable(s"bigdata_dwd.$sinkTable")
  }

  private def save(spark: SparkSession): Unit = {

    spark.sql("use bigdata_dwd")
    spark.sql(s"create table IF NOT EXISTS t_sink_tables" +
      "(sys_code string, thumbnail_id string, thumbnail_url string, image_id string, image_url string, " +
      "feature_info binary, algo_version int, gender_info string, age_info string, hairstyle_info string, " +
      "hat_info string, glasses_info string, race_info string, mask_info string, skin_info string, " +
      "pose_info string, quality_info float, target_rect string, target_rect_float string, land_mark_info string," +
      "target_thumbnail_rect string, source_id string, source_type string, site string, feature_quality float, " +
      "time timestamp, create_time timestamp, column1 string, column2 string," +
      "column3 string, field1 string) PARTITIONED BY (dt string) ROW FORMAT DELIMITED FIELDS TERMINATED BY ','")

    spark.sql("set  hive.exec.dynamic.partition.mode = nonstrict")
    spark.sql("set  hive.exec.dynamic.partition = true")

    print("创建数据库完成")

    spark.sql(s"INSERT INTO TABLE t_sink_tables PARTITION(dt='${DateUtil.dateTimeToYMDStr(LocalDateTime.now())}') " +
      s"SELECT sys_code, thumbnail_id, thumbnail_url, image_id, image_url, " +
      s"feature_info, algo_version, gender_info, age_info, hairstyle_info, " +
      s"hat_info, glasses_info, race_info, mask_info, skin_info, " +
      s"pose_info, quality_info, target_rect, target_rect_float, land_mark_info," +
      "target_thumbnail_rect, source_id, source_type, site, feature_quality, " +
      "time, create_time, column1, column2, column3, field1 from face_table")
  }


  def getDataFromDB(spark: SparkSession, partitions: Int): DataFrame = {

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

    val hiveDt = udf((str: String) => DateUtil.dateTimeToYMDStr(LocalDateTime.now()))

    val condition = getParalleledCondition(partitions)
    // 并行处理
    Range(0, partitions)
      .map(index => {
        spark
          .read
          .format("jdbc")
          .option("driver", "")
          .option("url", "")
          .option("dbtable", s"(SELECT * FROM t_source_tables WHERE ${condition(index)}) AS t_tmp_$index")
          .option("user","")
          .option("password", "")
          .option("fetchsize", "")
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
      .withColumn("dt", hiveDt(col("thumbnail_id")))
      .drop(col("save_time"))
  }
}
