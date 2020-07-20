package me.w1992wishes.flink.details.state

import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

object OperatorStateDetail extends App {
  val env = StreamExecutionEnvironment.getExecutionEnvironment
  // 开启检查点机制
  env.enableCheckpointing(1000)
  env.setParallelism(1)
  // 设置并行度为1
  val dataStreamSource = env.fromElements(
    ("a", 50L), ("a", 80L), ("a", 400L),
    ("a", 100L), ("a", 200L), ("a", 200L),
    ("b", 100L), ("b", 200L), ("b", 200L),
    ("b", 500L), ("b", 600L), ("b", 700L))
  dataStreamSource
    .flatMap(new OperatorStateDetailThresholdWarning(100L, 3))
    .printToErr()
  env.execute("Managed Operator State")
}

class OperatorStateDetailThresholdWarning(threshold: Long, numberOfTimes: Int) extends RichFlatMapFunction[(String, Long), (String, ListBuffer[(String, Long)])] with CheckpointedFunction {

  // 正常数据缓存
  private var bufferedData: ListBuffer[(String, Long)] = ListBuffer[(String, Long)]()

  // checkPointedState
  private var checkPointedState: ListState[(String, Long)] = _

  override def flatMap(value: (String, Long), out: Collector[(String, ListBuffer[(String, Long)])]): Unit = {
    val inputValue = value._2
    // 超过阈值则进行记录
    if (inputValue >= threshold) {
      bufferedData += value
    }
    // 超过指定次数则输出报警信息
    if (bufferedData.size >= numberOfTimes) {
      // 顺便输出状态实例的hashcode
      out.collect((checkPointedState.hashCode() + "阈值警报！", bufferedData))
      bufferedData.clear()
    }
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    // 在进行快照时，将数据存储到checkPointedState
    checkPointedState.clear()
    for (element <- bufferedData) {
      checkPointedState.add(element)
    }
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    // 注册ListStateDescriptor
    val descriptor = new ListStateDescriptor[(String, Long)](
      "buffered-abnormalData", TypeInformation.of(new TypeHint[(String, Long)]() {})
    )

    // 从FunctionInitializationContext中获取OperatorStateStore，进而获取ListState
    checkPointedState = context.getOperatorStateStore.getListState(descriptor)

    // 如果是作业重启，读取存储中的状态数据并填充到本地缓存中
    if (context.isRestored) {
      for (element <- checkPointedState.get()) {
        bufferedData += element
      }
    }
  }
}
