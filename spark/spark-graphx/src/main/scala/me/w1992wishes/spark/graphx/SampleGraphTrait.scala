package me.w1992wishes.spark.graphx

import org.apache.spark.graphx.{Edge, Graph}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

/**
  * 生成一个Sample Graph
  *
  * @author w1992wishes 2019/9/10 22:55.
  */
trait SampleGraphTrait {

  def sampleGraph( spark: SparkSession): Graph[String, String] = {
    val myVertices: RDD[(Long, String)] = spark.sparkContext.makeRDD(Array((1L, "Ann"), (2L, "Bill"),
      (3L, "Charles"), (4L, "Diane"), (5L, "Went to gym this morning")))

    val myEdges: RDD[Edge[String]] = spark.sparkContext.makeRDD(Array(Edge(1L, 2L, "is-friends-with"),
      Edge(2L, 3L, "is-friends-with"), Edge(3L, 4L, "is-friends-with"), Edge(4L, 5L, "Likes-status"), Edge(3L, 5L, "Wrote-status ")))

    Graph(myVertices, myEdges)
  }

}
