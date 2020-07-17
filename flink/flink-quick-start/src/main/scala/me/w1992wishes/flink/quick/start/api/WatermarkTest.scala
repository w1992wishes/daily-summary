package me.w1992wishes.flink.quick.start.api

import java.text.SimpleDateFormat

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

/**
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
  */
object WatermarkTest {

  def main(args: Array[String]): Unit = {
    if (args.length != 4) {
      System.err.println("USAGE:\nSocketWatermarkTest <--host localhost> <--port 7777>")
      return
    }

    // 从外部命令中获取参数
    val params: ParameterTool = ParameterTool.fromArgs(args)
    val host: String = params.get("host")
    val port: Int = params.getInt("port")

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    // 不设置为 1，不方便观察
    env.setParallelism(1)

    val input: DataStream[String] = env.socketTextStream(host, port)

    val inputMap: DataStream[(String, Long)] = input.map(f => {
      val arr = f.split(",")
      val code = arr(0)
      val time = arr(1).toLong
      (code, time)
    })

    val watermark: DataStream[(String, Long)] = inputMap.assignTimestampsAndWatermarks(new AssignerWithPeriodicWatermarks[(String, Long)] {
      var currentMaxTimestamp = 0L
      val maxOutOfOrderness = 10000L //最大允许的乱序时间是10s

      val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

      override def getCurrentWatermark: Watermark = {
        new Watermark(currentMaxTimestamp - maxOutOfOrderness)
      }

      override def extractTimestamp(t: (String, Long), l: Long): Long = {
        val timestamp = t._2
        currentMaxTimestamp = Math.max(timestamp, currentMaxTimestamp)
        println(s"id:${t._1}, timestamp:[${format.format(t._2)}], currentMaxTimestamp:[${format.format(currentMaxTimestamp)}], watermark[${format.format(getCurrentWatermark.getTimestamp)}]")
        timestamp
      }
    })

    val window = watermark
      .keyBy(_._1)
      .window(TumblingEventTimeWindows.of(Time.seconds(3)))
      .apply(new WindowFunctionTest)

    window.print()

    env.execute()
  }

  class WindowFunctionTest extends WindowFunction[(String, Long), (String), String, TimeWindow] {

    override def apply(key: String, window: TimeWindow, input: Iterable[(String, Long)], out: Collector[(String)]): Unit = {
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