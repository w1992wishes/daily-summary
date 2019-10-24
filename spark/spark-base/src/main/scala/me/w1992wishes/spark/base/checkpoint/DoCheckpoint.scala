package me.w1992wishes.spark.base.checkpoint

import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/10/24 14:24
  */
object DoCheckpoint {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("checkpoint")
      .master("local")
      .config("spark.default.parallelism", 20) // 不起作用
      .config("spark.sql.shuffle.partitions", 10)
      .getOrCreate()

    import spark.implicits._
    val text = spark.read.textFile("README.md").flatMap(_.split(" ")).map((_, 1))

    // 1
    println(text.rdd.getNumPartitions + "---------------------")

    val wordNum = text.groupByKey(_._1).count()

    // wordNum.cache()
    // spark.sparkContext.setCheckpointDir("checkout_a")
    // wordNum.checkpoint()

    wordNum.show(5)

    // 10
    println(wordNum.rdd.getNumPartitions + "---------------------")
  }

}
