package me.w1992wishes.spark.etl.util

/**
  * @author w1992wishes 2019/1/25 19:13
  */
object AgeUtils {

  /**
    * age占用两个字节，分别表示年龄的区间和实际的年龄值，先左移8位（该8位用于表示年龄段），得到高八位，转换为十进制，用于表示真实年龄
    *
    * @param ageInfo 年龄信息
    * @return 是否是真实年龄 true，返回真实年龄， false，返回年龄段
    */
  def parseRealAge(ageInfo: Int): (Boolean, Int) =
    if (ageInfo > 255) {
      (true, Integer.valueOf(Integer.toBinaryString(ageInfo >> 8), 2))
    } else {
      (false, ageInfo)
    }
}
