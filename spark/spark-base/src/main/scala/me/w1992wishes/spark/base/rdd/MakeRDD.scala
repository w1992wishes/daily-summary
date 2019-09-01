package me.w1992wishes.spark.base.rdd

import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * @author w1992wishes 2019/8/31 17:24
  */
object MakeRDD {
  def main(args: Array[String]): Unit = {
    val sparkSession = SparkSession.builder()
      .appName("make RDD")
      .master("local")
      .getOrCreate()

    parallelism(sparkSession)
    makeRdd(sparkSession)
    textFile(sparkSession)
  }

  private def parallelism(sparkSession: SparkSession): Unit = {
    val collect = Seq(1 to 10)
    val rdd = sparkSession.sparkContext.parallelize(collect, 3)

    println(rdd.partitions.length)
    println(rdd.preferredLocations(rdd.partitions(0)))
  }

  private def makeRdd(sparkSession: SparkSession): Unit = {

    // 指定 1-10 首选位置为 "master", "slave1"
    val collect = Seq((1 to 10, Seq("master", "slave1")), (11 to 15, Seq("slave2", "slave3")))
    val rdd = sparkSession.sparkContext.makeRDD(collect)

    println(rdd.partitions.length)
    println(rdd.preferredLocations(rdd.partitions(0)))
  }

  private def textFile(sparkSession: SparkSession): Unit = {

    import sparkSession.sqlContext.implicits._

    val df: DataFrame = sparkSession.read.text("README.md").toDF("line")
    println(df.count())
    println(df.rdd.partitions.length)

    sparkSession.conf.set("spark.sql.shuffle.partitions", 40)
    val shuffleDs = df
      .flatMap(line => line.getAs[String]("line").split(" "))
      .repartition()
    println(shuffleDs.rdd.partitions.length)
  }
}
