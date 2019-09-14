package me.w1992wishes.spark.graphx.generators

import java.io.PrintWriter

import me.w1992wishes.spark.graphx.`trait`.GexfTrait
import org.apache.spark.graphx.util
import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/9/12 22:34.
  */
object StarGraph extends GexfTrait {

  def main(args: Array[String]): Unit = {

    val sc = SparkSession.builder().master("local").getOrCreate().sparkContext

    val pw = new PrintWriter("starGraph.gexf")
    pw.write(toGexf(util.GraphGenerators.starGraph(sc, 8)))
    pw.close()

    sc.stop()
  }

}
