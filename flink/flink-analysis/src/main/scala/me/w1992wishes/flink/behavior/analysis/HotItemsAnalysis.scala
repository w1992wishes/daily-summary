package me.w1992wishes.flink.behavior.analysis

import java.sql.Timestamp
import java.util.Properties

import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.java.tuple.{Tuple, Tuple1}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer

/**
  * 实时热门商品：每隔 5 分钟输出过去一小时内点击量最多的前 N 个商品
  *
  * @author w1992wishes 2020/6/9 20:51
  */
object HotItemsAnalysis {


  /**
    * 输出数据样例类
    *
    * @param itemId    主键商品ID
    * @param windowEnd 计算的窗口
    * @param count     点击量
    */
  case class ItemViewCount(itemId: Long, windowEnd: Long, count: Long)

  /**
    * 输入数据样例类
    *
    * @param userId     加密后的用户ID
    * @param itemId     加密后的商品ID
    * @param categoryId 加密后的商品所属类别
    * @param behavior   用户行为,pv:浏览量 buy:商品购买 cart:加购物车 fav:收藏
    * @param timestamp  时间戳毫秒数
    */
  case class UserBehavior(userId: Long, itemId: Long, categoryId: Int, behavior: String, timestamp: Long)

  def main(args: Array[String]): Unit = {
    // 创建一个 StreamExecutionEnvironment
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    // 设定Time类型为EventTime（Flink 默认使用 ProcessingTime 处理）
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    // 为了打印到控制台的结果不乱序，我们配置全局的并发为1，这里改变并发对结果正确性没有影响
    env.setParallelism(1)

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "localhost:9092")
    properties.setProperty("group.id", "consumer-group")
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("auto.offset.reset", "latest")

    //  readTextFile 或者 addSource 选其一
    val stream = env
      //.readTextFile("E:\\project\\my_project\\daily-summary\\flink\\flink-analysis\\src\\main\\resources\\UserBehavior.csv") // 以window下为例，需替换成自己的路径
      .addSource(new FlinkKafkaConsumer[String]("hotitems", new SimpleStringSchema(), properties)) // 从 kafka 读取
      .map(line => {
        val linearray = line.split(",")
        UserBehavior(linearray(0).toLong, linearray(1).toLong, linearray(2).toInt, linearray(3), linearray(4).toLong)
      })
      // 指定时间戳和 watermark
      // Watermark是用来追踪业务事件的概念，可以理解成EventTime世界中的时钟，用来指示当前处理到什么时刻的数据了。由于数据源的数据已经经过整理，没有乱序，即事件的时间戳是单调递增的，所以可以将每条数据的业务时间就当做Watermark。这里我们用 assignAscendingTimestamps来实现时间戳的抽取和Watermark的生成。
      // （真实业务场景一般都是乱序的，所以一般不用assignAscendingTimestamps，而是使用BoundedOutOfOrdernessTimestampExtractor。）
      .assignAscendingTimestamps(_.timestamp * 1000)
      .filter(_.behavior == "pv") // 过滤出点击事件
      // 由于要每隔5分钟统计一次最近一小时每个商品的点击量，所以窗口大小是一小时，每隔5分钟滑动一次。即分别要统计[09:00, 10:00), [09:05, 10:05), [09:10, 10:10)…等窗口的商品点击量。是一个常见的滑动窗口需求（Sliding Window）。
      .keyBy("itemId") // 对商品进行分组
      .timeWindow(Time.minutes(60), Time.minutes(5)) // 对每个商品做滑动窗口（1小时窗口，5分钟滑动一次）
      // .aggregate(AggregateFunction af, WindowFunction wf) 做增量的聚合操作，它能使用 AggregateFunction 提前聚合掉数据，减少state的存储压力。较之 .apply(WindowFunction wf) 会将窗口中的数据都存储下来，最后一起计算要高效地多。这里的CountAgg实现了AggregateFunction接口，功能是统计窗口中的条数，即遇到一条数据就加一
      // .aggregate(AggregateFunction af, WindowFunction wf) 的第二个参数 WindowFunction 将每个key每个窗口聚合后的结果带上其他信息进行输出。这里实现的 WindowResultFunction 将<主键商品ID，窗口，点击量>封装成了ItemViewCount 进行输出。
      .aggregate(new CountAgg(), new WindowResultFunction())
      .keyBy("windowEnd") // 为了统计每个窗口下最热门的商品，需要再次按窗口进行分组，这里根据ItemViewCount中的windowEnd进行keyBy()操作
      .process(new TopNHotItems(3)) // 然后使用 ProcessFunction 实现一个自定义的 TopN 函数 TopN HotItems 来计算点击量排名前 3 名的商品，并将排名结果格式化成字符串，便于后续输出。

    stream.print()
    env.execute("Hot Items Job")
  }

  // COUNT统计的聚合函数实现，每出现一条记录就加一
  class CountAgg extends AggregateFunction[UserBehavior, Long, Long] {
    override def createAccumulator(): Long = 0L

    override def add(userBehavior: UserBehavior, acc: Long): Long = acc + 1

    override def getResult(acc: Long): Long = acc

    override def merge(acc1: Long, acc2: Long): Long = acc1 + acc2
  }

  // 用于输出窗口的结果
  class WindowResultFunction extends WindowFunction[Long, ItemViewCount, Tuple, TimeWindow] {
    override def apply(key: Tuple, window: TimeWindow, aggregateResult: Iterable[Long],
                       collector: Collector[ItemViewCount]): Unit = {
      val itemId: Long = key.asInstanceOf[Tuple1[Long]].f0
      val count = aggregateResult.iterator.next
      collector.collect(ItemViewCount(itemId, window.getEnd, count))
    }
  }

  // 求某个窗口中前 N 名的热门点击商品，key 为窗口时间戳，输出为 TopN 的结果字符串
  class TopNHotItems(topSize: Int) extends KeyedProcessFunction[Tuple, ItemViewCount, String] {
    private var itemState: ListState[ItemViewCount] = _

    override def open(parameters: Configuration): Unit = {
      super.open(parameters)
      // 命名状态变量的名字和状态变量的类型
      val itemsStateDesc = new ListStateDescriptor[ItemViewCount]("itemState-state", classOf[ItemViewCount])
      // 定义状态变量
      itemState = getRuntimeContext.getListState(itemsStateDesc)
    }

    override def processElement(input: ItemViewCount, context: KeyedProcessFunction[Tuple, ItemViewCount, String]#Context, collector: Collector[String]): Unit = {
      // 每条数据都保存到状态中
      itemState.add(input)
      // 注册 windowEnd+1 的 EventTime Timer, 当触发时，说明收齐了属于 windowEnd 窗口的所有商品数据
      // 也就是当程序看到 windowend + 1 的水位线 watermark 时，触发 onTimer 回调函数
      context.timerService.registerEventTimeTimer(input.windowEnd + 1)
    }

    override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[Tuple, ItemViewCount, String]#OnTimerContext, out: Collector[String]): Unit = {
      // 获取收到的所有商品点击量
      val allItems: ListBuffer[ItemViewCount] = ListBuffer()
      import scala.collection.JavaConversions._
      for (item <- itemState.get) {
        allItems += item
      }
      // 提前清除状态中的数据，释放空间
      itemState.clear()
      // 按照点击量从大到小排序
      val sortedItems = allItems.sortBy(_.count)(Ordering.Long.reverse).take(topSize)
      // 将排名信息格式化成 String, 便于打印
      val result: StringBuilder = new StringBuilder
      result.append("====================================\n")
      result.append("时间: ").append(new Timestamp(timestamp - 1)).append("\n")

      for (i <- sortedItems.indices) {
        val currentItem: ItemViewCount = sortedItems(i)
        // e.g.  No1：  商品ID=12224  浏览量=2413
        result.append("No").append(i + 1).append(":")

          .append("  商品ID=").append(currentItem.itemId)

          .append("  浏览量=").append(currentItem.count).append("\n")
      }
      result.append("====================================\n\n")
      // 控制输出频率，模拟实时滚动结果
      Thread.sleep(1000)
      out.collect(result.toString)
    }
  }
}
