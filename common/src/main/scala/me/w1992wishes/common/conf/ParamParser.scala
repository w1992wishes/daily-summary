package me.w1992wishes.common.conf

import java.util.Properties

class ParamParser(args : Array[String]) {
  private val props:java.util.Properties = new java.util.Properties()
  parse(args)
  /**
    * 根据传入的参数转换properties对象
    * @param args 输入的参数
    */
  def parse(args:Array[String]): Unit ={
    for(index <- args.indices by 2){
      props.put(args(index), args(index + 1))
    }
  }

  /**
    * 获取全部配置
    * @return
    */
  def getConf: Properties = props

  /**
    * 获取指定配置
    * @param key
    * @return
    */
  def getConf(key:String):String = {
    props.getProperty(key)
  }

  /**
    * 设置参数
    * @param key
    * @param value
    * @return
    */
  def setConf(key:String,value:String): AnyRef ={
    props.put(key,value)
  }

  /**
    * 设置多个参数
    * @param map
    */
  def setConf(map:java.util.Map[String,String]): Unit ={
    props.putAll(map)
  }

}


object ParamParser {
  def apply(args:Array[String]): ParamParser = new ParamParser(args.map(_.replace("#REPLACE_IT_WITH_SPACES#", " ")))

}
