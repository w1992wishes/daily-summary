package me.w1992wishes.spark.offline.preprocess.config

import me.w1992wishes.spark.offline.preprocess.util.{BooleanParam, IntParam}

import scala.annotation.tailrec

/**
  * 程序命令行传递的参数
  *
  * @author w1992wishes 2018/11/13 17:31
  */
class CommandLineArgs(args: Array[String]) extends Serializable {

  // 是否本地模式运行
  var isLocal: Boolean = false

  // 预处理的实现： 1 PreProcessOnRDBMS； 2 PreProcessOnPartitions
  var preProcessImpl: Int = 1

  // 数据表分区的类型 d 按天分区， h 按小时分区
  var partitionType: String = "d"

  // 设置并行的分区度
  var partitions: Int = 54

  // 预处理的起始时间
  var preProcessStartTime: String = _

  // 预处理的结束时间
  var preProcessEndTime: String = _

  // 每个分区最小应处理的时间跨度，单位分钟
  var minutesPerPartition: Int = 5

  // 预处理前的表
  var preProcessTable: String =_

  // 预处理后的表
  var preProcessedTable: String =_

  // 时间防护，开启后最多取 运行时间的前一天0时0分0秒至运行时间 内的数据，可以避免大数据量一次预处理大量的数据
  var timeProtection: Boolean = true

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--isLocal") :: BooleanParam(value) :: tail =>
      isLocal = value
      parse(tail)

    case ("--preProcessImpl") :: IntParam(value) :: tail =>
      preProcessImpl = value
      parse(tail)

    case ("--partitionType") :: value :: tail =>
      partitionType = value
      parse(tail)

    case ("--preProcessStartTime") :: value :: tail =>
      preProcessStartTime = value
      parse(tail)

    case ("--preProcessEndTime") :: value :: tail =>
      preProcessEndTime = value
      parse(tail)

    case ("--partitions") :: IntParam(value) :: tail =>
      partitions = value
      parse(tail)

    case ("--preProcessTable") :: value :: tail =>
      preProcessTable = value
      parse(tail)

    case ("--preProcessedTable") :: value :: tail =>
      preProcessedTable = value
      parse(tail)

    case ("--timeProtection") :: BooleanParam(value) :: tail =>
      timeProtection = value
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
        "  --isLocal true|false    master url 是否是 local，true 多用作调试\n" +
        "  --preProcessImpl int    预处理的实现, 1 PreProcessOnRDBMS, 2 PreProcessOnPartitions，默认是 1  \n" +
        "  --partitionType d|h    d 表示按天分区   \n" +
        "  --partitions num    分区数\n" +
        "  --preProcessStartTime yyyyMMddHHmmss    预处理开始时间（如果不设置，默认取 preProcessedTable 最大时间）\n" +
        "  --preProcessEndTime yyyyMMddHHmmss    预处理结束时间（如果不设置，默认取 preProcessTable 最大时间）\n" +
        "  --minutesPerPartition num    每个分区最小的时间跨度，防止小数据量多分区，单位分钟，默认为 5\n" +
        "  --preProcessTable tableName    预处理数据源表（如果不设置，默认从配置中读取）\n" +
        "  --preProcessedTable tableName    预处理后的数据（如果不设置，默认从配置中读取）\n" +
        "  --timeProtection true|false    时间防护，开启后最多取 运行时间的前一天0时0分0秒至运行时间 内的数据，可以避免大数据量一次预处理大量的数据"
    )
    // scalastyle:on println
    System.exit(exitCode)
  }
}

