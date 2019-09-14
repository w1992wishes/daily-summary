package me.w1992wishes.spark.graphx.`trait`

import org.apache.spark.graphx.Graph

/**
  * @author w1992wishes 2019/9/12 22:34.
  */
trait GexfTrait {

  def toGexf[VD,ED](g:Graph[VD,ED]) : String = {
    val string = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<gexf xmlns=\"http://www.gexf.net/1.2draft\" version=\"1.2\">\n" +
      " <graph mode=\"static\" defaultedgetype=\"directed\">\n" +
      " <nodes>\n" +
      g.vertices.map(v => " <node id=\"" + v._1 + "\" label=\"" +
        v._2 + "\" />\n").collect.mkString +
      " </nodes>\n" +
      " <edges>\n" +
      g.edges.map(e => " <edge source=\"" + e.srcId +
        "\" target=\"" + e.dstId + "\" label=\"" + e.attr +
        "\" />\n").collect.mkString +
      " </edges>\n" +
      " </graph>\n" +
      "</gexf>"
    string
  }

}
