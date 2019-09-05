package me.w1992wishes.spark.sql.except

import me.w1992wishes.common.util.{BooleanParam, IntParam}

import scala.annotation.tailrec

/**
  * 程序命令行传递的参数
  *
  * @author w1992wishes 2018/11/13 17:31
  */
class CommandLineArgs(args: Array[String]) extends Serializable {

  // startTime
  var archiveStartTime: String = _

  // endTime
  var archiveEndTime: String = _

  // startTime
  var archiveEventStartTime: String = _

  // endTime
  var archiveEventEndTime: String = _

  // 设置并行的分区度
  var partitions: Int = 54

  // ip
  var gpIp: String = _

  // odlTable
  var archiveTable: String = "odl_bigdata_import_personfile_5029"

  // odlTable
  var archiveEventTable: String = "dwd_bigdata_archive_event_face_5029"

  // 档案表去除档案事件表的差集后保存进入的表
  var archiveExceptTable: String = "odl_bigdata_import_personfile_5029_copy"

  // 是否本地测试
  var isLocal = false

  // 是否保存中间数据
  var isSaveTempData = false

  parse(args.toList)

  @tailrec
  private def parse(args: List[String]): Unit = args match {

    case ("--isLocal") :: BooleanParam(value) :: tail =>
      isLocal = value
      parse(tail)

    case ("--isSaveTempData") :: BooleanParam(value) :: tail =>
      isSaveTempData = value
      parse(tail)

    case ("--archiveStartTime") :: value :: tail =>
      archiveStartTime = value
      parse(tail)

    case ("--archiveEndTime") :: value :: tail =>
      archiveEndTime = value
      parse(tail)

    case ("--archiveEventStartTime") :: value :: tail =>
      archiveEventStartTime = value
      parse(tail)

    case ("--archiveEventEndTime") :: value :: tail =>
      archiveEventEndTime = value
      parse(tail)

    case ("--gpIp") :: value :: tail =>
      gpIp = value
      parse(tail)

    case ("--archiveTable") :: value :: tail =>
      archiveTable = value
      parse(tail)

    case ("--archiveEventTable") :: value :: tail =>
      archiveEventTable = value
      parse(tail)

    case ("--archiveExceptTable") :: value :: tail =>
      archiveExceptTable = value
      parse(tail)

    case ("--partitions") :: IntParam(value) :: tail =>
      partitions = value
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
        "  --archiveStartTime archiveStartTime    archiveStartTime\n" +
        "  --archiveEndTime archiveEndTime    archiveEndTime\n" +
        "  --partitions num    分区数"
    )
    // scalastyle:on println
    System.exit(exitCode)
  }
}
