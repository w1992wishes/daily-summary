package me.w1992wishes.common.util;

import org.apache.commons.lang3.time.FastDateFormat;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author w1992wishes 2018/11/8 16:02
 */
public class DateUtils {

    public static final DateTimeFormatter DF_NORMAL_NO_LINE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static final DateTimeFormatter DF_NORMAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final DateTimeFormatter DF_YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final DateTimeFormatter DF_YMD_NO_LINE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static final DateTimeFormatter DF_YMDH_NO_LINE = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private static final FastDateFormat FAST_DATE_FORMAT_NORMAL = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    private static final FastDateFormat FAST_DATE_FORMAT_YMD = FastDateFormat.getInstance("yyyy-MM-dd");

    private DateUtils() {
    }

    /**
     * 日期转字符串
     */
    public static String dateTimeToStr(LocalDateTime dateTime) {
        return dateTime.format(DF_NORMAL);
    }

    /**
     * 日期转字符串
     */
    public static String dateTimeToYMDStr(LocalDateTime dateTime) {
        return dateTime.format(DF_YMD);
    }

    /**
     * 日期转字符串
     */
    public static String dateTimeToStr(LocalDateTime dateTime, DateTimeFormatter dateTimeFormatter) {
        return dateTime.format(dateTimeFormatter);
    }

    /**
     * 字符串转日期
     */
    public static LocalDateTime strToDateTime(String timePattern) {
        return LocalDateTime.parse(timePattern, DF_NORMAL);
    }

    /**
     * 字符串转日期
     */
    public static LocalDateTime strToDateTime(String timePattern, DateTimeFormatter dateTimeFormatter) {
        return LocalDateTime.parse(timePattern, dateTimeFormatter);
    }

    public static Date strToDateNormal(String timePattern) {
        Date date = new Date();
        try {
            date = FAST_DATE_FORMAT_NORMAL.parse(timePattern);
        } catch (ParseException ex) {
            //
        }
        return date;
    }

    public static Date strToDateYMD(String timePattern) {
        Date date = new Date();
        try {
            date = FAST_DATE_FORMAT_YMD.parse(timePattern);
        } catch (ParseException ex) {
            //
        }
        return date;
    }

    public static String dateYMDToStr(Date date) {
        return FAST_DATE_FORMAT_YMD.format(date);
    }

}
