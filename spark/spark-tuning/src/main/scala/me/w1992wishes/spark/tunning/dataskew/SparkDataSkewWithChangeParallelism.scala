package me.w1992wishes.spark.tunning.dataskew

import java.util.concurrent.atomic.AtomicInteger

import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/12/2 10:19
  */
object SparkDataSkewWithChangeParallelism {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      //.master("local[20]")
      .appName("SparkDataSkewWithChangeParallelism")
      .config("hive.metastore.uris", "thrift://ds072:9083")
      //.config("spark.sql.shuffle.partitions", 12)
      .enableHiveSupport()
      .getOrCreate()

    spark.sql("use wqf")

    //import spark.implicits._
    val df = spark.sql("select * from test")
   df.rdd
      .map(row => (row.getInt(0), row.getString(1)))
      .groupByKey(48)
      .map(tuple => {
        val id = tuple._1
        val counter = new AtomicInteger(0)
        tuple._2.foreach(name => counter.incrementAndGet())
        (id, counter.get())
      })
      .count()

    spark.stop()
    spark.close()
  }

}
