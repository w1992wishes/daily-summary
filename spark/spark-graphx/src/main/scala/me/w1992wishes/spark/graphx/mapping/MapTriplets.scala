package me.w1992wishes.spark.graphx.mapping

import me.w1992wishes.spark.graphx.`trait`.SampleGraphTrait
import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/9/10 22:54.
  */
object MapTriplets extends SampleGraphTrait {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().master("local").getOrCreate()

    val myGraph = sampleGraph(spark)

    /**
      * ((1,Ann),(2,Bill),is-friends-with)
      * ((2,Bill),(3,Charles),is-friends-with)
      * ((3,Charles),(4,Diane),is-friends-with)
      * ((3,Charles),(5,Went to gym this morning),Wrote-status)
      * ((4,Diane),(5,Went to gym this morning),Likes-status)
      */
    myGraph.triplets
      .collect
      .foreach(println(_))

    // 东边属性上增加布尔类型的属性来表示一个条件限制
    /**
      * ((1,Ann),(2,Bill),(is-friends-with,true))
      * ((2,Bill),(3,Charles),(is-friends-with,false))
      * ((3,Charles),(4,Diane),(is-friends-with,true))
      * ((3,Charles),(5,Went to gym this morning),(Wrote-status,false))
      * ((4,Diane),(5,Went to gym this morning),(Likes-status,false))
      */
    myGraph.mapTriplets(t => (t.attr, t.attr == "is-friends-with" && t.srcAttr.toLowerCase.contains("a")))
      .triplets
      .collect
      .foreach(println(_))

  }

}
