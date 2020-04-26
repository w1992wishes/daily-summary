package me.w1992wishes.spark.hive.intellif.util

import java.sql.{Connection, ResultSet, SQLException, Statement}

object DataSourceUtil {

  /**
    * 释放连接 Connection
    *
    * @param conn 数据库连接
    */
  def closeConnection(conn: Connection): Unit = {
    if (conn != null) try
      conn.close()
    catch {
      case e: SQLException => println("======> close db connection failure ", e)
    }
  }

  /**
    * 释放语句执行者 Statement
    *
    * @param st Statement
    */
  def closeStatement(st: Statement): Unit = {
    if (st != null) try
      st.close()
    catch {
      case e: SQLException => println("======> close db statement failure ", e)
    }
  }

  /**
    * 释放 ResultSet
    *
    * @param rs ResultSet
    */
  def closeResultSet(rs: ResultSet): Unit = {
    if (rs != null) try
      rs.close()
    catch {
      case e: SQLException => println("======> close db ResultSet failure ", e)
    }
  }

  def closeResource(conn: Connection, st: Statement, rs: ResultSet = null): Unit = {
    closeConnection(conn)
    closeStatement(st)
    closeResultSet(rs)
  }
}