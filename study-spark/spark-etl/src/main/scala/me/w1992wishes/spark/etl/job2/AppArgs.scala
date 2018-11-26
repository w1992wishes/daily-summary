package me.w1992wishes.spark.etl.job2

import me.w1992wishes.spark.etl.util.{BooleanParam, IntParam}

import scala.annotation.tailrec

/**
  * 程序命令行传递的参数
  *
  * @author w1992wishes 2018/11/13 17:31
  */
private[core] class AppArgs (args: Array[String]) extends Serializable {

  // 是否过滤夜间照片
  var filterNightEnable: Boolean = false

  var startTime: AppTime = _

  var endTime: AppTime = _

  case class AppTime(hour: Int, min: Int, sec: Int)

  parse(args.toList)

  private def parseTime(timeStr: String) = {
    val timeArray = timeStr.split(":").toList
    timeArray match {
      case IntParam(value1) :: IntParam(value2) :: IntParam(value3) :: a =>
        AppTime(value1, value2, value3)
    }
  }

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--filter-night-enable") :: BooleanParam(value) :: tail =>
      filterNightEnable = value
      parse(tail)

    case ("--start-time") :: value :: tail =>
      startTime = parseTime(value)
      parse(tail)

    case ("--end-time") :: value :: tail =>
      endTime = parseTime(value)
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
        "  --filter-night-enable true|false   when true open the filter, otherwise, close \n" +
        "  --start-time HH:mm:SS   filter start time\n" +
        "  --end-time HH:mm:SS   filter end time\n"
    )
    // scalastyle:on println
    System.exit(exitCode)
  }
}
