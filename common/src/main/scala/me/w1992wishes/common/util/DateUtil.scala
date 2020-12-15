package me.w1992wishes.common.util

import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.{ChronoField, ChronoUnit}
import java.util.{Calendar, Date}

import org.apache.commons.lang.time.DateFormatUtils

/**
  * @author w1992wishes 2019/11/25 19:40
  */
object DateUtil {

  val DF_NORMAL_NO_LINE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

  val DF_NORMAL: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  val DF_YMD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  val DF_YMD_DEFAULT: DateTimeFormatter = new DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd[[ HH][:mm][:ss]]")
    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
    .toFormatter

  val DF_YMD_NO_LINE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  val DF_YMD_NO_LINE_DEFAULT: DateTimeFormatter = new DateTimeFormatterBuilder()
    .appendPattern("yyyyMMdd[[HH][mm][ss]]")
    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
    .toFormatter

  val DF_YMDH_NO_LINE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHH")

  val DAY_PATTERN: String = "yyyy-MM-dd"
  val DATETIME_PATTERN: String = "yyyy-MM-dd HH:mm:ss"
  val DATETIME_MS_PATTERN: String = "yyyy-MM-dd HH:mm:ss.SSS"

  private val FAST_DATE_FORMAT_YMD = org.apache.commons.lang.time.FastDateFormat.getInstance(DAY_PATTERN)

  /**
    * Parse date by 'yyyy-MM-dd' pattern
    */
  def parseByDayPattern(str: String): Date = parseDate(str, DAY_PATTERN)

  /**
    * Parse date by 'yyyy-MM-dd HH:mm:ss' pattern
    */
  def parseByDateTimePattern(str: String): Date = parseDate(str, DATETIME_PATTERN)

  def parseDate(str: String, pattern: String): Date = {
    org.apache.commons.lang.time.DateUtils.parseDate(str, Array(pattern))
  }

  /**
    * Format date into string
    */
  def formatDate(date: Date, pattern: String): String = DateFormatUtils.format(date, pattern)

  /**
    * Format date by 'yyyy-MM-dd' pattern
    */
  def formatByDayPattern(date: Date): String = if (date != null) DateFormatUtils.format(date, DAY_PATTERN) else null

  /**
    * Format date by 'yyyy-MM-dd HH:mm:ss' pattern
    */
  def formatByDateTimePattern(date: Date): String = DateFormatUtils.format(date, DATETIME_PATTERN)

  /**
    * Get current day using format date by 'yyyy-MM-dd HH:mm:ss' pattern
    */
  def getCurrentDayByDayPattern: String = {
    val cal = Calendar.getInstance
    formatByDayPattern(cal.getTime)
  }

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

  def formatDateStr(timePattern: String, inputFormatter: DateTimeFormatter, outputFormatter: DateTimeFormatter): String = {
    LocalDateTime.parse(timePattern, inputFormatter).format(outputFormatter)
  }

  def now(formatter: DateTimeFormatter = DF_NORMAL): String = {
    LocalDateTime.now.format(formatter)
  }

  def today(formatter: DateTimeFormatter = DF_NORMAL): String = {
    LocalDateTime.now.truncatedTo(ChronoUnit.DAYS).format(formatter)
  }

  def yesterday(formatter: DateTimeFormatter = DF_NORMAL): String = {
    LocalDateTime.now.truncatedTo(ChronoUnit.DAYS).minusDays(1).format(formatter)
  }

  def tomorrow(formatter: DateTimeFormatter = DF_NORMAL): String = {
    LocalDateTime.now.truncatedTo(ChronoUnit.DAYS).plusDays(1).format(formatter)
  }

  def dateAgo(dateTime: String, days: Int, inFormatter: DateTimeFormatter = DF_NORMAL, outFormatter: DateTimeFormatter = DF_YMD_NO_LINE): String = {
    strToDateTime(dateTime, inFormatter).minusDays(days).format(outFormatter)
  }

  /**
    * 日期加上时分秒
    */
  def dateEndSecond(dateTime : String, sourceFormatter : DateTimeFormatter = DF_YMD_NO_LINE): String ={
    transform(dateTime, sourceFormatter, DF_YMD) + " 23:59:59"
  }

  /**
    * 时间格式转换
    */
  def transform(dateTime : String, inFormat : DateTimeFormatter, outFormat : DateTimeFormatter): String ={
    dateTimeToStr(strToDateTime(dateTime, inFormat), outFormat)
  }

  def dateYMDToStr(date: Date): String = FAST_DATE_FORMAT_YMD.format(date)
}
