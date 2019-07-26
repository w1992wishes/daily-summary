package me.w1992wishes.spark.offline.preprocess.config

import me.w1992wishes.common.util.{BooleanParam, IntParam}
import me.w1992wishes.common.util.IntParam

import scala.annotation.tailrec

/**
  * 程序命令行传递的参数
  *
  * @author w1992wishes 2018/11/13 17:31
  */
class CommandLineArgs(args: Array[String]) extends Serializable {

  var confName: String = "offlinePreProcess.properties"

  // 是否本地模式运行
  var isLocal: Boolean = false

  // 数据表分区的类型 d 按天分区， h 按小时分区
  var partitionType: String = "d"

  // 设置并行的分区度
  var partitions: Int = 54

  // 预处理的起始时间
  var preProcessStartTime: String = _

  // 预处理的结束时间
  var preProcessEndTime: String = _

  // 预处理前的表
  var preProcessTable: String =_

  // 预处理后的表
  var preProcessedTable: String =_

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--confName") :: value :: tail =>
      confName = value
      parse(tail)

    case ("--isLocal") :: BooleanParam(value) :: tail =>
      isLocal = value
      parse(tail)

    case ("--partitions") :: IntParam(value) :: tail =>
      partitions = value
      parse(tail)

    case ("--preProcessTable") :: value :: tail =>
      preProcessTable = value
      parse(tail)

    case ("--preProcessedTable") :: value :: tail =>
      preProcessedTable = value
      parse(tail)

    case Nil => // No-op

    case _ =>
      printUsageAndExit(1)
  }

  /**
    * Print usage and exit JVM with the given exit code.
    */
  private def printUsageAndExit(exitCode: Int) {
    // scalastyle:off println
    System.err.println(
      "Usage: [options]\n" +
        "\n" +
        "Options:\n" +
        "  --confName file    配置文件名\n" +
        "  --isLocal true|false    master url 是否是 local，true 多用作调试\n" +
        "  --partitions num    分区数\n" +
        "  --preProcessTable tableName    预处理数据源表（如果不设置，默认从配置中读取）\n" +
        "  --preProcessedTable tableName    预处理后的数据（如果不设置，默认从配置中读取）"
    )
    // scalastyle:on println
    System.exit(exitCode)
  }
}
