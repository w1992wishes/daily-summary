# 【Spark】Spark 基础实践--Spark UDF

[TOC]

用户定义函数（User-defined functions, UDFs）是大多数 SQL 环境的关键特性，用于扩展系统的内置功能。UDF 允许开发人员通过抽象其低级语言实现来在更高级语言（如 SQL）中启用新功能。 

## 一、Spark SQL 中 UDF 用法

```scala
object SparkSqlUDF {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().master("local").appName("SparkSqlUDF").getOrCreate()

    // -------------------------  先创建测试 DataFrame ------------------------- //
    // 构造测试数据，有两个字段、名字和年龄
    val userData = Array(("A", 16), ("B", 21), ("B", 14), ("B", 18))
    // 创建测试df
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
```

## 二、DataFrame 中 UDF 用法

DataFrame 的 udf 方法虽然和 Spark Sql 的名字一样，但是属于不同的类，它在org.apache.spark.sql.functions 里。

```scala
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
```

withColumn 的功能是实现增加一列，或者替换一个已存在的列，会先判断DataFrame 里有没有这个列名，如果有的话就会替换掉原来的列，没有的话就用调用 select 方法增加一列。

## 三、UDAF 

UDAF：User Defined Aggregate Function。用户自定义聚合函数。

UDF，针对单行输入，返回一个输出；UDAF，则可以针对一组(多行)输入，进行聚合计算，返回一个输出，功能更加强大。

```scala
class CustomMaxUDAF extends UserDefinedAggregateFunction {

  // 聚合函数的输入参数数据类型
  def inputSchema: StructType = {
    StructType(StructField("input", LongType) :: Nil)
  }

  // 中间缓存的数据类型
  def bufferSchema: StructType = {
    StructType(StructField("sum", LongType) :: StructField("count", LongType) :: Nil)
  }

  // 最终输出结果的数据类型
  def dataType: DataType = LongType

  def deterministic: Boolean = true

  // 初始值，要是DataSet没有数据，就返回该值
  def initialize(buffer: MutableAggregationBuffer): Unit = {
    buffer(0) = 0L
  }

  // 相当于把当前分区的，每行数据都需要进行计算，计算的结果保存到 buffer 中
  def update(buffer: MutableAggregationBuffer, input: Row): Unit = {
    if (!input.isNullAt(0)) {
      if (input.getLong(0) > buffer.getLong(0)) {
        buffer(0) = input.getLong(0)
      }
    }
  }

  /**
    * 相当于把每个分区的数据进行汇总
    *
    * @param buffer1 分区一的数据
    * @param buffer2 分区二的数据
    */
  def merge(buffer1: MutableAggregationBuffer, buffer2: Row): Unit = {
    if (buffer2.getLong(0) > buffer1.getLong(0)) buffer1(0) = buffer2.getLong(0)
  }

  // 计算最终的结果
  def evaluate(buffer: Row): Long = buffer.getLong(0)

}
```

测试一下：

```scala
object UDAFTest {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().appName("UDAFTest").master("local[1]").getOrCreate()

    val data = Array(("A", 16), ("B", 21), ("B", 14), ("B", 18))
    val df = spark.createDataFrame(data).toDF("name", "age")

    df.createOrReplaceTempView("test")

    spark.udf.register("customMax", new CustomMaxUDAF)
    spark.sql("select customMax(age) as max from test").show()
  }
}
```

更多例子可见：https://github.com/w1992wishes/daily-summary/tree/master/spark/spark-base

