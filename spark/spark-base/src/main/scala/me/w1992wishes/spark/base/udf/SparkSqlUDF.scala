package me.w1992wishes.spark.base.udf

import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/9/6 15:06
  */
object SparkSqlUDF {

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

    // -------------------------  通过匿名函数注册UDF ------------------------- //
    spark.udf.register("strLen", (str: String) => str.length())
    spark.sql("select name, strLen(name) as name_len from user").show

    // -------------------------  通过实名函数注册UDF ------------------------- //
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

    spark.udf.register("isAdult", isAdult _)
    spark.sql("select name, isAdult(age) as isAdult from user").show
  }

}
