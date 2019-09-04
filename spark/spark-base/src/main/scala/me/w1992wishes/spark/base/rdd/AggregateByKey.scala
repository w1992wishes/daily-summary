package me.w1992wishes.spark.base.rdd

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, SparkSession}

/**
  * 统计用户的总访问次数和访问 URL 次数
  *
  * 类似
  *
  * select id,name, count(0) as ct,count(distinct url) as urlcount
  * from table
  * group by id,name;
  *
  * map阶段： id和name组合为key， url为value
  * reduce阶段： len(urls) 出现次数, len(set(urls)) 访问 url 数
  *
  * @author w1992wishes 2019/9/4 13:47
  */
object AggregateByKey {

  case class User(id: String, name: String, vtm: String, url: String)

  def main(args: Array[String]) {

    /**
      * id1,user1,2,http://www.hupu.com
      * id1,user1,2,http://www.hupu.com
      * id1,user1,3,http://www.hupu.com
      * id1,user1,100,http://www.hupu.com
      * id2,user2,2,http://www.hupu.com
      * id2,user2,1,http://www.hupu.com
      * id2,user2,50,http://www.hupu.com
      * id2,user2,2,http://touzhu.hupu.com
      */
    val spark = SparkSession.builder().appName("AggregateByKey").master("local").getOrCreate()
    import spark.implicits._

    val data: Dataset[String] = spark.read.textFile("src/main/resources/Aggregate.txt")
    val userDs1: Dataset[User] = data.map(line => {
      val r = line.split(",")
      User(r(0), r(1), r(2), r(3))
    })

    // map阶段： id和name组合为key， url为value
    val userDs2: Dataset[((String, String), User)] = userDs1.map(user => ((user.id, user.name), user))

    // map 阶段 combine
    val seqOp = (a: (Int, List[String]), b: User) => a match {
      case (0, List()) => (1, List(b.url))
      case _ => (a._1 + 1, b.url :: a._2)
    }

    // reduce 阶段 combine
    val combOp = (a: (Int, List[String]), b: (Int, List[String])) => {
      (a._1 + b._1, a._2 ::: b._2)
    }

    println("-----------------------------------------")
    val rdd3: RDD[((String, String), (Int, List[String]))] = userDs2.rdd.aggregateByKey((0, List[String]()))(seqOp, combOp)
    val rdd4: RDD[((String, String), Int, Int)] = rdd3.map(a => {
      (a._1, a._2._1, a._2._2.distinct.length)
    })
    rdd4.collect.foreach(println)
    println("-----------------------------------------")
    spark.stop()
  }
}
