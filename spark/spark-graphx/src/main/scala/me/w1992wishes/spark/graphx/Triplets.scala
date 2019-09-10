package me.w1992wishes.spark.graphx

import org.apache.spark.graphx.{Edge, Graph}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

/**
  * EdgeTriplet 的常用成员属性
  * 字段  描述
  * Attr  边的属性数据
  * srcid  边的源顶点的回
  * srcAttr  边的源顶点的属性数据
  * dstid  边的目标顶点的 ID
  * dstAttr  边的目标顶点的属性数据
  * @author w1992wishes 2019/9/10 22:47.
  */
object Triplets extends SampleGraphTrait {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().master("local").getOrCreate()

    val myGraph: Graph[String, String] = sampleGraph(spark)

    // 从构建的图中获得边集合
    /**
      * Edge(1,2,is-friends-with )
      * Edge(2,3,is-friends-with)
      * Edge(3,4,is-friends-with)
      * Edge(3,5,Wrote- status )
      * Edge(4,5,Likes-status)
      */
    myGraph.edges.collect.foreach(println(_))

    // 可以使用 triplets （）方法，根据 VertexId 将顶点和边联合在一起 。 Graph 将数据分开存储在对应的边 RDD 和顶点 RDD 内， triplets（）函数方便地将它们联合在一起
    // 函数 triplets （）返回 EdgeTriplet[VD,ED ］类型的 RDD ，它是 Edge [ED]的子类，并包含边的源顶点和目标顶点的引用
    // EdgeTriplet 类提供了访问边（以及边属性数据）以及源顶点和目标顶点属性数据的方法
    /**
      * ((1,Ann),(2,Bill),is-friends-with)
      * ((2,Bill),(3,Charles),is-friends-with)
      * ((3,Charles),(4,Diane),is-friends-with)
      * ((3,Charles),(5,Went to gym this morning),Wrote-status )
      * ((4,Diane),(5,Went to gym this morning),Likes-status)
      */
    myGraph.triplets.collect.foreach(println(_))

    spark.stop()
  }

}
