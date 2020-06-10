package me.w1992wishes.flink.behavior.analysis

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer

case class LoginEvent(userId: Long, ip: String, eventType: String, eventTime: Long)

/**
  * 恶意登录监控：按照用户ID分流，然后遇到登录失败的事件时将其保存在ListState中，然后设置一个定时器，2秒后触发。
  * 定时器触发时检查状态中的登录失败事件个数，如果大于等于2，那么就输出报警信息。
  *
  * @author w1992wishes 2020/6/10 15:37
  */
object LoginFailDetect {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    val loginEventStream = env.fromCollection(List(
      LoginEvent(1, "192.168.0.1", "fail", 1558430842),
      LoginEvent(1, "192.168.0.2", "fail", 1558430843),
      LoginEvent(1, "192.168.0.3", "fail", 1558430844),
      LoginEvent(2, "192.168.10.10", "success", 1558430845)
    ))
      .assignAscendingTimestamps(_.eventTime * 1000)
      .keyBy(_.userId)
      .process(new MatchFunction())

    loginEventStream.print()

    env.execute("Login Fail Detect Job")
  }

  class MatchFunction extends KeyedProcessFunction[Long, LoginEvent, LoginEvent] {
    // 定义状态变量

    lazy val loginState: ListState[LoginEvent] = getRuntimeContext.getListState(
      new ListStateDescriptor[LoginEvent]("saved login", classOf[LoginEvent]))

    override def processElement(login: LoginEvent,
                                context: KeyedProcessFunction[Long, LoginEvent,
                                  LoginEvent]#Context, out: Collector[LoginEvent]): Unit = {
      if (login.eventType == "fail") {
        loginState.add(login)
      }
      // 注册定时器，触发事件设定为2秒后
      context.timerService.registerEventTimeTimer(login.eventTime + 2 * 1000)
    }

    override def onTimer(timestamp: Long,
                         ctx: KeyedProcessFunction[Long, LoginEvent,
                           LoginEvent]#OnTimerContext, out: Collector[LoginEvent]): Unit = {

      val allLogins: ListBuffer[LoginEvent] = ListBuffer()
      import scala.collection.JavaConversions._
      for (login <- loginState.get) {
        allLogins += login
      }
      loginState.clear()

      if (allLogins.length > 1) {
        out.collect(allLogins.head)
      }
    }
  }

}
