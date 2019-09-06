package me.w1992wishes.spark.base.df

import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/9/6 17:39
  */
object GroupByKey {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().appName("GroupByKey").master("local").getOrCreate()

    val data = Seq(("783423_890932_20190901 00:23:01", 1), ("783423_890932_20190901 00:23:01", 1),
      ("783432_823932_20190902 12:13:31", 2), ("783432_823932_20190802 04:25:23", 3))
    val df = spark.createDataFrame(data).toDF("peers", "t")

    import spark.implicits._
    val df1 = df.map(_.getString(0))
      .map(row => (row.split(" ")(0), 1))
      .groupByKey(_._1)
      .count()

    df1.show()

    val df2 = df1.map(row => {
      val splits = row._1.split("_")
      (splits(0) + "_" + splits(1), "t" + splits(2).replace("-", "") + ":" + row._2)
    })
      .groupByKey(_._1)
      .mapValues(row => row._2)
      .mapGroups((k, i) => (k, i.toSeq))

    df2.show()

    df2.foreachPartition(
      rows => {
        val list = rows.map(row => {
          val splits = row._1.split("_")
          val peers = row._2.foldLeft("{")((s, e) => s + e + ",").dropRight(1) + "}"
          s"{from:'${splits(0)}',to:'${splits(1)}',key: '${row._1}', peers:$peers}"
        }).toList
        list.foreach(println(_))
      })
  }

}
