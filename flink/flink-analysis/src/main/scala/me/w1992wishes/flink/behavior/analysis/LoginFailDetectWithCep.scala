package me.w1992wishes.flink.behavior.analysis

import org.apache.flink.cep.scala.CEP
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

/**
  * 直接把每次登录失败的数据存起来、设置定时器一段时间后再读取，这种做法尽管简单，但和我们开始的需求还是略有差异的。
  *
  * 这种做法只能隔2秒之后去判断一下这期间是否有多次失败登录，而不是在一次登录失败之后、再一次登录失败时就立刻报警。
  *
  * flink为我们提供了CEP（Complex Event Processing，复杂事件处理）库，用于在流中筛选符合某种复杂模式的事件。
  *
  * begin\notFollowedBy\followedBy 表示事件的类型
  * begin: 事件的起始
  * next: 紧挨着的事件
  * followedBy： 在之后的事件（但不一定紧接着）
  * notNext: 紧挨着没有发生的事件
  * notFollowedBy: 之后再也没有发生
  * start\without\follow 部分：为该事件取名字，用于之后提取该阶段匹配到的相关事件
  * login\item\coupon 部分：返回一个 boolean 类型，通过流中数据来判断是否匹配事件
  *
  * @author w1992wishes 2020/6/10 15:47
  */
object LoginFailDetectWithCep {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    val loginStream = env.fromCollection(List(
      LoginEvent(1, "192.168.0.1", "fail", 1558430842),
      LoginEvent(1, "192.168.0.2", "fail", 1558430843),
      LoginEvent(1, "192.168.0.3", "fail", 1558430844),
      LoginEvent(2, "192.168.10.10", "success", 1558430845)
    )).assignAscendingTimestamps(_.eventTime * 1000)
      .keyBy(_.userId)

    //  定义匹配模式
    val loginFaiPattern = Pattern
      .begin[LoginEvent]("begin").where(_.eventType == "fail")
      .next("next").where(_.eventType == "fail")
      .within(Time.seconds(2))

    //在KeyBy之后的流中匹配出定义的pattern stream
    val patternStream = CEP.pattern(loginStream, loginFaiPattern)

    //再从 pattern stream 当中获取匹配的事件流
    val loginFailDataStream = patternStream.select(
      // 数据都保存在pattern里面了    fail,success 指的是可迭代
      (pattern: scala.collection.Map[String, Iterable[LoginEvent]]) => {
        val begin = pattern.getOrElse("begin", null).iterator.next()
        val next = pattern.getOrElse("next", null).iterator.next()
        (next.userId, begin.ip, next.ip, next.eventType)
      }
    )

    loginFailDataStream.print()
    env.execute("Login Fail Detect job")
  }
}
