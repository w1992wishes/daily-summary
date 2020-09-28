package me.w1992wishes.common.util

import java.io.FileInputStream
import java.util.Properties

/**
  * 配置文件参数
  *
  * @author w1992wishes 2019/1/14 10:37
  */
class PropertiesTool(confName: String) extends Serializable{

  val properties = new Properties()
  properties.load(new FileInputStream(confName))

  def getString(key: String, value: String): String = {
    properties.getOrDefault(key, value).toString
  }

  def getString(key: String): String = {
    properties.getProperty(key)
  }

  def getInt(key: String, value: Int): Int = {
    getString(key, value.toString).toInt
  }

  def getInt(key: String): Int = {
    getString(key).toInt
  }

  def getFloat(key: String, value: Float): Float = {
    getString(key, value.toString).toFloat
  }

  def getFloat(key: String): Float = {
    getString(key).toFloat
  }

  def getBoolean(key: String, value: Boolean): Boolean = {
    getString(key, value.toString).toBoolean
  }

  def getBoolean(key: String): Boolean = {
    getString(key).toBoolean
  }

  def getValidProperty(key: String): String = {
    properties.getProperty(key) match {
      case _ => properties.getProperty(key)
      case null => throw new Exception(s"$key can not be empty")
    }
  }
}

object PropertiesTool {
  def apply(confName: String): PropertiesTool = {
    new PropertiesTool(confName)
  }

}
