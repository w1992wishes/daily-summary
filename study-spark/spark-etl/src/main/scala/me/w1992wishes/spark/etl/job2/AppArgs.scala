package me.w1992wishes.spark.etl.job2

import scala.annotation.tailrec

/**
  * 程序命令行传递的参数
  *
  * @author w1992wishes 2018/11/13 17:31
  */
private[core] class AppArgs (args: Array[String]) extends Serializable {

  // 设置并行的分区度
  var partitions: Int = 54

  // 预处理的起始时间
  var preprocessStartTime: String = _

  // 预处理的结束时间
  var preprocessEndTime: String = _

  // 用于过滤夜间抓拍照片的 case 类
  case class AppTime(hour: Int, min: Int, sec: Int)

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {
    case ("--preprocessStartTime") :: value :: tail =>
      preprocessStartTime = value
      parse(tail)

    case ("--preprocessEndTime") :: value :: tail =>
      preprocessEndTime = value
      parse(tail)

    case ("--partitions") :: value :: tail =>
      partitions = value.toInt
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
        "  --preprocessStartTime yyyy-MM-dd    the preprocess start time\n" +
        "  --preprocessEndTime yyyy-MM-dd    the preprocess end time"
    )
    // scalastyle:on println
    System.exit(exitCode)
  }
}
