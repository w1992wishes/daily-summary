package me.w1992wishes.spark.graphx

import org.apache.spark.graphx.{Graph, GraphLoader, VertexRDD}
import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/9/6 23:05.
  */
object InDegrees {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().appName("").master("local").getOrCreate()

    val graph: Graph[Int, Int] = GraphLoader.edgeListFile(spark.sparkContext, "Cit-HepTh.txt")

    // 入度
    val inDegrees: VertexRDD[Int] = graph.inDegrees

    inDegrees.take(10).foreach(println(_))

    println(inDegrees.reduce((a, b) => if (a._2 > b._2) a else b))
  }

}
