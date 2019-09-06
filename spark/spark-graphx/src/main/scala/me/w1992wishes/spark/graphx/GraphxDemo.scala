package me.w1992wishes.spark.graphx

import org.apache.spark.graphx.GraphLoader
import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/9/6 23:05.
  */
object GraphxDemo {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().appName("").master("local").getOrCreate()

    val graph = GraphLoader.edgeListFile(spark.sparkContext, "Cit-HepTh.txt")

    print(graph.inDegrees.reduce((a, b) => if (a._2 > b._2) a else b))
  }

}
