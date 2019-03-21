package me.w1992wishes.spark.etl.core

import java.sql.{Connection, Statement}

import me.w1992wishes.spark.etl.config.CommandLineArgs
import me.w1992wishes.spark.etl.util.ConnectionUtils

/**
  * 预处理任务，基于 RDBMS 实现
  *
  * @author w1992wishes 2019/2/26 16:19
  */
class PreProcessOnRDBMS(commandLineArgs: CommandLineArgs) extends CommonPreProcess(commandLineArgs: CommandLineArgs) {

  override def preProcessBefore(): Unit = {
    // 2.先清除数据，防止数据重复
    clearDatas(startTimeStr, endTimeStr)
  }

  override def preProcessPost(): Unit = {
    // wait to do something
  }

  /**
    * 清除数据
    *
    * @param timeRange 起始时间 Tupple
    */
  private def clearDatas(timeRange: (String, String)): Unit = {
    var conn: Connection = null
    var st: Statement = null
    try {
      conn = ConnectionUtils.getConnection(config.dbUrl, config.dbUser, config.dbPasswd)
      try {
        val sql = s"delete from ${config.sinkTable} WHERE create_time > '${timeRange._1}' and create_time <= '${timeRange._2}'"
        println(s"======> clear sql -- $sql")

        st = conn.createStatement()
        val result = st.executeUpdate(sql)
        println(s"======> clear $result row data from ${config.sinkTable}")
      } finally {
        ConnectionUtils.closeConnection(conn)
        ConnectionUtils.closeStatement(st)
      }
    } finally {
      ConnectionUtils.closeConnection(conn)
    }
  }

}
