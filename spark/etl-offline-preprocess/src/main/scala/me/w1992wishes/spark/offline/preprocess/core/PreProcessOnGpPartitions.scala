package me.w1992wishes.spark.offline.preprocess.core

import java.sql.{Connection, ResultSet, Statement, Timestamp}
import java.time.{LocalDateTime, ZoneId}

import me.w1992wishes.common.util.DateUtils
import me.w1992wishes.spark.offline.preprocess.config.CommandLineArgs
import me.w1992wishes.spark.offline.preprocess.util.{ConnectionUtils, Constants}
import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.SaveMode

/**
  * 预处理任务，基于分区表实现
  *
  * @author w1992wishes 2019/3/22 15:08
  */
class PreProcessOnGpPartitions(commandLineArgs: CommandLineArgs) extends CommonPreProcess(commandLineArgs: CommandLineArgs) {

  private def partitionPostfix: String = commandLineArgs.partitionType match {
    case "d" => Constants.GP_PARTITION_STR.concat(DateUtils.dateTimeToStr(LocalDateTime.now().minusDays(1), DateUtils.DF_YMD_NO_LINE))
    case "h" => Constants.GP_PARTITION_STR.concat(DateUtils.dateTimeToStr(LocalDateTime.now().minusHours(1), DateUtils.DF_YMDH_NO_LINE))
  }

  // 预处理前的表
  override def getPreProcessTable: String =
    if (StringUtils.isNotEmpty(commandLineArgs.preProcessTable))
      commandLineArgs.preProcessTable
    else
      config.sourceTable.concat(partitionPostfix)

  // 预处理后的表
  override def getPreProcessedTable: String =
    if (StringUtils.isNotEmpty(commandLineArgs.preProcessedTable))
      commandLineArgs.preProcessedTable
    else
      config.sinkTable.concat(partitionPostfix)

  override def preProcessBefore(): Unit = {
    clearDatas()
  }

  override def preProcessPost(): Unit = {
    // wait to do something
  }

  override def getSaveMode: SaveMode = SaveMode.Append

  override def getTimeScope(startTimeStr: String, endTimeStr: String): (String, String) = {

    var conn: Connection = null
    var st: Statement = null
    var rs: ResultSet = null
    var startTime: Timestamp = null
    var endTime: Timestamp = null
    try {
      conn = ConnectionUtils.getConnection(config.sourceUrl, config.sourceUser, config.sourcePasswd)
      st = conn.createStatement()
      val sql = s"SELECT MIN(create_time) minTime, MAX(create_time) maxTime FROM $preProcessTable"
      println(s"======> query start time and end time sql -- $sql")
      rs = st.executeQuery(sql)
      if (rs.next() && rs.getTimestamp(1) != null && rs.getTimestamp(2) != null) {
        startTime = rs.getTimestamp(1)
        endTime = rs.getTimestamp(2)
      }

      // 当前日期的前一天的 0时0分0秒
      var startTimeStr: String = DateUtils.dateTimeToStr(LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0))
      // 当前日期的 0时0分0秒
      var endTimeStr: String = DateUtils.dateTimeToStr(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0))
      if (startTime != null)
        // 减一秒，防止漏掉数据
        startTimeStr = DateUtils.dateTimeToStr(LocalDateTime.ofInstant(startTime.toInstant, ZoneId.systemDefault()).minusSeconds(1))
      if (endTime != null)
        endTimeStr = DateUtils.dateTimeToStr(LocalDateTime.ofInstant(endTime.toInstant, ZoneId.systemDefault()))

      (startTimeStr, endTimeStr)
    } finally {
      ConnectionUtils.closeResource(conn, st, rs)
    }
  }

  /**
    * 清除数据
    *
    */
  private def clearDatas(): Unit = {
    var conn: Connection = null
    var st: Statement = null
    try {
      conn = ConnectionUtils.getConnection(config.sinkUrl, config.sinkUser, config.sinkPasswd)
      val sql = s"TRUNCATE TABLE $preProcessedTable"
      println(s"======> TRUNCATE TABLE sql -- $sql")

      st = conn.createStatement()
      st.executeUpdate(sql)
    } finally {
      ConnectionUtils.closeConnection(conn)
      ConnectionUtils.closeStatement(st)
    }
  }
}
