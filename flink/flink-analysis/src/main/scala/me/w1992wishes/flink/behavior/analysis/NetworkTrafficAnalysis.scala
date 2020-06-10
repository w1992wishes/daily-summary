package me.w1992wishes.flink.behavior.analysis

import java.sql.Timestamp
import java.text.SimpleDateFormat

import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer

/**
  * 输入数据格式
  *
  * @param ip        访问的IP
  * @param userId    访问的userID
  * @param eventTime 访问时间
  * @param method    访问方法 get/post/put/delete
  * @param url       访问的URL
  */
case class ApacheLogEvent(ip: String, userId: String, eventTime: Long, method: String, url: String)

//输出数据格式
case class UrlViewCount(url: String, windowEnd: Long, count: Long)

/**
  * 实时流量统计：每隔5秒，输出最近10分钟内访问量最多的前N个URL
  *
  * @author w1992wishes 2020/6/10 11:41
  */
object TrafficAnalysis {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)
    val stream = env
      // 以window下为例，需替换成自己的路径
      .readTextFile("E:\\project\\my_project\\daily-summary\\flink\\flink-analysis\\src\\main\\resources\\apache.log")
      .map(line => {
        val linearray = line.split(" ")
        val simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy:HH:mm:ss")
        val timestamp = simpleDateFormat.parse(linearray(3)).getTime
        ApacheLogEvent(linearray(0), linearray(2), timestamp, linearray(5), linearray(6))
      })
      .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[ApacheLogEvent](Time.milliseconds(1000)) {
        override def extractTimestamp(t: ApacheLogEvent): Long = {
          t.eventTime
        }
      })
      .filter(_.method == "GET")
      .keyBy(_.url)
      .timeWindow(Time.minutes(1), Time.seconds(5))
      .aggregate(new CountAgg(), new WindowResultFunction())
      .keyBy(_.windowEnd)
      .process(new TopNHotUrls(5))
      .print()
    env.execute("Traffic Analysis Job")
  }
}

//自定义实现聚合函数
class CountAgg() extends AggregateFunction[ApacheLogEvent, Long, Long] {
  //迭代状态的初始值
  override def createAccumulator(): Long = 0L

  //每一条输入数据，和迭代数据如何迭代
  override def add(in: ApacheLogEvent, acc: Long): Long = acc + 1

  //多个分区的迭代数据如何合并
  override def getResult(acc: Long): Long = acc

  //返回数据，对最终的迭代数据如何处理，并返回结果。
  override def merge(acc: Long, acc1: Long): Long = acc + acc1
}

//自定义实现windowFunction输出的是
//in:输入累加器类型,out累加后输出类型,key tuple泛型,w聚合的窗口 w.getRnd可以拿到窗口的结束时间
class WindowResultFunction extends WindowFunction[Long, UrlViewCount, String, TimeWindow] {
  override def apply(key: String, window: TimeWindow, aggregateResult: Iterable[Long],
                     collector: Collector[UrlViewCount]): Unit = {
    val url: String = key
    val count = aggregateResult.iterator.next
    collector.collect(UrlViewCount(url, window.getEnd, count))
  }
}

//自定义processFunction,统计访问量最大的URL,排序输出
class TopNHotUrls extends KeyedProcessFunction[Long, UrlViewCount, String] {
  private var topSize = 0

  def this(topSize: Int) {
    this()
    this.topSize = topSize
  }

  //直接定义状态变量,懒加载
  lazy val urlState: ListState[UrlViewCount] = getRuntimeContext.getListState(new ListStateDescriptor[UrlViewCount]("urlState", classOf[UrlViewCount]))

  override def processElement(i: UrlViewCount, context: KeyedProcessFunction[Long, UrlViewCount, String]#Context, collector: Collector[String]): Unit = {
    //将每条数据保存到状态中
    urlState.add(i)
    //注册定时器,windowEnd
    context.timerService().registerEventTimeTimer(i.windowEnd + 10 * 1000)
  }

  //实现onTimes
  override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[Long, UrlViewCount, String]#OnTimerContext, out: Collector[String]): Unit = {

    //从状态中获取所有的URL的访问量
    val allUrlViews: ListBuffer[UrlViewCount] = ListBuffer()

    import scala.collection.JavaConversions._

    for (urlView <- urlState.get()) {
      allUrlViews += urlView
    }
    //情况state
    urlState.clear()
    //按照访问量排序输出
    val sortedUrlViews = allUrlViews.sortBy(_.count)(Ordering.Long.reverse).take(topSize)
    //  将排名信息格式化成 String,  便于打印
    var result: StringBuilder = new StringBuilder
    result.append("====================================\n")
    result.append(" 时间: ").append(new Timestamp(timestamp - 1)).append("\n")
    for (i <- sortedUrlViews.indices) {
      val currentItem: UrlViewCount = sortedUrlViews(i)
      // e.g. No1 ： URL =/blog/tags/firefox?flav=rss20 流量 =55
      result.append("No").append(i + 1).append(":")
        .append(" URL=").append(currentItem.url)
        .append(" 流量=").append(currentItem.count).append("\n")
    }
    result.append("====================================\n\n")
    //  控制输出频率，模拟实时滚动结果
    Thread.sleep(1000)
    out.collect(result.toString)
  }
}