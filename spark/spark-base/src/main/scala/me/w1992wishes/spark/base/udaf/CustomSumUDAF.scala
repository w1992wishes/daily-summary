package me.w1992wishes.spark.base.udaf

import org.apache.spark.sql.Row
import org.apache.spark.sql.expressions.{MutableAggregationBuffer, UserDefinedAggregateFunction}
import org.apache.spark.sql.types.{DataType, LongType, StructField, StructType}

/**
  * ).initialize()方法,初使使，即没数据时的值
  * ).update() 方法把每一行的数据进行计算，放到缓冲对象中
  * ).merge() 把每个分区，缓冲对象进行合并
  * ).evaluate()计算结果表达式，把缓冲对象中的数据进行最终计算
  */
class CustomSumUDAF extends UserDefinedAggregateFunction {

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

  // 确保一致性 一般用 true,用以标记针对给定的一组输入，UDAF 是否总是生成相同的结果
  def deterministic: Boolean = true

  // 初始值，要是 DataSet 没有数据，就返回该值
  def initialize(buffer: MutableAggregationBuffer): Unit = {
    buffer(0) = 0L
    buffer(1) = 0L
  }

  /**
    * 局部聚合，相当于把当前分区的，每行数据都需要进行计算，计算的结果保存到buffer中
    *
    * @param buffer buffer
    * @param input  Row
    */
  def update(buffer: MutableAggregationBuffer, input: Row): Unit = {
    if (!input.isNullAt(0)) {
      buffer(0) = buffer.getLong(0) + input.getLong(0)
      buffer(1) = buffer.getLong(1) + 1
    }
  }

  /**
    * 相当于把每个分区的数据进行汇总
    *
    * @param buffer1 分区一的数据
    * @param buffer2 分区二的数据
    */
  def merge(buffer1: MutableAggregationBuffer, buffer2: Row): Unit = {
    buffer1(0) = buffer1.getLong(0) + buffer2.getLong(0)
    buffer1(1) = buffer1.getLong(1) + buffer2.getLong(1)
  }

  //计算最终的结果
  def evaluate(buffer: Row): (Long, Long) = (buffer.getLong(0), buffer.getLong(1))

}
