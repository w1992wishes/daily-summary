package me.w1992wishes.spark.hive.intellif.param

import java.time.LocalDateTime

import me.w1992wishes.spark.hive.intellif.util.DateUtil

import scala.annotation.tailrec

/**
  * 程序命令行传递的参数
  *
  * @author w1992wishes 2018/11/13 17:31
  */
class BigdataEventCarCLParam(args: Array[String]) extends Serializable {

  // 是否本地模式运行
  var isLocal: Boolean = false

  var date: String = DateUtil.dateTimeToStr(LocalDateTime.now().minusDays(1), DateUtil.DF_YMD_NO_LINE)

  var bizCode: String = "bigdata"

  var coalescePartitions: Int = 1

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--isLocal") :: BooleanParam(value) :: tail =>
      isLocal = value
      parse(tail)

    case ("--date") :: value :: tail =>
      date = value
      parse(tail)

    case ("--bizCode") :: value :: tail =>
      bizCode = value
      parse(tail)

    case ("--coalescePartitions") :: IntParam(value) :: tail =>
      coalescePartitions = value
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
    System.err.println("please check param")
    // scalastyle:on println
    System.exit(exitCode)
  }
}

object BigdataEventCarCLParam {
  def apply(args: Array[String]): BigdataEventCarCLParam = new BigdataEventCarCLParam(args)
}
