package me.w1992wishes.spark.mllib.rough

import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.rdd.RDD

/**
  * @author w1992wishes 2018/7/13 17:22
  */
class RoughModel (val clusterCenters: Array[Vector]) {

  /**
    * Maps given points to their cluster indices.
    *
    * @param points
    * @return
    */
  def predict(points: RDD[Vector]): RDD[(Int, Vector)] = {
    val bcCenters = points.context.broadcast(clusterCenters)
    points.map(p =>
      new EuclideanDistanceMeasure().findClosest(bcCenters.value, p))
  }

}
