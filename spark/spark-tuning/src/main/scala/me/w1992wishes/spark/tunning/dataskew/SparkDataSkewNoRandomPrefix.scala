package me.w1992wishes.spark.tunning.dataskew

import java.util.concurrent.atomic.AtomicInteger

import org.apache.spark.sql.{Row, SparkSession}

/**
  * @author w1992wishes 2019/12/2 16:19
  */
object SparkDataSkewNoRandomPrefix {

  def main(args: Array[String]): Unit = {

    var parallelism = 48

    if (args.length > 0) {
      parallelism = args(0).toInt
    }

    val spark = SparkSession.builder()
      //.master("local[20]")
      .appName("SparkDataSkewNoRandomPrefix")
      .config("spark.sql.shuffle.partitions", parallelism)
      .config("spark.sql.crossJoin.enabled", value = true)
      .config("spark.default.parallelism", parallelism)
      //.enableHiveSupport()
      .getOrCreate()

    import spark.implicits._

    val leftRDD = spark.read.text("hdfs://myha01/user/hive/warehouse/wqf.db/test/")
      .map(row => {
        def foo(row: String): (String, String) = {
          val str = row.split(",")
          (str(0), str(1))
        }

        foo(row.getString(0))
      })
      .toDF("id", "name")

    val rightRDD = spark.read.text("hdfs://myha01/user/hive/warehouse/wqf.db/test_new/")
      .map(row => {
        def foo(row: String) = {
          val str = row.split(",")
          (str(0), str(1))
        }

        foo(row.getString(0))
      })
      .toDF("id", "name1")

    leftRDD
      .join(rightRDD, "id")
      .foreachPartition((iterator: Iterator[Row]) => {
        val atomicInteger = new AtomicInteger()
        iterator.foreach(_ => atomicInteger.incrementAndGet())
        println(atomicInteger.get())
      })

    spark.stop()
    spark.close()
  }
}
