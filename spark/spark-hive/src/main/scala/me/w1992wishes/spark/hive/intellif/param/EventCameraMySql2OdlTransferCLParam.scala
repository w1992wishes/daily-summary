package me.w1992wishes.spark.hive.intellif.param

import scala.annotation.tailrec

/**
  * @author w1992wishes 2020/3/9 14:52.
  */
class EventCameraMySql2OdlTransferCLParam(args: Array[String]) {

  var confName: String = "EventCameraMySql2OdlTransferTask.properties"

  var bizCode: String = "matrix"

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--confName") :: value :: tail =>
      confName = value
      parse(tail)

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

object EventCameraMySql2OdlTransferCLParam {
  def apply(args: Array[String]): EventCameraMySql2OdlTransferCLParam = new EventCameraMySql2OdlTransferCLParam(args)

}
