package me.w1992wishes.common.conf

import com.typesafe.config.{Config, ConfigFactory}

/**
 * 每种类型的分析任务对应一个配置文件，一个ConfigArgs对象，配置隔离，避免互相影响，每个分析任务的配置文件共同依赖父配置文件
 */
trait ConfigArgsGlobal {
  val config: Config = ConfigFactory.load("application-global.conf") // 父配置文件

  //全局开始时间，结束时间
  val startTimeGlobal: String = config.getString("startTime")
  val endTimeGlobal: String = config.getString("endTime")

}