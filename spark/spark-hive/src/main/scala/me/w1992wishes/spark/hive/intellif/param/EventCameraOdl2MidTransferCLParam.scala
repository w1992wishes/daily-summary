package me.w1992wishes.spark.hive.intellif.param

import scala.annotation.tailrec

/**
  * @author w1992wishes 2020/6/5 18:12
  */
class EventCameraOdl2MidTransferCLParam(args: Array[String]) extends Serializable {

  var bizCode: String = "matrix"

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--bizCode") :: value :: tail =>
      bizCode = value
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

object EventCameraOdl2MidTransferCLParam {
  def apply(args: Array[String]): EventCameraOdl2MidTransferCLParam = new EventCameraOdl2MidTransferCLParam(args)

}
