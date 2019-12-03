package me.w1992wishes.spark.tunning.dataskew

import java.util.concurrent.atomic.AtomicInteger

import org.apache.spark.sql.{Row, SparkSession}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/**
  * @author w1992wishes 2019/12/2 16:19
  */
object SparkDataSkewWithRandomPrefix1 {

  def main(args: Array[String]): Unit = {

    var parallelism = 48

    if (args.length > 0) {
      parallelism = args(0).toInt
    }

    val spark = SparkSession.builder()
      //.master("local[20]")
      .appName("SparkDataSkewWithRandomPrefix1")
      .config("spark.sql.shuffle.partitions", parallelism)
      .config("spark.sql.crossJoin.enabled", value = true)
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
      .toDF("id", "name1")

    val rightRDD = spark.read.text("hdfs://myha01/user/hive/warehouse/wqf.db/test_new/")
      .map(row => {
        def foo(row: String) = {
          val str = row.split(",")
          (str(0), str(1))
        }

        foo(row.getString(0))
      })
      .toDF("id", "name2")

    val addList = new ArrayBuffer[String]

    // 1 - 48 的随机数
    Range(1, 49).map(
      i => addList += i.toString
    )

    // 广播随机前缀
    val addListKeys = spark.sparkContext.broadcast(addList)

    // 大表增加随机前缀
    val leftRandomRDD = leftRDD
      .map(row => (s"${(new Random().nextInt(48) + 1).toString},${row.getString(0)}", row.getString(1)))

    // 小表扩大 N 倍， 每个增加1 to 48 的前缀
    val rightNewRDD = rightRDD
      .map(row => addListKeys.value.map(i => (s"$i,${row.getString(0)}", row.getString(1))).toArray)
      .flatMap(records => {
        for (record <- records) yield record
      })
      .toDF("id", "name4")

    val joinRDD = leftRandomRDD.join(rightNewRDD, "id")
      .map(row => (row.getString(0).split(",")(1), row.getString(2))) // 还原 key

    joinRDD.toDF()
      .foreachPartition((iterator: Iterator[Row]) => {
        val atomicInteger = new AtomicInteger()
        iterator.foreach(_ => atomicInteger.incrementAndGet())
        println(atomicInteger.get())
      })

    spark.stop()
    spark.close()
  }


}
