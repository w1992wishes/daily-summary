package me.w1992wishes.spark.streaming.config

import me.w1992wishes.common.util.IntParam

import scala.annotation.tailrec

/**
  * 命令参数
  *
  * @author w1992wishes 2019/5/15 14:35
  */
abstract class TaskArguments(args: List[String]) extends Serializable {

  var partitions: Int = 12

  var batchDuration: Int = 10

  var confName: String = "config.properties"

  parseCommonArguments(args)

  @tailrec
  private def parseCommonArguments(args: List[String]): Unit = args match {

    // 按照顺序第一个参数
    case ("--batchDuration") :: IntParam(value) :: tail =>
      batchDuration = value
      parseCommonArguments(tail)
    // 按照顺序第二个参数
    case ("--partitions") :: IntParam(value) :: tail =>
      partitions = value
      parseCommonArguments(tail)
    // 按照顺序第三个参数
    case ("--confName") :: value :: tail =>
      confName = value
      parseCustomArguments(tail)

    case Nil => // No-op
    case _ =>

      printUsageAndExit(1)
  }

  def printUsageAndExit(exitCode: Int): Unit = {
    System.err.println(
      commonUsageInfo()
    )
  }

  def parseCustomArguments(args: List[String]): Unit

  /**
    * Print usage and exit JVM with the given exit code.
    */
  def commonUsageInfo(): String = {
    "Usage: [options]\n" +
      "--batchDuration int    流式批处理时间间隔，必须是第一个参数\n" +
      "--partitions num    分区数，必须是第二个参数\n" +
      "--confName string    配置文件名字，必须是第三个参数\n"
  }
}

