package me.w1992wishes.spark.hive.intellif.param

import java.time.LocalDateTime

import me.w1992wishes.common.util.{BooleanParam, DateUtil}

import scala.annotation.tailrec

/**
  * @author w1992wishes 2020/6/15 19:50
  */
class EventMultiDwd2DimArchiveCLParam(args: Array[String]) extends Serializable {
  var dt: String = DateUtil.dateTimeToStr(LocalDateTime.now().minusDays(1), DateUtil.DF_YMD_NO_LINE)

  var bizCode: String = "matrix"

  var dataType: String = _

  var debug: Boolean = false

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--dt") :: value :: tail =>
      dt = value
      parse(tail)

    case ("--bizCode") :: value :: tail =>
      bizCode = value
      parse(tail)

    case ("--dataType") :: value :: tail =>
      dataType = value
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

object EventMultiDwd2DimArchiveCLParam {
  def apply(args: Array[String]): EventMultiDwd2DimArchiveCLParam = new EventMultiDwd2DimArchiveCLParam(args)
}