package me.w1992wishes.spark.graphx.generators

import java.io.PrintWriter

import me.w1992wishes.spark.graphx.`trait`.GexfTrait
import org.apache.spark.graphx._
import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/9/12 22:25.
  */
object GridGraph extends GexfTrait {

  def main(args: Array[String]): Unit = {

    val sc = SparkSession.builder().master("local").getOrCreate().sparkContext

    val pw = new PrintWriter("gridGraph.gexf")
    pw.write(toGexf(util.GraphGenerators.gridGraph(sc, 4, 4)))
    pw.close()

    sc.stop()
  }

}
