package me.w1992wishes.flink.details.watermark

import java.text.SimpleDateFormat

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

/**
  *
  * 0001,1538359882000		2018-10-01 10:11:22
  * 0002,1538359886000		2018-10-01 10:11:26
  * 0003,1538359892000		2018-10-01 10:11:32
  * 0004,1538359893000		2018-10-01 10:11:33
  * 0005,1538359894000		2018-10-01 10:11:34
  * 0006,1538359896000		2018-10-01 10:11:36
  * 0007,1538359897000		2018-10-01 10:11:37
  *
  * 0008,1538359899000		2018-10-01 10:11:39
  * 0009,1538359891000		2018-10-01 10:11:31
  * 0010,1538359903000		2018-10-01 10:11:43
  *
  * 0011,1538359892000		2018-10-01 10:11:32
  * 0012,1538359891000		2018-10-01 10:11:31
  *
  * 0010,1538359906000		2018-10-01 10:11:46
  *
  *
  * @author w1992wishes 2020/7/17 14:22
  */
object PeriodicWatermarkDetail {

  def main(args: Array[String]): Unit = {
    //定义socket的端口号
    val port = 9010
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

    val window = waterMarkStream
      .keyBy(_._1)
      .window(TumblingEventTimeWindows.of(Time.seconds(3))) //按照消息的EventTime分配窗口，和调用TimeWindow效果一样
      .apply(new WindowFunctionTest)

    //测试-把结果打印到控制台即可
    window.print()

    //注意：因为flink是懒加载的，所以必须调用execute方法，上面的代码才会执行
    env.execute("eventtime-watermark")

  }

  class WindowFunctionTest extends WindowFunction[(String, Long), (String), String, TimeWindow] {
    /**
      * 对window内的数据进行排序，保证数据的顺序
      */
    override def apply(key: String, window: TimeWindow, input: Iterable[(String, Long)], out: Collector[String]): Unit = {
      val list = input.toList.sortBy(_._2)
      val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
      val result =
        s"""
           |"键值: $key
           | 触发窗内数据个数: ${input.size}
           | 触发窗起始数据：${format.format(list.head._2)}
           | 触发窗最后（可能是延时）数据：${format.format(list.last._2)}
           | 实际窗起始和结束时间：${format.format(window.getStart)}《----》${format.format(window.getEnd)} \n
        """.stripMargin
      out.collect(result)
    }
  }

}
