package me.w1992wishes.spark.hive.intellif.param

import scala.annotation.tailrec

/**
  * @author w1992wishes 2020/3/9 14:52.
  */
class EventCameraOdl2DimEtlCLParam(args: Array[String]) {

  var bizCode: String = "bigdata"

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

object EventCameraOdl2DimEtlCLParam {
  def apply(args: Array[String]): EventCameraOdl2DimEtlCLParam = new EventCameraOdl2DimEtlCLParam(args)

}
