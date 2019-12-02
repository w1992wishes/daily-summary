package me.w1992wishes.spark.tunning.dataskew

import java.util.concurrent.atomic.AtomicInteger

import org.apache.spark.sql.{Row, SparkSession}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/**
  * @author w1992wishes 2019/12/2 16:19
  */
object SparkDataSkewWithRandomPrefix {

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

    val skewedKeyArray = Array[String]("9500048", "9500096")
    val skewedKeySet = mutable.Set[String]()
    val addList = new ArrayBuffer[String]

    Range(0, 24).map(
      i => addList += i.toString
    )

    for (key <- skewedKeyArray) {
      skewedKeySet.add(key)
    }

    val skewedKeys = spark.sparkContext.broadcast(skewedKeySet)
    val addListKeys = spark.sparkContext.broadcast(addList)

    import spark.implicits._
    // 倾斜的 key 单独找出
    val leftSkewRDD = leftRDD
      .filter(row => skewedKeys.value.contains(row.getString(0)))
      .map(row => (s"${(new Random().nextInt(24) + 1).toString},${row.getString(0)}", row.getString(1))) // 映射为随机 24 个前缀
      .toDF("id", "name3")

    // 连接的 key 加上相同的前缀，相当于扩大 N 倍
    val rightSkewRDD = rightRDD
      .filter(row => skewedKeys.value.contains(row.getString(0)))
      .map(row => addListKeys.value.map(i => (s"$i,${row.getString(0)}", row.getString(1))).toArray)
      .flatMap(records => {
        for (record <- records) yield record
      })
      .toDF("id", "name4")

    // 倾斜的 key 做连接
    val skewedJoinRDD = leftSkewRDD.join(rightSkewRDD, "id")
      .map(row => (row.getString(0).split(",")(1), row.getString(2))) // 还原 key

    val leftUnSkewRDD = leftRDD
      .filter(row => !skewedKeys.value.contains(row.getString(0)))
      .toDF("id", "mame5")

    val unskewedJoinRDD = leftUnSkewRDD.join(rightRDD, "id")
      .map(row => (row.getString(0), row.getString(1)))

    skewedJoinRDD.union(unskewedJoinRDD)
      .toDF()
      .foreachPartition((iterator: Iterator[Row]) => {
        val atomicInteger = new AtomicInteger()
        iterator.foreach(_ => atomicInteger.incrementAndGet())
        println(atomicInteger.get())
      })

    spark.stop()
    spark.close()
  }


}
