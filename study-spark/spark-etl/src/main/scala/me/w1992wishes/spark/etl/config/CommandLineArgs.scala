package me.w1992wishes.spark.etl.config

import scala.annotation.tailrec

/**
  * 程序命令行传递的参数
  *
  * @author w1992wishes 2018/11/13 17:31
  */
class CommandLineArgs(args: Array[String]) extends Serializable {

  // 设置并行的分区度
  var partitions: Int = 54

  // 预处理的起始时间
  var preProcessStartTime: String = _

  // 预处理的结束时间
  var preProcessEndTime: String = _

  // 每个分区最小应处理的时间跨度，单位分钟
  var minutesPerPartition: Int = 10

  // 用于过滤夜间抓拍照片的 case 类
  case class AppTime(hour: Int, min: Int, sec: Int)

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {
    case ("--preProcessStartTime") :: value :: tail =>
      preProcessStartTime = value
      parse(tail)

    case ("--preProcessEndTime") :: value :: tail =>
      preProcessEndTime = value
      parse(tail)

    case ("--partitions") :: value :: tail =>
      partitions = value.toInt
      parse(tail)

    case ("--minutesPerPartition") :: value :: tail =>
      minutesPerPartition = value.toInt
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
        "  --partitions num    the spark partitions\n" +
        "  --preProcessStartTime yyyyMMddHHmmss    the preprocess start time\n" +
        "  --preProcessEndTime yyyyMMddHHmmss    the preprocess end time\n" +
        "  --minutesPerPartition num    minimum minutes per partition"
    )
    // scalastyle:on println
    System.exit(exitCode)
  }
}
