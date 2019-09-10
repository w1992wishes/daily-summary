package me.w1992wishes.spark.graphx.serialize

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import me.w1992wishes.spark.graphx.`trait`.SampleGraphTrait
import org.apache.spark.graphx.{Edge, Graph}
import org.apache.spark.sql.SparkSession

/**
  * 序列化为 JSON 的简单方法
  *
  * @author w1992wishes 2019/9/10 23:43.
  */
object SaveAsTextFile extends SampleGraphTrait {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().master("local").getOrCreate()

    val myGraph = sampleGraph(spark)

/*    myGraph.vertices.map(x => {
      val mapper = new ObjectMapper()
      mapper.registerModule(DefaultScalaModule)
      val writer = new java.io.StringWriter()
      mapper.writeValue(writer, x)
      writer.toString
    }).coalesce(1, shuffle = true) // 在处理小图时, 为了避免保存到多个不同的文件中，可以先执行 coalesce(l , true ）
      .saveAsTextFile("myGraphVertices")*/

    // 对于每一个顶点，都会构建一个全新的 JSON 解析器。为应对这种情况，Spark API 提供了 mapPartitions() 作为 map() 的替代方案
    myGraph.vertices.mapPartitions(vertices => {
      val mapper = new ObjectMapper()
      mapper.registerModule(DefaultScalaModule)
      val writer = new java.io.StringWriter()
      vertices.map(v => {
        writer.getBuffer.setLength(0)
        mapper.writeValue(writer, v)
        writer.toString
      })
    }).coalesce(1, shuffle = true) // 在处理小图时, 为了避免保存到多个不同的文件中，可以先执行 coalesce(l , true ）
      .saveAsTextFile("myGraphVertices")

    myGraph.edges.mapPartitions(edges => {
      val mapper = new ObjectMapper()
      mapper.registerModule(DefaultScalaModule)
      val writer = new java.io.StringWriter()
      edges.map(e => {
        writer.getBuffer.setLength(0)
        mapper.writeValue(writer, e)
        writer.toString
      })
    }).coalesce(1, shuffle = true) // 在处理小图时, 为了避免保存到多个不同的文件中，可以先执行 coalesce(l , true ）
      .saveAsTextFile("myGraphEdges")


    val myGraph2 = Graph(
      spark.sparkContext.textFile("myGraphVertices")
        .mapPartitions(vertices => {
          val mapper = new ObjectMapper()
          mapper.registerModule(DefaultScalaModule)
          vertices.map(v => {
            val r = mapper.readValue[(Integer, String)](v, new
                TypeReference[(Integer, String)] {})
            (r._1.toLong, r._2)
          })
        }),
      spark.sparkContext.textFile("myGraphEdges")
        .mapPartitions(edges => {
          val mapper = new ObjectMapper()
          mapper.registerModule(DefaultScalaModule)
          edges.map(e => mapper.readValue[Edge[String]](e,
            new TypeReference[Edge[String]] {}))
        })
    )
    myGraph2.triplets.collect().foreach(println(_))
  }
}
