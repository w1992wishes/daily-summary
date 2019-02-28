package me.w1992wishes.spark.etl.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
  * @author w1992wishes 2018/10/12 18:18
  */
object DateUtils {

  val dfNoLine: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

  val dfDefault: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  val dfYMD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  /**
    * 日期转字符串
    *
    * @param dateTime 日期
    * @return
    */
  def dateTimeToStr(dateTime: LocalDateTime): String = {
    dateTime.format(dfDefault)
  }

  /**
    * 日期转字符串
    *
    * @param dateTime 日期
    * @return
    */
  def dateTimeToYMDStr(dateTime: LocalDateTime): String = {
    dateTime.format(dfYMD)
  }

  /**
    * 日期转字符串
    *
    * @param dateTime 日期
    * @return
    */
  def dateTimeToStr(dateTime: LocalDateTime, dateTimeFormatter: DateTimeFormatter): String = {
    dateTime.format(dateTimeFormatter)
  }

  /**
    * 字符串转日期
    *
    * @param timePattern 时间字符串
    * @return
    */
  def strToDateTime(timePattern: String): LocalDateTime = {
    LocalDateTime.parse(timePattern, dfDefault)
  }
}
