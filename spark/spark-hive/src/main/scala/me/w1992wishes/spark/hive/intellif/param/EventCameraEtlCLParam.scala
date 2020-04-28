package me.w1992wishes.spark.hive.intellif.param

import scala.annotation.tailrec

/**
  * @author w1992wishes 2020/3/9 14:52.
  */
class EventCameraEtlCLParam(args: Array[String]) {

  var confName: String = "EventCameraEtlTask.properties"

  var bizCode: String = "bigdata"

  var isCoalesce: Boolean = true

  var coalescePartitions: Int = 1

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--confName") :: value :: tail =>
      confName = value
      parse(tail)

    case ("--bizCode") :: value :: tail =>
      bizCode = value
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

object EventCameraEtlCLParam {
  def apply(args: Array[String]): EventCameraEtlCLParam = new EventCameraEtlCLParam(args)

}
