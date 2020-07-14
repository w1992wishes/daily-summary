package me.w1992wishes.flink.quick.start.wc

import org.apache.flink.api.scala.ExecutionEnvironment
// 解决运行隐氏转换的错误
import org.apache.flink.api.scala._
/**
  * 批处理 word count
  *
  * @author w1992wishes 2020/4/16 20:03
  */
object WordCount {
  def main(args: Array[String]): Unit = {
    // 创建执行环境
    val env = ExecutionEnvironment.getExecutionEnvironment

    // 从文件中读取数据
    val inputPath = "E:\\project\\my_project\\daily-summary\\flink\\flink-quick-start\\src\\main\\resources\\hello.txt"
    val inputDataSet = env.readTextFile(inputPath)

    // 切分数据得到 word，然后根据 word 做分组聚合
    val wordCountDataSet = inputDataSet.flatMap(_.split(" "))
      .map((_, 1))
      .groupBy(0)
      .sum(1)

    wordCountDataSet.print()
  }
}
