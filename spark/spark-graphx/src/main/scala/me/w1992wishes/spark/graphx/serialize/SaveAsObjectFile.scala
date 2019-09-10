package me.w1992wishes.spark.graphx.serialize

import me.w1992wishes.spark.graphx.`trait`.SampleGraphTrait
import org.apache.spark.graphx.{Edge, Graph, VertexId}
import org.apache.spark.sql.SparkSession

/**
  *
  * 读写持久化文件
  *
  * @author w1992wishes 2019/9/10 23:26.
  */
object SaveAsObjectFile extends SampleGraphTrait {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().master("local").getOrCreate()

    val myGraph = sampleGraph(spark)

    // Spark RDD 的 API 函数 saveAsObjectFile() 默认用标准的 Java 序列化来序列化顶点和边对象
    // saveAsObjectFile() 函数会在一个目录中保存多个文件，一个 Spark 分区对应其中一个文件。 传入参数作为目录名
    myGraph.vertices.saveAsObjectFile("myGraphVertices")
    myGraph.edges.saveAsObjectFile("myGraphEdges")

    val myGraph2 = Graph(
      spark.sparkContext.objectFile[(VertexId, String)]("myGraphVertices"),
      spark.sparkContext.objectFile[Edge[String]]("myGraphEdges"))

    /**
      * ((1,Ann),(2,Bill),is-friends-with)
      * ((2,Bill),(3,Charles),is-friends-with)
      * ((3,Charles),(4,Diane),is-friends-with)
      * ((3,Charles),(5,Went to gym this morning),Wrote-status )
      * ((4,Diane),(5,Went to gym this morning),Likes-status)
      */
    myGraph.triplets.collect().foreach(println(_))
    myGraph2.triplets.collect().foreach(println(_))
  }

}
