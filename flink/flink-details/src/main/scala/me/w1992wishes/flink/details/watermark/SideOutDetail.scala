package me.w1992wishes.flink.details.watermark

import java.text.SimpleDateFormat

import me.w1992wishes.flink.details.watermark.PeriodicWatermarkDetail.WindowFunctionTest
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time

/**
  * @author w1992wishes 2020/7/17 15:36
  */
object SideOutDetail {

  def main(args: Array[String]): Unit = {
    //定义socket的端口号
    val port = 9000
    //获取运行环境
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    //设置使用eventtime，默认是使用processtime
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    //设置并行度为1,默认并行度是当前机器的cpu数量
    env.setParallelism(1)
    //连接socket获取输入的数据
    val text = env.socketTextStream("localhost", port)

    //解析输入的数据
    val inputMap = text.map(
      f => {
        val arr = f.split(",")
        val code = arr(0)
        val time = arr(1).toLong
        (code, time)
      }
    )

    //抽取timestamp和生成watermark
    val waterMarkStream = inputMap.assignTimestampsAndWatermarks(new AssignerWithPeriodicWatermarks[(String, Long)] {

      var currentMaxTimestamp = 0L
      val maxOutOfOrderness = 10000L // 最大允许的乱序时间是10s

      val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

      override def getCurrentWatermark: Watermark = {
        new Watermark(currentMaxTimestamp - maxOutOfOrderness)
      }

      //定义如何提取timestamp
      override def extractTimestamp(element: (String, Long), l: Long): Long = {
        val timestamp = element._2
        currentMaxTimestamp = Math.max(timestamp, currentMaxTimestamp)
        println("键值:" + element._1 + ", 事件事件: [" + sdf.format(element._2) + "], currentMaxTimestamp: [" +
          sdf.format(currentMaxTimestamp) + "], watermark 时间: [" + sdf.format(getCurrentWatermark().getTimestamp) + "]")
        timestamp
      }
    })

    //保存被丢弃的数据
    val outputTag = new OutputTag[(String, Long)]("late-data")
    //注意，由于getSideOutput方法是SingleOutputStreamOperator子类中的特有方法，所以这里的类型，不能使用它的父类dataStream。
    val window = waterMarkStream.keyBy(0)
      .keyBy(_._1)
      .window(TumblingEventTimeWindows.of(Time.seconds(3))) //按照消息的EventTime分配窗口，和调用TimeWindow效果一样
      //.allowedLateness(Time.seconds(2))//允许数据迟到2秒
      .sideOutputLateData(outputTag)
      .apply(new WindowFunctionTest)
    //把迟到的数据暂时打印到控制台，实际中可以保存到其他存储介质中
    val sideOutput = window.getSideOutput(outputTag)
    sideOutput.print()
    //测试-把结果打印到控制台即可
    window.print()

    //注意：因为flink是懒加载的，所以必须调用execute方法，上面的代码才会执行
    env.execute("eventtime-watermark")
  }

}
