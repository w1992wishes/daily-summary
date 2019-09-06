package me.w1992wishes.spark.base.udaf

import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/9/6 16:24
  */
object UDAFTest {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().appName("UDAFTest").master("local[1]").getOrCreate()

    val data = Array(("A", 16), ("B", 21), ("B", 14), ("B", 18))
    val df = spark.createDataFrame(data).toDF("name", "age")

    df.createOrReplaceTempView("test")

    spark.udf.register("customCount", new CustomCountUDAF)
    spark.sql("select customCount(name) as count from test").show()

    spark.udf.register("customMax", new CustomMaxUDAF)
    spark.sql("select customMax(age) as max from test").show()
  }
}
