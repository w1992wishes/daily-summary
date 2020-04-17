package me.w1992wishes.phoenix.spark

import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * resource 目录下需引入 hbase-site.xml 和 hdfs-site.xml
  *
  * @author w1992wishes 2020/4/17 10:09
  */
object SparkWriteOnPhoenix1 {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      //.master("local[16]")
      .getOrCreate()

    // 从 gp 构建 DF
    val data = spark.read
      .format("jdbc")
      .option("url", "jdbc:postgresql://192.168.11.31:5432/bigdata_odl")
      .option("driver", "org.postgresql.Driver")
      .option("dbtable", "public.odl_bigdata_event_face_pending_1_prt_p20200417")
      .option("user", "gpadmin")
      .option("password", "gpadmin")
      .load()

    // 通过 phoenix 写入 hbase
    data.write
      .format("org.apache.phoenix.spark")
      .mode(SaveMode.Overwrite)
      .options(Map(
        "phoenix.schema.isNamespaceMappingEnabled" -> "true",
        "driver" -> "org.apache.phoenix.jdbc.PhoenixDriver",
        "zkUrl" -> "jdbc:phoenix:intellif-bigdata-node1,intellif-bigdata-node2,intellif-bigdata-node3:2181",  // 指定 zk 地址
        "table" -> "BIGDATA_ODL.ODL_BIGDATA_EVENT_FACE_5030" // 指定插入的 phoenix 表
      ))
      .save()

    spark.close()
  }
}
