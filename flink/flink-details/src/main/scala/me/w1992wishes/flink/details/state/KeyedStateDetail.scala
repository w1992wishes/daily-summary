package me.w1992wishes.flink.details.state

import java.util

import org.apache.commons.compress.utils.Lists
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object KeyedStateDetail extends App {
  val env = StreamExecutionEnvironment.getExecutionEnvironment
  val dataStreamSource = env.fromElements(
    ("a", 50L), ("a", 80L), ("a", 400L),
    ("a", 100L), ("a", 200L), ("a", 200L),
    ("b", 100L), ("b", 200L), ("b", 200L),
    ("b", 500L), ("b", 600L), ("b", 700L))

  dataStreamSource
    .keyBy(_._1)
    .flatMap(new KeyedStateThresholdWarning(100L, 3)) // 超过100的阈值3次后就进行报警
    .printToErr()
  env.execute("Managed Keyed State")
}

class KeyedStateThresholdWarning(threshold: Long, numberOfTimes: Int) extends RichFlatMapFunction[(String, Long), (String, util.ArrayList[Long])] {

  // 通过ListState来存储非正常数据的状态
  private var abnormalData: ListState[Long] = _

  override def open(parameters: Configuration): Unit = {
    // 创建StateDescriptor
    val abnormalDataStateDescriptor = new ListStateDescriptor[Long]("abnormalData", classOf[Long])
    // 通过状态名称(句柄)获取状态实例，如果不存在则会自动创建
    abnormalData = getRuntimeContext.getListState(abnormalDataStateDescriptor)
  }

  override def flatMap(value: (String, Long), out: Collector[(String, util.ArrayList[Long])]): Unit = {
    val inputValue = value._2
    // 如果输入值超过阈值，则记录该次不正常的数据信息
    if (inputValue >= threshold) abnormalData.add(inputValue)
    val list = Lists.newArrayList(abnormalData.get.iterator)
    // 如果不正常的数据出现达到一定次数，则输出报警信息
    if (list.size >= numberOfTimes) {
      out.collect((value._1 + " 超过指定阈值 ", list))
      // 报警信息输出后，清空状态
      abnormalData.clear()
    }
  }
}
