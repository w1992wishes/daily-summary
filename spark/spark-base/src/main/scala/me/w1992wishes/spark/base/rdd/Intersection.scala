package me.w1992wishes.spark.base.rdd

import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/8/21 11:42
  */
object Intersection {

  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession.builder().master("local").getOrCreate()

    val rdd1 = spark.sparkContext.parallelize(Seq(1, 2, 3, 4, 5, 6, 7, 8, 9), 5)
    val rdd2 = spark.sparkContext.parallelize(Seq(2, 4, 6, 8), 2)

    // 默认 parition 数量取 max(numPartitions(parent rdd 1), ... , numPartitions(parent rdd n))
    println(rdd1.intersection(rdd2).getNumPartitions)
  }

}
