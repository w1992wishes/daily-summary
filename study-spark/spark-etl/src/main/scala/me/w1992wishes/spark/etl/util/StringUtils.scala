package me.w1992wishes.spark.etl.util

/**
  * @author w1992wishes 2018/9/22 9:55
  */
object StringUtils {

  /**
    * 判断字符串是否是纯数字组成的串，如果是，就返回对应的数值，否则返回0
    *
    * @param str
    * @return
    */
  def strToInt(str: String): Int = {
    val regex = """([0-9]+)""".r
    val res = str match {
      case regex(num) => num
      case _ => "0"
    }
    val resInt = Integer.parseInt(res)
    resInt
  }

}
