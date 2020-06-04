package me.w1992wishes.spark.mllib.intellif

import org.apache.spark.mllib.linalg.Vector

import scala.collection.mutable.ArrayBuffer

/**
  * @author w1992wishes 2018/7/5 20:48
  */
abstract class SampleStrategy {

  def takeSamples(sourcesPoints : ArrayBuffer[Vector], nums : Int) : Array[Vector]

}
