package me.w1992wishes.phoenix.spark

import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2020/4/17 10:09
  */
object SparkReadOnPhoenix2 {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      //.master("local[16]")
      .getOrCreate()

    val ds = spark.read
      .format("org.apache.phoenix.spark")
      .option("table", "BIGDATA_ODL.ODL_BIGDATA_EVENT_FACE_5030") // phoenix 表
      .option("zkUrl", "intellif-bigdata-node1,intellif-bigdata-node2,intellif-bigdata-node3:2181:/hbase") // zk URL
      .load
      .select("THUMBNAIL_ID") // 选择字段

    ds.show(10)

    spark.close()
  }

}
