package me.w1992wishes.flink.quick.start.api

import java.text.SimpleDateFormat

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

/**
  * @author w1992wishes 2020/6/23 16:57
  */
object WindowTumblingTest {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val dataDS: DataStream[String] = env.socketTextStream("localhost", 7777)
    val waterDS = dataDS.map(
      data=>{
        val datas = data.split(",")
        WaterSensor(datas(0), datas(1).toLong)
      }
    )

    // 设定数据的事件时间已经定义Watermark
    val markDS: DataStream[WaterSensor] = waterDS.assignTimestampsAndWatermarks(
      // TODO 设定Watermark延迟5s进行窗口计算
      new BoundedOutOfOrdernessTimestampExtractor[WaterSensor](Time.seconds(3)) {
        // TODO 提取每条数据的事件时间(毫秒)
        override def extractTimestamp(element: WaterSensor): Long = {
          element.ts * 1000
        }
      }
    )

    // TODO 允许延迟数据进行处理
    markDS.keyBy(_.id)
      //.timeWindow(Time.seconds(5))
      // timeWindow(size)方法其实就是TumblingEventTimeWindows.of的一种封装
      .window(TumblingEventTimeWindows.of(Time.seconds(5)))
      .apply(
        (s:String, window:TimeWindow, list:Iterable[WaterSensor], out:Collector[String]) => {
          val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
          out.collect(s"window:[${window.getStart}-${window.getEnd}):{ ${list.mkString(",")} }")
        }
      ).print("data>>>>")

    markDS.print("watermark")

    env.execute()

  }

  case class WaterSensor(id: String, ts: Long)

}
