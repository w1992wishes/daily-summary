package me.w1992wishes.spark.streaming.common.config

import java.io.FileInputStream
import java.util.Properties

import me.w1992wishes.common.exception.MyRuntimeException

/**
  * 配置属性
  *
  * @author w1992wishes 2019/5/15 14:35
  */
class StreamingConfig(confName: String) extends Serializable {
  val properties = new Properties()
  properties.load(new FileInputStream(confName))

  def getString(key: String, value: String): String = {
    properties.getOrDefault(key, value).toString
  }

  def getInt(key: String, value: Int): Int = {
    getString(key, value.toString).toInt
  }

  def getFloat(key: String, value: Float): Float = {
    getString(key, value.toString).toFloat
  }

  def getBoolean(key: String, value: Boolean): Boolean = {
    getString(key, value.toString).toBoolean
  }

  def getValidProperty(key: String): String = {
    properties.getProperty(key) match {
      case _ => properties.getProperty(key)
      case null => throw new MyRuntimeException(s"$key can not be empty")
    }
  }
}

object StreamingConfig {
  def apply(confName: String): StreamingConfig = {
    new StreamingConfig(confName)
  }
}
