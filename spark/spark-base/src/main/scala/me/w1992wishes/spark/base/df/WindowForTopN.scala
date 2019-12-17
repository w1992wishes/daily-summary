package me.w1992wishes.spark.base.df

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.expressions.Window

/**
  * @author w1992wishes 2019/12/17 13:51
  */
object WindowForTopN {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().appName("WindowForTopN").master("local").getOrCreate()

    val df = spark.createDataFrame(
      Array(
        ("A", "Tom", 78),
        ("B", "James", 47),
        ("A", "Jim", 43), ("C", "James", 89),
        ("A", "Lee", 93), ("C", "Jim", 65),
        ("A", "James", 10),
        ("C", "Lee", 39),
        ("A", "James", 10),
        ("C", "Lee", 39),
        ("B", "Tom", 99),
        ("C", "Tom", 53),
        ("B", "Lee", 100),
        ("B", "Jim", 100)
      )).toDF("category", "name", "score")

    val win = Window.partitionBy("category").orderBy(org.apache.spark.sql.functions.col("score").desc)

    val topN = df.withColumn("topN", org.apache.spark.sql.functions.row_number().over(win))
      .where(org.apache.spark.sql.functions.col("topN") < 3)

    topN.show()


  }

}
