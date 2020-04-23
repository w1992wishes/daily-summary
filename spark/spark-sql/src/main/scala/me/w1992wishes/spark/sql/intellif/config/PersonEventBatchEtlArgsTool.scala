package me.w1992wishes.spark.sql.intellif.config

import me.w1992wishes.common.util.IntParam
import scala.annotation.tailrec

/**
  * 程序命令行传递的参数
  *
  * @author w1992wishes 2018/11/13 17:31
  */
class PersonEventBatchEtlArgsTool(args: Array[String]) extends Serializable {

  var confName: String = "PersonEventEtlTask.properties"

  var shufflePartitions: Int = 200

  // 设置并行的分区度
  var partitions: Int = 54

  var eventType: String = _

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

    case ("--eventType") :: value :: tail =>
      eventType = value
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
        "  --isLocal true|false    master url 是否是 local，true 多用作调试\n" +
        "  --partitions num    分区数"
    )
    // scalastyle:on println
    System.exit(exitCode)
  }
}
