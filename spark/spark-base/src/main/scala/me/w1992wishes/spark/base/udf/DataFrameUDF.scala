package me.w1992wishes.spark.base.udf

import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/9/6 15:17
  */
object DataFrameUDF {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().master("local").appName("SparkSqlUDF").getOrCreate()

    // -------------------------  先创建测试 DataFrame ------------------------- //
    // 构造测试数据，有两个字段、名字和年龄
    val userData = Array(("A", 16), ("B", 21), ("B", 14), ("B", 18))
    //创建测试df
    val userDF = spark.createDataFrame(userData).toDF("name", "age")
    userDF.show

    // 注册一张user表
    userDF.createOrReplaceTempView("user")

    // -------------------------  注册 UDF ------------------------- //

    import org.apache.spark.sql.functions._

    //方法一：注册自定义函数（通过匿名函数）
    val strLen = udf((str: String) => str.length())

    //方法二：注册自定义函数（通过实名函数）
    /**
      * 根据年龄大小返回是否成年 成年：true,未成年：false
      */
    def isAdult(age: Int) = {
      if (age < 18) {
        false
      } else {
        true
      }
    }

    val udf_isAdult = udf(isAdult _)

    // -------------------------  使用 UDF ------------------------- //
    // 可通过 withColumn 和 select 使用，下面的代码已经实现了给 user 表添加两列的功能
    // 通过withColumn添加列
    userDF.withColumn("name_len", strLen(col("name"))).withColumn("isAdult", udf_isAdult(col("age"))).show
    //通过select添加列
    userDF.select(col("*"), strLen(col("name")) as "name_len", udf_isAdult(col("age")) as "isAdult").show
  }

}
