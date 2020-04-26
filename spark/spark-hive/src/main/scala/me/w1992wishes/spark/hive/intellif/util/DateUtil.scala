package me.w1992wishes.spark.hive.intellif.util

import java.text.ParseException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

import org.apache.commons.lang3.time.FastDateFormat

/**
  * @author w1992wishes 2019/11/25 19:40
  */
object DateUtil {

  val DF_NORMAL_NO_LINE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

  val DF_NORMAL: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  val DF_YMD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  val DF_YMD_NO_LINE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  val DF_YMDH_NO_LINE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHH")

  private val FAST_DATE_FORMAT_NORMAL = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss")

  private val FAST_DATE_FORMAT_YMD = FastDateFormat.getInstance("yyyy-MM-dd")

  /**
    * 日期转字符串
    */
  def dateTimeToStr(dateTime: LocalDateTime): String = dateTime.format(DF_NORMAL)

  def dateTimeToYMDStr(dateTime: LocalDateTime): String = dateTime.format(DF_YMD)

  def dateTimeToStr(dateTime: LocalDateTime, dateTimeFormatter: DateTimeFormatter): String = dateTime.format(dateTimeFormatter)

  /**
    * 字符串转日期
    */
  def strToDateTime(timePattern: String): LocalDateTime = LocalDateTime.parse(timePattern, DF_NORMAL)

  def strToDateTime(timePattern: String, dateTimeFormatter: DateTimeFormatter): LocalDateTime = LocalDateTime.parse(timePattern, dateTimeFormatter)

  def strToDateNormal(timePattern: String): Date = {
    var date = new Date
    try
      date = FAST_DATE_FORMAT_NORMAL.parse(timePattern)
    catch {
      case ex: ParseException =>

      //
    }
    date
  }

  def strToDateYMD(timePattern: String): Date = {
    var date = new Date
    try
      date = FAST_DATE_FORMAT_YMD.parse(timePattern)
    catch {
      case ex: ParseException =>

    }
    date
  }

  def dateYMDToStr(date: Date): String = FAST_DATE_FORMAT_YMD.format(date)

}
