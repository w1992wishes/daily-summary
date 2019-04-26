package me.w1992wishes.spark.streaming.config

import me.w1992wishes.my.common.util.{BooleanParam, IntParam}

import scala.annotation.tailrec

/**
  * 程序命令行传递的参数
  *
  * @author w1992wishes 2018/11/13 17:31
  */
class CommandLineArgs(args: Array[String]) extends Serializable {

  // 是否本地模式运行
  var isLocal: Boolean = false

  // 设置并行的分区度
  var partitions: Int = 54

  // 预处理后的表
  var preProcessedTable: String =_

  // 流式批处理时间间隔
  var batchDuration: Int = 10

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--isLocal") :: BooleanParam(value) :: tail =>
      isLocal = value
      parse(tail)

    case ("--partitions") :: IntParam(value) :: tail =>
      partitions = value
      parse(tail)

    case ("--preProcessedTable") :: value :: tail =>
      preProcessedTable = value
      parse(tail)

    case ("--batchDuration") :: IntParam(value) :: tail =>
      batchDuration = value
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
        "  --isLocal true|false    master url 是否是 local，true 多用作调试\n" +
        "  --partitions num    分区数\n" +
        "  --preProcessedTable tableName    预处理后的数据（如果不设置，默认从配置中读取）\n" +
        "  --batchDuration int    流式批处理时间间隔"
    )
    // scalastyle:on println
    System.exit(exitCode)
  }
}
