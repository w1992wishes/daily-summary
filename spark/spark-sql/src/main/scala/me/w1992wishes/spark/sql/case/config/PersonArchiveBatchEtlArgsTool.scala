package me.w1992wishes.spark.sql.`case`.config

import me.w1992wishes.common.util.IntParam

import scala.annotation.tailrec

/**
  * @author w1992wishes 2019/11/26 15:54
  */
class PersonArchiveBatchEtlArgsTool(args: Array[String]) {

  var confName: String = "bigdata-track-person-archive-task-v1.4.0.properties"

  var partitions: Int = 54

  var shufflePartitions: Int = 200

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--confName") :: value :: tail =>
      confName = value
      parse(tail)

    case ("--partitions") :: IntParam(value) :: tail =>
      partitions = value
      parse(tail)

    case ("--shufflePartitions") :: IntParam(value) :: tail =>
      shufflePartitions = value
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
    System.err.println(
      "Usage: [options]\n" +
        "\n" +
        "Options:\n" +
        "  --confName file    配置文件名\n" +
        "  --partitions num    分区数"
    )
    // scalastyle:on println
    System.exit(exitCode)
  }

}
