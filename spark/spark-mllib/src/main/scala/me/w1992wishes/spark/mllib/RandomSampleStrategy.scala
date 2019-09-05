package me.w1992wishes.spark.mllib

import org.apache.spark.mllib.linalg.Vector

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/**
  * @author w1992wishes 2018/7/5 20:51
  */
class RandomSampleStrategy extends SampleStrategy {

  override def takeSamples(sourcesPoints: ArrayBuffer[Vector], nums: Int): Array[Vector] = {
    var samples : ArrayBuffer[Vector] = new ArrayBuffer[Vector]()
    var random = new Random();
    for (i <- 0 to nums - 1) {
      samples += sourcesPoints(random.nextInt(sourcesPoints.size))
    }
    samples.toArray;
  }

}
