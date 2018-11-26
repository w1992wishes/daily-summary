package me.w1992wishes.spark.etl.util

import java.time.LocalDateTime

/**
  * @author w1992wishes 2018/10/12 18:18
  */
object DateUtils {

  import java.time.format.DateTimeFormatter

  private[this] val df: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  private[this] val dfYMD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  /**
    * 日期转字符串
    *
    * @param dateTime
    * @return
    */
  def dateTimeToStr(dateTime: LocalDateTime) = {
    dateTime.format(df)
  }

  /**
    * 日期转字符串
    *
    * @param dateTime
    * @return
    */
  def dateTimeToYMDStr(dateTime: LocalDateTime) = {
    dateTime.format(dfYMD)
  }

  /**
    * 日期转字符串
    *
    * @param dateTime
    * @return
    */
  def dateTimeToStr(dateTime: LocalDateTime, dateTimeFormatter: DateTimeFormatter) = {
    dateTime.format(dateTimeFormatter)
  }

  /**
    * 字符串转日期
    *
    * @param timePattern
    * @return
    */
  def strToDateTime(timePattern: String) = {
    LocalDateTime.parse(timePattern, df)
  }
}
