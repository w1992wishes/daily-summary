package me.w1992wishes.spark.etl.util

import java.sql._

import com.typesafe.scalalogging.Logger

/**
  * 数据库连接 工具类
  *
  * @author w1992wishes 2018/10/17 15:37
  */
object ConnectionUtils {

  private[this] val LOG = Logger(this.getClass)

  /**
    * 获取连接
    *
    * @param url
    * @param username
    * @param password
    * @throws SQLException
    * @return
    */
  @throws[SQLException]
  def getConnection(url: String, username: String, password: String): Connection = DriverManager.getConnection(url, username, password)

  /**
    * 释放资源
    *
    * @param conn
    * @param st
    */
  def closeResource(conn: Connection, st: Statement, rs: ResultSet): Unit = {
    closeStatement(st)
    closeConnection(conn)
    closeResultSet(rs)
  }

  /**
    * 释放连接 Connection
    *
    * @param conn
    */
  def closeConnection(conn: Connection): Unit = {
    if (conn != null) try
      conn.close()
    catch {
      case e: SQLException => LOG.error("======> close db connection failure ", e)
    }
  }

  /**
    * 释放语句执行者 Statement
    *
    * @param st
    */
  def closeStatement(st: Statement): Unit = {
    if (st != null) try
      st.close()
    catch {
      case e: SQLException => LOG.error("======> close db statement failure ", e)
    }
  }

  /**
    * 释放 ResultSet
    *
    * @param rs
    */
  def closeResultSet(rs: ResultSet): Unit = {
    if (rs != null) try
      rs.close()
    catch {
      case e: SQLException => LOG.error("======> close db ResultSet failure ", e)
    }
  }
}
