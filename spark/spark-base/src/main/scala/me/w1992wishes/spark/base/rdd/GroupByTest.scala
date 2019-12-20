package me.w1992wishes.spark.base.rdd

import org.apache.spark.{SparkConf, SparkContext}

import scala.util.Random

object GroupByTest {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setAppName("GroupBy Test").setMaster("local")

    val numMappers = 100
    val numKVPairs = 10000
    val valSize = 1000
    val numReducers = 36

    val sc = new SparkContext(sparkConf)

    val pairs1 = sc.parallelize(0 until numMappers, numMappers)
      .flatMap { p =>
        val ranGen = new Random
        val arr1 = new Array[(Int, Array[Byte])](numKVPairs)
        for (i <- 0 until numKVPairs) {
          val byteArr = new Array[Byte](valSize)
          ranGen.nextBytes(byteArr)
          arr1(i) = (ranGen.nextInt(Int.MaxValue), byteArr)
        }
        arr1
      }.cache

    // Enforce that everything has been calculated and in cache
    // 1000000
    println(pairs1.count)

    // 999762
    println(pairs1.groupByKey(numReducers).count)

    sc.stop()
  }
}