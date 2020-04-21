package me.w1992wishes.flink.quick.start

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
// 解决运行隐氏转换的错误
import org.apache.flink.api.scala._

/**
  * 流处理 word count
  *
  * @author w1992wishes 2020/4/20 10:30
  */
object StreamingWordCount {
  def main(args: Array[String]): Unit = {
    val param = ParameterTool.fromArgs(args)
    val host = param.get("host")
    val port = param.getInt("port")

    // 创建流处理执行环境
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    // 接受一个 socket 文本流
    val dataStream = env.socketTextStream(host, port)

    // 每条数据处理和转换
    val wordCountDataStream = dataStream.flatMap(_.split(" "))
      .filter(_.nonEmpty)
      .map((_, 1))
      .keyBy(0)
      .sum(1)

    wordCountDataStream.print()

    // 启动 executor
    env.execute("stream job word count")
  }
}
