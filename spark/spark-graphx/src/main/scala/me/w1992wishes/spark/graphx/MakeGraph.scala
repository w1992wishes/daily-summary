package me.w1992wishes.spark.graphx

import me.w1992wishes.spark.graphx.`trait`.SampleGraphTrait
import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/9/9 10:01.
  */
object MakeGraph extends SampleGraphTrait {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().master("local").getOrCreate()
    // make graph
    val myGraph = sampleGraph(spark)

    myGraph.inDegrees.foreach(println(_))

    // 从构建的图中获得边集合
    myGraph.edges.collect().foreach(println(_))

    // 获得 triplets 形式的图数据， Graph 本来是将数据分开存储在对应的边 RDD 和顶点 RDD 内， triplets （）函数将它们联合在一起
    myGraph.triplets.collect().foreach(println(_))

    spark.stop()
  }
}
