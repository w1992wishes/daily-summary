package me.w1992wishes.spark.base.rdd

import org.apache.spark.sql.SparkSession

/**
  * Spark Scala TopN分组排序，核心思想是先对分组内部排序，取前Ｎ个，然后对分组之间进行排序；
  *
  * @author w1992wishes 2019/9/2 17:35
  */
object GroupBy {
  def main(args: Array[String]): Unit = {

    val spark: SparkSession = SparkSession.builder().appName("TopNSecond by Scala").master("local").getOrCreate()

    val data = spark.sparkContext.textFile("src/main/resources/TopNSecond.txt", 1)
    val lines = data.map(line => (line.split(" ")(0), line.split(" ")(1).toInt))
    val groups = lines.groupByKey()

    /**
      * groupBy和groupByKey是不同的，比如（A，1），（A，2）；使用groupBy之后结果是（A，（（A，1），（A，2）））；
      * 使用groupByKey之后结果是：（A，（1,2））；关键区别就是合并之后是否会自动去掉key信息；
      */
    val groupsSort = groups.map(tu => {
      val key = tu._1
      val values = tu._2
      val sortValues = values.toList.sortWith(_ > _).take(4)
      (key, sortValues)
    })
    groupsSort.sortBy(tu => tu._1, ascending = false, 1).collect().foreach(value => {
      print(value._1)
      value._2.foreach(v => print("\t" + v))
      println()
    })

    /**
      * Spark	98	95	95	87
      * Kafka	76	70	67
      * Hadoop	98	98	91	68
      * Flink	85	82	55
      */
    spark.stop()
  }
}
