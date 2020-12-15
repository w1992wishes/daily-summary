package me.w1992wishes.common.enhance

import org.apache.commons.lang.StringUtils

/**
  * @author w1992wishes 2020/7/24 14:58
  */
object StrEnhance {

  implicit class StrUtil(str: String) {
    def replaceNotNull(oldStr: String, newStr: String): String = {
      if (StringUtils.isNotEmpty(str) && StringUtils.isNotEmpty(oldStr) && StringUtils.isNotEmpty(newStr))
        str.replace(oldStr, newStr)
      else
      str
    }

    def replaceLower(oldStr: String, newStr: String): String = {
      if (StringUtils.isNotEmpty(str) && StringUtils.isNotEmpty(oldStr) && StringUtils.isNotEmpty(newStr))
        str.replace(oldStr, newStr.toLowerCase())
      else
        str
    }
  }

  def main(args: Array[String]): Unit = {
    println(StrUtil("").replaceNotNull("a", "b"))
    println(StrUtil("a").replaceNotNull("a", "b"))
    println(StrUtil("a").replaceNotNull("a", ""))
  }
}