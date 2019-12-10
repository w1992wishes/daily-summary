package me.w1992wishes.spark.sql.phoenix

import java.sql.Timestamp
import java.util.UUID

import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * @author w1992wishes 2019/12/10 15:27
  */
object SparkSqlHiveToPhoenix {

  /**
    * 为保证代码正常运行，需在 spark-default.conf 配置下添加：
    *
    * spark.driver.extraClassPath /home/hadoop/phoenix/phoenix-spark-4.14.3-HBase-1.4.jar:/home/hadoop/phoenix/phoenix-4.14.3-HBase-1.4-client.jar
    * spark.executor.extraClassPath /home/hadoop/phoenix/phoenix-spark-4.14.3-HBase-1.4.jar:/home/hadoop/phoenix/phoenix-4.14.3-HBase-1.4-client.jar
    *
    * 如果分布式运行报 phoenix namespace mapping 错误，将 hbase 的 hbase-site.xml 复制到spark 集群中
    */
  def main(args: Array[String]): Unit = {

    println("开始")

    val spark = SparkSession
      .builder()
      .enableHiveSupport()
      .config("spark.debug.maxToStringFields", 100)
      //.master("local[20]")
      .appName("ToPhoenixApplication")
      .getOrCreate()

    spark.sql("show databases").show(10)
    spark.sql("use bigdata_dim")

    val archives = spark.sql("select * from bigdata_dim.dim_bigdata_event_face_person_5030")

    import org.apache.spark.sql.functions._
    def makeGTRowKey(time: Timestamp, geoHash: String): String = {
      val rowKey = geoHash.concat(time.getTime / 1000 + "").concat(UUID.randomUUID().toString.replace("-", ""))
      rowKey
    }

    val udf_gt_rowkey = udf(makeGTRowKey _)

    archives
      .withColumn("id", udf_gt_rowkey(archives.col("time"), archives.col("geo_hash")))
      .write
      .format("org.apache.phoenix.spark")
      .mode(SaveMode.Overwrite)
      .options(Map(
        "phoenix.schema.isNamespaceMappingEnabled" -> "true",
        "driver" -> "org.apache.phoenix.jdbc.PhoenixDriver",
        "zkUrl" -> "jdbc:phoenix:ds072,ds073,ds074:2181",
        "table" -> "DIM_BIGDATA_EVENT_FACE_PERSON_5030"
      ))
      .save()
  }

}
