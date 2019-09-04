package me.w1992wishes.spark.base.udf

import java.lang
 
import org.apache.spark.sql.expressions.{MutableAggregationBuffer, UserDefinedAggregateFunction}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Dataset, Row, SparkSession}
 
object UDAFys {
 
  def main(args: Array[String]): Unit = {
 
    val session: SparkSession = SparkSession.builder().appName("UDAFTest").master("local[*]").getOrCreate()
 
 
    val udaf = new UDAFys
 
    //注册函数
//    session.udf.register("udaf",udaf)
     val range: Dataset[lang.Long] = session.range(1, 11)
//    range.createTempView("table")
//    val df = session.sql("SELECT udaf(id) result FROM table")
 
    import session.implicits._
 
    val df = range.agg(udaf($"id").as("geomean"))
    df.show()
 
    session.stop()
  }
}
 
 
class UDAFys extends UserDefinedAggregateFunction {
 
  //输入数据的类型
  override def inputSchema: StructType = StructType(List(
    StructField("value", DoubleType)
  ))
 
  //产生中间结果的数据类型
  override def bufferSchema: StructType = StructType(List(
    //相乘之后返回的积
    StructField("project", DoubleType),
    //参与运算数字的个数
    StructField("Num", LongType)
  ))
 
  //最终返回的结果类型
  override def dataType: DataType = DoubleType
 
  //确保一致性，一般用true
  override def deterministic: Boolean = true
 
  //指定初始值
  override def initialize(buffer: MutableAggregationBuffer): Unit = {
    //相乘的初始值，这里的要和上边的中间结果的类型和位置相对应
    buffer(0) = 1.0
    //参与运算数字个数的初始值
    buffer(1) = 0L
  }
 
  //每有一条数据参与运算就更新一下中间结果(update相当于在每一个分区中的计算)
  override def update(buffer: MutableAggregationBuffer, input: Row): Unit = {
    //每有一个数字参与运算就进行相乘(包含中间结果)
    buffer(0) = buffer.getDouble(0) * input.getDouble(0)
    //参与运算的数字个数更新
    buffer(1) = buffer.getLong(1) + 1L
  }
 
  //全局聚合
  override def merge(buffer1: MutableAggregationBuffer, buffer2: Row): Unit = {
    //每个分区计算的结果进行相乘
    buffer1(0) = buffer1.getDouble(0) * buffer2.getDouble(0)
 
    buffer1(1) = buffer1.getLong(1) + buffer2.getLong(1)
  }
 
  //计算最终的结果
  override def evaluate(buffer: Row): Any = {
    math.pow(buffer.getDouble(0), 1.toDouble / buffer.getLong(1))
  }
}