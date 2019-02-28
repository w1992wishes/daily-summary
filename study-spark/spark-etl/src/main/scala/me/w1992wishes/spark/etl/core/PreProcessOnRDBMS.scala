package me.w1992wishes.spark.etl.core

import java.sql.{Connection, PreparedStatement}

import me.w1992wishes.spark.etl.config.CommandLineArgs
import me.w1992wishes.spark.etl.util.ConnectionUtils

/**
  * 预处理任务，基于 RDBMS 实现
  *
  * @author w1992wishes 2019/2/26 16:19
  */
class PreProcessOnRDBMS(commandLineArgs: CommandLineArgs) extends PreProcess(commandLineArgs: CommandLineArgs) {

  def clearDataByTimeRange(timeRange: (String, String), table: String, conn: Connection): Unit = {
    var pstmt: PreparedStatement = null
    try {
      val sql = s"delete from $table WHERE create_time > '${timeRange._1}' and create_time <= '${timeRange._2}'"
      println(s"======> clear sql -- $sql")

      pstmt = conn.prepareStatement(sql)
      val result = pstmt.executeUpdate()
      println(s"======> clear $result row data from $table")
    } finally {
      ConnectionUtils.closeStatement(pstmt)
    }
  }

}
