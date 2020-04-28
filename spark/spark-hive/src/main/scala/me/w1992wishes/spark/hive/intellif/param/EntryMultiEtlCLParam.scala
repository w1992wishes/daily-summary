package me.w1992wishes.spark.hive.intellif.param

import java.time.LocalDateTime

import me.w1992wishes.spark.hive.intellif.util.DateUtil

import scala.annotation.tailrec

/**
  * @author w1992wishes 2020/4/26 15:21
  */
class EntryMultiEtlCLParam(args: Array[String]) {

  var shufflePartitions: Int = 200

  var eventType: String = _

  var bizCode: String = "bigdata"

  var date: String = DateUtil.dateTimeToStr(LocalDateTime.now().minusDays(1), DateUtil.DF_YMD_NO_LINE)

  var debug: Boolean = false

  var isCoalesce: Boolean = true

  var coalescePartitions: Int = 1

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--shufflePartitions") :: IntParam(value) :: tail =>
      shufflePartitions = value
      parse(tail)

    case ("--eventType") :: value :: tail =>
      eventType = value
      parse(tail)

    case ("--bizCode") :: value :: tail =>
      bizCode = value
      parse(tail)

    case ("--date") :: value :: tail =>
      date = value
      parse(tail)

    case ("--debug") :: BooleanParam(value) :: tail =>
      debug = value
      parse(tail)

    case ("--isCoalesce") :: BooleanParam(value) :: tail =>
      isCoalesce = value
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

object EntryMultiEtlCLParam {
  def apply(args: Array[String]): EntryMultiEtlCLParam = new EntryMultiEtlCLParam(args)
}