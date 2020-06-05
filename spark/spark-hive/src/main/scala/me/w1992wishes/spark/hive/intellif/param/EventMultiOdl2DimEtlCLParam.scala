package me.w1992wishes.spark.hive.intellif.param

import java.time.LocalDateTime

import me.w1992wishes.common.util.{DateUtil, IntParam}

import scala.annotation.tailrec

/**
  * 程序命令行传递的参数
  *
  * @author w1992wishes 2018/11/13 17:31
  */
class EventMultiOdl2DimEtlCLParam(args: Array[String]) extends Serializable {

  var dt: String = DateUtil.dateTimeToStr(LocalDateTime.now().minusDays(1), DateUtil.DF_YMD_NO_LINE)

  var bizCode: String = "bigdata"

  var geoLength : Int = 12

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--dt") :: value :: tail =>
      dt = value
      parse(tail)

    case ("--bizCode") :: value :: tail =>
      bizCode = value
      parse(tail)

    case ("--geoLength") :: IntParam(value) :: tail =>
      geoLength = value
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

object EventMultiOdl2DimEtlCLParam {
  def apply(args: Array[String]): EventMultiOdl2DimEtlCLParam = new EventMultiOdl2DimEtlCLParam(args)
}


