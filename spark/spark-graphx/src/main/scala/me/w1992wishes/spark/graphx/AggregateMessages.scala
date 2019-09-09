package me.w1992wishes.spark.graphx

import org.apache.spark.graphx.{Edge, EdgeContext, Graph, VertexRDD}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

/**
  * 使用 aggregateMessages[]() 计算每个顶点的出度
  *
  * @author w1992wishes 2019/9/9 22:42.
  */
object AggregateMessages {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().master("local").getOrCreate()
    val myVertices: RDD[(Long, String)] = spark.sparkContext.makeRDD(Array((1L, "Ann"), (2L, "Bill"),
      (3L, "Charles"), (4L, "Diane"), (5L, "Went to gym this morning")))
    val myEdges: RDD[Edge[String]] = spark.sparkContext.makeRDD(Array(Edge(1L, 2L, "is-friends-with "),
      Edge(2L, 3L, "is-friends-with"), Edge(3L, 4L, "is-friends-with"), Edge(4L, 5L, "Likes-status"), Edge(3L, 5L, "Wrote- status ")
    ))
    val myGraph: Graph[String, String] = Graph(myVertices, myEdges)

    calOutDegrees(myGraph)
    calFarthestDis(myGraph)
  }

  // 计算出度，一次 Map/Reduce 即可
  private def calOutDegrees(myGraph: Graph[String, String]): Unit = {
    /**
      * def aggregateMessages[Msg](sendMsg: EdgeContext[VD, ED, Msg] => Unit, mergeMsg: (Msg, Msg) => Msg): VertexRDD[Msg]
      *
      * Msg: 类型表示函数返回结果数据的类型，要对从顶点传出的边数进行统计， Msg 的具体类型选择 Int 比较合适。
      * sendMsg: 函数以 Edge Coηtext 作为输入参数，没有返回值， EdgeContext 提供了两个消息的发送函数:
      *   sendToSrc ：将 Msg 类型的消息发送给源顶点;
      *   sendToDst ： 将 Msg 类型的消息发送给目标顶点。
      * mergeMsg: 每个顶点收到的所有消息都会被聚集起来传递给 mergeMsg 函数
      *
      * 在每个顶点上应用 mergeMsg 函数最终返回一 个 VertexRDD[Int ］对象 。VertexRDD 是一个包含了 二元组的 RDD ，包括了顶点的 B 以及该顶点的 mergeMsg 操作的结果。
      */
    val outDegrees: VertexRDD[Int] = myGraph.aggregateMessages[Int](_.sendToSrc(1), _ + _)
    outDegrees.cache()

    outDegrees.collect.foreach(println(_))
    /**
      * (4,1)
      * (1,1)
      * (3,2)
      * (2,1)
      */
    // 原始的 VertexId 不容易理解，为顶点添加上可读的名字
    outDegrees.join(myGraph.vertices).collect.foreach(println(_))
    /**
      * (4,(1,Diane))
      * (1,(1,Ann))
      * (3,(2,Charles))
      * (2,(1,Bill))
      */
    // 使用 map（）和 swap（）整理输出
    outDegrees.join(myGraph.vertices).map(_._2.swap).collect.foreach(println(_))
    /*
    * (Diane,1)
    * (Ann,1)
    * (Charles,2)
    * (Bill,1)
     */
    // 么重新获得丢弃的 5# 顶点
    outDegrees.rightOuterJoin(myGraph.vertices).map(_._2.swap).collect.foreach(println(_))
    /*
     * (Diane,Some(1))
     * (Ann,Some(1))
     * (Charles,Some(2))
     * (Went to gym this morning,None)
     * (Bill,Some(1))
     */
    // 去除 rightOuterJoin 产生的 some/none
    outDegrees.rightOuterJoin(myGraph.vertices).map(x => (x._2._2, x._2._1.getOrElse(0))).collect.foreach(println(_))
    /*
     * (Diane,1)
     * (Ann,1)
     * (Charles,2)
     * (Went to gym this morning,0)
     * (Bill,1)
     */
  }

  // 为每个顶点标记上离它最远的根顶点的距离，需进行迭代 Map/Reduce
  private def calFarthestDis(myGraph: Graph[String, String]): Unit = {
    // sendMsg 函数作为参数传入 aggregateMessages，这个函数会在图中的每条边上被调用，这里 sendMsg 只是简单的累加计数器。
    def sendMsg(ec: EdgeContext[Int, String, Int]): Unit = {
      ec.sendToDst(ec.srcAttr + 1)
    }

    // 这里定义了 mergeMsg 函数，这个函数会在所有的消息传递到顶点后被重复调用 。消息经过合并后，最终得出结果为包含最大距离值的顶点。
    def mergeMsg(a: Int, b: Int): Int = {
      math.max(a, b)
    }

    def propagateEdgeCount(g: Graph[Int, String]): Graph[Int, String] = {
      // 生成新的顶点集
      val verts: VertexRDD[Int] = g.aggregateMessages[Int](sendMsg, mergeMsg)
      // 生成一个更新后的包含新的信息的圈
      val g2: Graph[Int, String] = Graph(verts, g.edges)
      // 将两组顶点连接在一起来看看更新的图，这会生成新的数据 Tuple2[VertexId, Tuple2[old vertex data, new vertex data] ].
      val check = g2.vertices.join(g.vertices)
        // 查看join 顶点集后的每个元素，并计算元素中的不同点，如果相同则返回0
        .map(x => x._2._1 - x._2._2)
        // 合计所有的不同，如果所有的顶点完全相同，合计结果为0。
        .reduce(_ + _)
      if (check > 0)
        // 如果有变化，则继续递归执行
        propagateEdgeCount(g2)
      else
        //  没有变化则返回传入的图对象
        g
    }

    val initialGraph: Graph[Int, String] = myGraph.mapVertices((_, _) => 0)
    propagateEdgeCount(initialGraph).vertices.collect.foreach(println(_))
    /**
      * (4,3)
      * (1,0)
      * (3,2)
      * (5,4)
      * (2,1)
      */
  }


}
