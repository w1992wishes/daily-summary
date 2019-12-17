package me.w1992wishes.spark.base.rdd

import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/12/17 14:00
  */
object GroupByKeyForTopN {

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

    import spark.implicits._

    df.rdd
      .map(row => (row.getString(0), (row.getString(1), row.getInt(2))))
      .groupByKey()
      .map(row => {
        // 组内排序
        val array = row._2.toArray
        val sortValues = array.sortBy(_._2).reverse.take(2)

        (row._1, sortValues)
      })
      // 还原格式
      .flatMap(row => {
        for (value <- row._2) yield (row._1, value._1, value._2)
      }).
      toDF("category", "name", "score").show(false)

  }

}
