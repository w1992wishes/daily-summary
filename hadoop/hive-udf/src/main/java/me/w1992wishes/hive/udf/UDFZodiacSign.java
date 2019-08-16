package me.w1992wishes.hive.udf;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDF;
import org.apache.hadoop.hive.ql.udf.UDFType;
import org.joda.time.DateTime;

import java.util.Date;

/**
 * @author w1992wishes 2019/8/14 13:48
 */
@UDFType
@Description(
        name = "zodiac",
        value = "_FUNC_ (date) - " +
                " from the input date string " +
                " or separate month and day arguments, \n" +
                " returns the sign of the Zodiac.",
        extended = "Example :\n" +
                "> SELECT _FUNC_ (date_string) FROM src;\n" +
                "> SELECT _FUNC_ (month, day) FROM src;")
public class UDFZodiacSign extends UDF {

    private static final String ERROR_DATE_OF_MONTH = "invalid date of specify month";

    private static final String ERROR_MONTH_ARGS = "invalid argument of month";

    private static final String ERROR_DATE_STRING = "invalid date format";

    public String evaluate(Date bday) {
        return this.evaluate(bday.getMonth() + 1, bday.getDate());
    }

    public String evaluate(String dateString) {
        DateTime dateTime;
        try {
            dateTime = new DateTime(dateString);
        } catch (Exception e) {
            return ERROR_DATE_STRING;
        }
        return this.evaluate(dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
    }

    public String evaluate(Integer month, Integer day) {

        switch (month) {
            //判断是几月
            case 1:
                //判断是当前月的哪一段时间；然后就可以得到星座了；下面代码都一样的
                if (day > 0 && day < 20) {
                    return "魔蝎座";
                } else if (day < 32) {
                    return "水瓶座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 2:
                if (day > 0 && day < 19) {
                    return "水瓶座";
                } else if (day < 29) {
                    return "双鱼座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 3:
                if (day > 0 && day < 21) {
                    return "双鱼座";
                } else if (day < 32) {
                    return "白羊座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 4:
                if (day > 0 && day < 20) {
                    return "白羊座";
                } else if (day < 31) {
                    return "金牛座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 5:
                if (day > 0 && day < 21) {
                    return "金牛座";
                } else if (day < 32) {
                    return "双子座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 6:
                if (day > 0 && day < 22) {
                    return "双子座";
                } else if (day < 31) {
                    return "巨蟹座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 7:
                if (day > 0 && day < 23) {
                    return "巨蟹座";
                } else if (day < 32) {
                    return "狮子座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 8:
                if (day > 0 && day < 23) {
                    return "狮子座";
                } else if (day < 32) {
                    return "处女座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 9:
                if (day > 0 && day < 23) {
                    return "处女座";
                } else if (day < 31) {
                    return "天平座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 10:
                if (day > 0 && day < 24) {
                    return "天平座";
                } else if (day < 32) {
                    return "天蝎座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 11:
                if (day > 0 && day < 23) {
                    return "天蝎座";
                } else if (day < 31) {
                    return "射手座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 12:
                if (day > 0 && day < 22) {
                    return "射手座";
                } else if (day < 32) {
                    return "摩羯座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            default:
                return ERROR_MONTH_ARGS;
        }

    }

}
