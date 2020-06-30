package me.w1992wishes.spark.hive.intellif.param

import java.time.LocalDateTime
import java.util.Properties

import me.w1992wishes.common.util.DateUtil

/**
  * @author w1992wishes 2020/6/19 11:28
  */
class CommandLineParam(args: Array[String]) extends Serializable {

  private val props: java.util.Properties = new java.util.Properties()
  parse(args)

  /**
    * 根据传入的参数转换properties对象
    *
    * @param args 输入的参数
    */
  def parse(args: Array[String]): Unit = {
    for (index <- args.indices by 2) {
      props.put(args(index), args(index + 1))
    }
  }

  /**
    * 获取全部配置
    *
    * @return
    */
  def getConf: Properties = props

  /**
    * 获取指定配置
    */
  def getConf(key: String): String = {
    props.getProperty(key)
  }

  /**
    * 获取指定配置
    */
  def getConf(key: String, value: String): String = {
    props.getProperty(key, value)
  }

  /**
    * 设置参数
    */
  def setConf(key: String, value: String): AnyRef = {
    props.put(key, value)
  }

  /**
    * 设置多个参数
    */
  def setConf(map: java.util.Map[String, String]): Unit = {
    props.putAll(map)
  }

  def getBizCode: String = {
    getConf(CommandLineParamOption.BIZ_CODE, "matrix")
  }

  def getConfName: String = {
    getConf(CommandLineParamOption.CONF_NAME, "matrix-static-archive.properties")
  }

  def getDt: String = {
    getConf(CommandLineParamOption.DT, DateUtil.dateTimeToStr(LocalDateTime.now().minusDays(1), DateUtil.DF_YMD_NO_LINE))
  }

  def getDataType: String = {
    getConf(CommandLineParamOption.DATA_TYPE)
  }
}

object CommandLineParam {
  def apply(args: Array[String]): CommandLineParam = new CommandLineParam(args)
}
