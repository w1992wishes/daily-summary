package me.w1992wishes.spark.offline.preprocess.core

/**
  * @author w1992wishes 2019/3/1 19:53
  */
trait PreProcess extends Serializable{

  def preProcessBefore(): Unit

  def preProcess(): Unit

  def preProcessPost(): Unit

  def wholePreProcess(): Unit
}
