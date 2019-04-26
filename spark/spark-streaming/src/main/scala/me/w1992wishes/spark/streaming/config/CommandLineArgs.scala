package me.w1992wishes.spark.streaming.config

import me.w1992wishes.my.common.util.IntParam

import scala.annotation.tailrec

/**
  * 程序命令行传递的参数
  *
  * @author w1992wishes 2018/11/13 17:31
  */
class CommandLineArgs(args: Array[String]) extends Serializable {

  var partitions: Int = 12

  var batchDuration: Int = 10

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--partitions") :: IntParam(value) :: tail =>
      partitions = value
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
        "  --partitions num    分区数\n" +
        "  --batchDuration int    流式批处理时间间隔"
    )
    // scalastyle:on println
    System.exit(exitCode)
  }
}
