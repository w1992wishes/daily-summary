package me.w1992wishes.spark.hive.intellif.param

import java.time.LocalDateTime

import me.w1992wishes.common.util.{BooleanParam, DateUtil, IntParam}

import scala.annotation.tailrec

/**
  * @author w1992wishes 2020/4/26 15:21
  */
class ArchivePersonGp2MidTransferCLParam(args: Array[String]) {

  var confName: String = "ArchivePersonGp2MidTransferTask.properties"

  var numPartitions: Int = 200

  var bizCode: String = "matrix"

  var dt: String = DateUtil.dateTimeToStr(LocalDateTime.now().minusDays(1), DateUtil.DF_YMD_NO_LINE)

  // gp 中的 create_time
  var startTime: String = _

  var endTime: String = _

  var debug: Boolean = false

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--confName") :: value :: tail =>
      confName = value
      parse(tail)

    case ("--numPartitions") :: IntParam(value) :: tail =>
      numPartitions = value
      parse(tail)

    case ("--bizCode") :: value :: tail =>
      bizCode = value
      parse(tail)

    case ("--dt") :: value :: tail =>
      dt = value
      parse(tail)

    case ("--startTime") :: value :: tail =>
      startTime = value
      parse(tail)

    case ("--endTime") :: value :: tail =>
      endTime = value
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

object ArchivePersonGp2MidTransferCLParam {
  def apply(args: Array[String]): ArchivePersonGp2MidTransferCLParam = new ArchivePersonGp2MidTransferCLParam(args)
}

