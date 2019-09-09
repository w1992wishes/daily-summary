package me.w1992wishes.spark.graphx

import org.apache.spark.graphx.{Edge, Graph}
import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/9/9 10:01.
  */
object MakeGraph{
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().master("local").getOrCreate()

    val myVertices = spark.sparkContext.makeRDD(Array((1L, "Ann"), (2L, "Bill"),
      (3L, "Charles"), (4L, "Diane"), (5L, "Went to gym this morning")))
    val myEdges = spark.sparkContext.makeRDD(Array(Edge(1L, 2L, "is-friends-with "),
      Edge(2L, 3L, "is-friends-with"), Edge(3L, 4L, "is-friends-with"), Edge(4L, 5L, "Likes-status"), Edge(3L, 5L, "Wrote- status ")
    ))
    // make graph
    val myGraph = Graph(myVertices, myEdges)

    myGraph.inDegrees.foreach(println(_))

    // 从构建的图中获得边集合
    myGraph.edges.collect().foreach(println(_))

    // 获得 triplets 形式的图数据， Graph 本来是将数据分开存储在对应的边 RDD 和顶点 RDD 内， triplets （）函数将它们联合在一起
    myGraph.triplets.collect().foreach(println(_))
  }
}
