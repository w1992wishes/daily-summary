package me.w1992wishes.spark.graphx.pregel

import me.w1992wishes.spark.graphx.`trait`.SampleGraphTrait
import org.apache.spark.graphx.{EdgeDirection, EdgeTriplet, Pregel, VertexId}
import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/9/14 17:59.
  */
object PregelApi extends SampleGraphTrait {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().master("local").getOrCreate()
    val myGraph = sampleGraph(spark)

    val g = Pregel(myGraph.mapVertices((vid, vd) => 0),
      0,
      activeDirection = EdgeDirection.Out)(
      (id: VertexId, vd: Int, a: Int) => math.max(vd, a),
      (et: EdgeTriplet[Int, String]) =>
        Iterator((et.dstId, et.srcAttr + 1)),
      (a: Int, b: Int) => math.max(a, b))
    g.vertices.collect.foreach(println(_))
  }

}
