package me.w1992wishes.spark.hive.intellif.param

import java.time.LocalDateTime

import me.w1992wishes.common.util.{BooleanParam, DateUtil, IntParam}

import scala.annotation.tailrec

/**
  * @author w1992wishes 2020/4/26 15:21
  */
class EntryMultiDim2MidEtlCLParam(args: Array[String]) {

  var shufflePartitions: Int = 200

  var dataType: String = _

  var bizCode: String = "bigdata"

  var dt: String = DateUtil.dateTimeToStr(LocalDateTime.now().minusDays(1), DateUtil.DF_YMD_NO_LINE)

  var debug: Boolean = false

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--shufflePartitions") :: IntParam(value) :: tail =>
      shufflePartitions = value
      parse(tail)

    case ("--dataType") :: value :: tail =>
      dataType = value
      parse(tail)

    case ("--bizCode") :: value :: tail =>
      bizCode = value
      parse(tail)

    case ("--dt") :: value :: tail =>
      dt = value
      parse(tail)

    case ("--debug") :: BooleanParam(value) :: tail =>
      debug = value
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

object EntryMultiDim2MidEtlCLParam {
  def apply(args: Array[String]): EntryMultiDim2MidEtlCLParam = new EntryMultiDim2MidEtlCLParam(args)
}