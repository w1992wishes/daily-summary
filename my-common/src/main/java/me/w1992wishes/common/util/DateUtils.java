package me.w1992wishes.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author w1992wishes 2018/11/8 16:02
 */
public class DateUtils {

    public static final DateTimeFormatter DF_NORMAL_NO_LINE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static final DateTimeFormatter DF_NORMAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final DateTimeFormatter DF_YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final DateTimeFormatter DF_YMD_NO_LINE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static final DateTimeFormatter DF_YMDH_NO_LINE = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private DateUtils(){}

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

}
