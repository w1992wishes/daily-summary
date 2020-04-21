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

    val df = spark.read
      .format("org.apache.phoenix.spark")
      .option("table", "BIGDATA_ODL.ODL_BIGDATA_EVENT_FACE_PENDING") // phoenix 表
      .option("zkUrl", "intellif-bigdata-node1,intellif-bigdata-node2,intellif-bigdata-node3:2181:/hbase") // zk URL
      .load

    // 通过 phoenix 写入 hbase
    df.filter(df("dt") >= 139000l && df("dt") < 1888888888l) // 根据时间 dt 过滤
      .write
      .format("org.apache.phoenix.spark")
      .mode(SaveMode.Overwrite) // phoenix 只支持 overwrite
      .options(Map(
      "phoenix.schema.isNamespaceMappingEnabled" -> "true",
      "driver" -> "org.apache.phoenix.jdbc.PhoenixDriver",
      "zkUrl" -> "jdbc:phoenix:intellif-bigdata-node1,intellif-bigdata-node2,intellif-bigdata-node3:2181", // 指定 zk 地址
      "table" -> "BIGDATA_ODL.ODL_BIGDATA_EVENT_FACE_5030" // 指定插入的 phoenix 表
    ))
      .save()

    spark.close()
  }
}
