package me.w1992wishes.phoenix.spark

import org.apache.hadoop.conf.Configuration
import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2020/4/17 10:09
  */
object SparkReadOnPhoenix1 {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      //.master("local[16]")
      .getOrCreate()

    // 官方提供的 phoenixTableAsDataFrame 的方法
    val conf = new Configuration
    conf.addResource("hbase-site.xml")
    conf.addResource("hdfs-site.xml")
    import org.apache.phoenix.spark._
    val ds = spark.sqlContext.phoenixTableAsDataFrame(
      "BIGDATA_ODL.ODL_BIGDATA_EVENT_FACE_5030", // phoenix 表
      Seq("THUMBNAIL_ID", "THUMBNAIL_URL", "FEATURE_INFO"), // 字段
      Option("SYS_CODE = 'DeepEye.t_face_29'"), // where 条件
      conf = conf,
      zkUrl = Some("intellif-bigdata-node1,intellif-bigdata-node2,intellif-bigdata-node3:2181:/hbase"))

    ds.show(10)

    spark.close()
  }

}
