package me.w1992wishes.spark.sql.intellif.task

import java.sql._

import me.w1992wishes.common.util.ConnectionPool
import me.w1992wishes.spark.sql.intellif.alility.DbcpSupportAbility
import me.w1992wishes.spark.sql.intellif.config.PropertiesTool

/**
  * @author w1992wishes 2019/11/25 15:01
  */
class BatchEtlTask(propertiesFactory: PropertiesTool) extends Serializable with DbcpSupportAbility {

  def createHistoryTable(table: String = "etl_history"): Unit = {
    val sql = s"CREATE TABLE IF NOT EXISTS $table (app_id varchar(255), time datetime, PRIMARY KEY (app_id))"
    var conn: Connection = null
    var st: Statement = null
    try {
      val prop = initDbcpProperties(propertiesFactory)
      conn = ConnectionPool(prop)
      st = conn.createStatement()
      st.execute(sql)
    } finally {
      ConnectionPool.closeResource(conn, st, rs = null)
    }
  }

  def getStartTime(table: String = "etl_history", appId: String = appId): Option[Timestamp] = {
    val sql = s"select time from $table where app_id = '$appId'"
    var conn: Connection = null
    var ps: PreparedStatement = null
    var rs: ResultSet = null
    try {
      val prop = initDbcpProperties(propertiesFactory)
      conn = ConnectionPool(prop)
      ps = conn.prepareStatement(sql)
      rs = ps.executeQuery()
      if (rs.next()) {
        Option(rs.getTimestamp("time"))
      } else {
        None
      }

    } finally {
      ConnectionPool.closeResource(conn, ps, rs)
    }
  }

  def updateStartTime(table: String = "etl_history", appId: String = appId, time: Timestamp): Unit = {
    if (time != null) {
      val sql = s"INSERT INTO $table (app_id, time) VALUES('$appId', ?) ON DUPLICATE KEY UPDATE app_id='$appId', time=?"
      var conn: Connection = null
      var ps: PreparedStatement = null
      try {
        val prop = initDbcpProperties(propertiesFactory)
        conn = ConnectionPool(prop)
        ps = conn.prepareStatement(sql)
        ps.setTimestamp(1, time)
        ps.setTimestamp(2, time)
        ps.execute()
      } finally {
        ConnectionPool.closeResource(conn, ps)
      }
    }
  }

  def appId: String = {
    s"${getClass.getSimpleName}_$getEventType"
  }

  def getEventType: String = {
    "person"
  }
}

