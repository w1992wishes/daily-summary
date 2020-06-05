package me.w1992wishes.spark.hive.intellif.param

import scala.annotation.tailrec

/**
  * @author w1992wishes 2020/3/9 14:52.
  */
class EventCamera2OdlEtlCLParam(args: Array[String]) {

  var confName: String = "EventCamera2OdlEtlTask.properties"

  var bizCode: String = "bigdata"

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

object EventCamera2OdlEtlCLParam {
  def apply(args: Array[String]): EventCamera2OdlEtlCLParam = new EventCamera2OdlEtlCLParam(args)
}


