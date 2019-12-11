package me.w1992wishes.spark.window.demo

import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * @author w1992wishes 2019/7/10 10:52
  */
object Example {

  def main(args: Array[String]): Unit = {

    val sparkSession = SparkSession.builder()
      .appName("RDD to DataFrame")
      .master("local")
      .getOrCreate()
    import sparkSession.implicits._

    val df = List(
      ("站点1", "2017-01-01", 50),
      ("站点1", "2017-01-03", 55),
      ("站点2", "2017-01-01", 25),
      ("站点1", "2017-01-02", 45),
      ("站点2", "2017-01-02", 29),
      ("站点2", "2017-01-03", 27),
      ("站点4", "2017-01-03", 27)
    ).toDF("site", "date", "user_cnt")

    df.cache()

    f1(df)
    f2(df)
    f3(df)

    Thread.sleep(10000000)
  }

  /**
    * 简单移动平均值
    *
    * @param df df
    */
  def f1(df: DataFrame): Unit = {

    import org.apache.spark.sql.expressions.Window
    // 这个 window spec 中，数据根据用户(customer)来分区
    // 每一个用户数据根据时间排序。然后，窗口定义从 -1(前一行)到 1(后一行)	，每一个滑动的窗口总用有3行
    val wSpec = Window.partitionBy("site")
      .orderBy("date")
      .rowsBetween(-1, 1)

    import org.apache.spark.sql.functions._
    df.withColumn("movingAvg",
      avg(df("user_cnt")).over(wSpec)).show()

    /**
      * +----+----------+--------+---------+
      * |site|      date|user_cnt|movingAvg|
      * +----+----------+--------+---------+
      * | 站点1|2017-01-01|      50|     47.5|
      * | 站点1|2017-01-02|      45|     50.0|
      * | 站点1|2017-01-03|      55|     50.0|
      * | 站点2|2017-01-01|      25|     27.0|
      * | 站点2|2017-01-02|      29|     27.0|
      * | 站点2|2017-01-03|      27|     28.0|
      * +----+----------+--------+---------+
      */
  }

  /**
    * 累计汇总
    *
    * @param df df
    */
  def f2(df: DataFrame): Unit = {
    import org.apache.spark.sql.expressions.Window

    val wSpec = Window.partitionBy("site")
      .orderBy("date")
      .rowsBetween(Long.MinValue, 0)

    import org.apache.spark.sql.functions._

    df.withColumn("cumSum",
      sum(df("user_cnt")).over(wSpec)).show()

    /**
      * +----+----------+--------+------+
      * |site|      date|user_cnt|cumSum|
      * +----+----------+--------+------+
      * | 站点4|2017-01-03|      27|    27|
      * | 站点1|2017-01-01|      50|    50|
      * | 站点1|2017-01-02|      45|    95|
      * | 站点1|2017-01-03|      55|   150|
      * | 站点2|2017-01-01|      25|    25|
      * | 站点2|2017-01-02|      29|    54|
      * | 站点2|2017-01-03|      27|    81|
      * +----+----------+--------+------+
      */
  }

  /**
    * 排名
    *
    * @param df df
    */
  def f3(df: DataFrame): Unit = {
    import org.apache.spark.sql.expressions.Window
    val wSpec = Window.partitionBy("site")
      .orderBy("date")

    import org.apache.spark.sql.functions._
    df.withColumn("rank",
      rank().over(wSpec)).show()

    /**
      * +----+----------+--------+----+
      * |site|      date|user_cnt|rank|
      * +----+----------+--------+----+
      * | 站点4|2017-01-03|      27|   1|
      * | 站点1|2017-01-01|      50|   1|
      * | 站点1|2017-01-02|      45|   2|
      * | 站点1|2017-01-03|      55|   3|
      * | 站点2|2017-01-01|      25|   1|
      * | 站点2|2017-01-02|      29|   2|
      * | 站点2|2017-01-03|      27|   3|
      * +----+----------+--------+----+
      */
  }
}
