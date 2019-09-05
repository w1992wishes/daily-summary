package me.w1992wishes.spark.sql

import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * @author w1992wishes 2019/4/17 9:52
  */
object CustomSqlLoader {

  def main(args: Array[String]): Unit = {

    // spark dataframe to pojo
    // val FaceEventEncoder = Encoders.bean(classOf[FaceEvent])
    // 自定义新增 feature_quality 列
    val addFeatureQualityColFunc: (Array[Byte] => Float) = (faceFeature: Array[Byte]) => if (faceFeature.isEmpty) -1.0f else 1.0f
    val addFeatureQualityCol = udf(addFeatureQualityColFunc)

    // 创建 spark
    var spark: SparkSession = SparkSession
      .builder()
      .master("local")
      .appName("CustomSqlLoader")
      .getOrCreate()

    val partitions = args(0).toInt
    // 并行处理
    Range(0, partitions)
      .map(index => {
        spark
          .read
          .format("jdbc")
          .option("driver", "com.pivotal.jdbc.GreenplumDriver")
          .option("url", "jdbc:pivotal:greenplum://127.0.0.1:5432;DatabaseName=bigdata_odl")
          .option("dbtable", s"(SELECT * FROM t_source_table WHERE ${getParalleledCondition(partitions)(index)}) AS t_tmp_$index")
          .option("user", "gpadimn")
          .option("password", "gpadimn")
          .option("fetchsize", 5000)
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
      .withColumn("feature_quality",
        addFeatureQualityCol(col("feature_info"), col("pose_info"), col("quality_info")))
      //.repartition(partitions, col("thumbnail_id"))
      .write
      .mode(SaveMode.Append)
      .format("jdbc")
      .option("driver", "com.pivotal.jdbc.GreenplumDriver")
      .option("url", "jdbc:pivotal:greenplum://127.0.0.1:5432;DatabaseName=bigdata_odl")
      .option("dbtable", "t_sink_table")
      .option("user", "gpadimn")
      .option("password", "gpadimn")
      .option("batchsize", 5000)
      .save()

    spark.stop()
  }

  /**
    * 获取 spark 并行查询的条件数组
    *
    * @param realPartitions 实际分区数
    * @return
    */
  protected def getParalleledCondition(realPartitions: Int): Array[String] = {

    // 转换为 spark sql where 字句中的查询条件
    Range(0, realPartitions).map (partition => s"CAST(thumbnail_id as numeric) % $realPartitions = $partition").toArray
  }
}
