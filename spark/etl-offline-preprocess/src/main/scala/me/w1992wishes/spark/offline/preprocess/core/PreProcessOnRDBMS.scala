package me.w1992wishes.spark.offline.preprocess.core

import java.sql.{Connection, ResultSet, Statement, Timestamp}
import java.time.{LocalDateTime, ZoneId}

import me.w1992wishes.common.util.DateUtils
import me.w1992wishes.spark.offline.preprocess.config.CommandLineArgs
import me.w1992wishes.spark.offline.preprocess.util.ConnectionUtils
import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.SaveMode

/**
  * 预处理任务，基于 RDBMS 实现
  *
  * @author w1992wishes 2019/2/26 16:19
  */
class PreProcessOnRDBMS(commandLineArgs: CommandLineArgs) extends CommonPreProcess(commandLineArgs: CommandLineArgs) {

  override def preProcessBefore(): Unit = {
    // 2.先清除数据，防止数据重复
    //clearDatas(startTimeStr, endTimeStr)
  }

  override def preProcessPost(): Unit = {
    // wait to do something
  }

  override def getSaveMode: SaveMode = SaveMode.Append

  /**
    * 获取预处理开始时间和结束时间
    *
    * @param startTimeStr 开始时间字符 yyyyMMddHHmmss
    * @param endTimeStr   结束时间字符 yyyyMMddHHmmss
    * @return
    */
  def getTimeScope(startTimeStr: String, endTimeStr: String): (String, String) = {
    val start =
      if (StringUtils.isEmpty(startTimeStr)) getStartTime else DateUtils.dateTimeToStr(LocalDateTime.parse(startTimeStr, DateUtils.DF_NORMAL_NO_LINE))
    val end =
      if (StringUtils.isEmpty(endTimeStr)) getEndTime else DateUtils.dateTimeToStr(LocalDateTime.parse(endTimeStr, DateUtils.DF_NORMAL_NO_LINE))
    (start, end)
  }

  /**
    * 获取开始时间
    *
    * @return
    */
  private def getStartTime: String = {
    var conn: Connection = null
    var st: Statement = null
    var rs: ResultSet = null
    var startTime: Timestamp = null
    var startLocalTime: LocalDateTime = null
    try {
      val sql = s"SELECT MAX(create_time) FROM $preProcessedTable"
      println(s"======> query start time sql -- $sql")
      conn = ConnectionUtils.getConnection(config.sinkUrl, config.sinkUser, config.sinkPasswd)
      st = conn.createStatement()
      rs = st.executeQuery(sql)
      if (rs.next() && rs.getTimestamp(1) != null) {
        startTime = rs.getTimestamp(1)
      }
      startLocalTime = if (startTime != null) LocalDateTime.ofInstant(startTime.toInstant, ZoneId.systemDefault()) else getSourceMinTime
      DateUtils.dateTimeToStr(startLocalTime)
    } finally {
      ConnectionUtils.closeResource(conn, st, rs)
    }
  }

  /**
    * 获取结束时间
    *
    * @return
    */
  private def getEndTime: String = {
    var conn: Connection = null
    var st: Statement = null
    var rs: ResultSet = null
    var startTime: Timestamp = null
    try {
      conn = ConnectionUtils.getConnection(config.sourceUrl, config.sourceUser, config.sourcePasswd)
      st = conn.createStatement()
      val sql = s"SELECT MAX(create_time) FROM $preProcessTable"
      println(s"======> query end time sql -- $sql")
      rs = st.executeQuery(sql)
      if (rs.next() && rs.getTimestamp(1) != null) {
        startTime = rs.getTimestamp(1)
      }
      if (startTime != null)
        DateUtils.dateTimeToStr(LocalDateTime.ofInstant(startTime.toInstant, ZoneId.systemDefault()))
      else
      // 当前日期的 0时0分0秒
        DateUtils.dateTimeToStr(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0))
    } finally {
      ConnectionUtils.closeResource(conn, st, rs)
    }
  }

  /**
    * 获取数据源最小时间
    *
    * @return
    */
  private def getSourceMinTime: LocalDateTime = {
    var conn: Connection = null
    var st: Statement = null
    var rs: ResultSet = null
    var minTime: Timestamp = null
    try {
      val sql = s"SELECT MIN(create_time) FROM $preProcessTable"
      println(s"======> query min source time sql --- $sql")
      conn = ConnectionUtils.getConnection(config.sourceUrl, config.sourceUser, config.sourcePasswd)
      st = conn.createStatement()
      rs = st.executeQuery(sql)
      if (rs.next() && rs.getTimestamp(1) != null) {
        minTime = rs.getTimestamp(1)
      }
      if (minTime != null)
      // 减去一秒，避免漏掉最开始的数据
        LocalDateTime.ofInstant(minTime.toInstant, ZoneId.systemDefault()).minusSeconds(1)
      else {
        // 当前日期的前一天的 0时0分0秒
        LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0)
      }
    } finally {
      ConnectionUtils.closeResource(conn, st, rs)
    }
  }

  /*  /**
      * 清除数据
      *
      * @param timeRange 起始时间 Tupple
      */
    private def clearDatas(timeRange: (String, String)): Unit = {
      var conn: Connection = null
      var st: Statement = null
      try {
        conn = ConnectionUtils.getConnection(config.sinkUrl, config.sinkUser, config.sinkPasswd)
        val sql = s"delete from $preProcessedTable WHERE create_time > '${timeRange._1}' and create_time <= '${timeRange._2}'"
        println(s"======> clear sql -- $sql")

        st = conn.createStatement()
        val result = st.executeUpdate(sql)
        println(s"======> clear $result row data from $preProcessedTable")
      } finally {
        ConnectionUtils.closeConnection(conn)
        ConnectionUtils.closeStatement(st)
      }
    }*/

}
