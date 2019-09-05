package me.w1992wishes.spark.mllib.rough

import org.apache.spark.mllib.linalg.Vector

/**
  * @author w1992wishes 2018/7/13 17:27
  */
abstract class DistanceMeasure {

  /**
    * @return the distance between two points.
    */
  def distance(
      v1: Vector,
      v2: Vector): Double

  /**
    * 查找最近的中心点
    *
    * @param centers
    * @param point
    * @return
    */
  def findClosest(centers: TraversableOnce[Vector], point: Vector): (Int, Vector) = {
    var bestDistance = Double.PositiveInfinity
    var bestIndex = 0
    var i = 0
    centers.foreach { center =>
      val currentDistance = distance(center, point)
      if (currentDistance < bestDistance) {
        bestDistance = currentDistance
        bestIndex = i
      }
      i += 1
    }
    (bestIndex, point)
  }

}

class EuclideanDistanceMeasure extends DistanceMeasure {

  override def distance(v1: Vector, v2: Vector): Double = {
    var sum = 0.0
    var i = 0
    val size = v1.size
    require(v2.size == size)
    while (i < size) {
      sum += (v1(i) - v2(i)) * (v1(i) - v2(i))
      i += 1
    }
    math.sqrt(sum)
  }

}
