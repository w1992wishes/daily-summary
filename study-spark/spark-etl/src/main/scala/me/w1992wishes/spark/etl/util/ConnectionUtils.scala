package me.w1992wishes.spark.etl.util

import java.sql._

/**
  * 数据库连接 工具类
  *
  * @author w1992wishes 2018/10/17 15:37
  */
object ConnectionUtils {

  /**
    * 获取连接
    *
    * @param url db url
    * @param username user name
    * @param password passwd
    * @throws SQLException sql exception
    * @return
    */
  @throws[SQLException]
  def getConnection(url: String, username: String, password: String): Connection = DriverManager.getConnection(url, username, password)

  /**
    * 释放资源
    *
    * @param conn 数据库连接
    * @param st Statement
    */
  def closeResource(conn: Connection, st: Statement, rs: ResultSet): Unit = {
    closeStatement(st)
    closeConnection(conn)
    closeResultSet(rs)
  }

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
}
