package me.w1992wishes.spark.window.demo

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.expressions.Window

/**
  * @author w1992wishes 2019/12/10 20:45
  */
object RowNumberForGroupTopN {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("RowNumberForGroupTopN")
      .getOrCreate()

    val df = spark.createDataFrame(
      Array(
        ("A", "Tom", 78),
        ("B", "James", 47),
        ("A", "Jim", 43),("C", "James", 89),
        ("A", "Lee", 93),("C", "Jim", 65),
        ("A", "James", 10),
        ("C", "Lee", 39),
        ("A", "James", 10),
        ("C", "Lee", 39),
        ("B", "Tom", 99),
        ("C", "Tom", 53),
        ("B", "Lee", 100),
        ("B", "Jim", 100)
      )).toDF("category", "name", "score")

    df.show(false)

    import org.apache.spark.sql.functions._
    val window = Window.partitionBy(col("category")).orderBy(col("score").desc)
    val rowNumDF = df.withColumn("topn", row_number().over(window))

    rowNumDF.show(false)

    val N = 3
    rowNumDF
      .where(col("topn") <= N)
      .show(false)



    // 或者使用 sql 取 TopN
    df.createOrReplaceTempView("grade")
    val sql = "select category, name, score from (select category, name, score, row_number() over (partition by category order by score desc ) rank from grade) g where g.rank <= 3".stripMargin
    val top3DFBySQL = spark.sql(sql)
    top3DFBySQL.show(false)



    // 使用 RDD 取 TopN
    // step 1: 分组
    val groupRDD = df.rdd
      .map(x => (x.getString(0), (x.getString(1), x.getInt(2))))
      .groupByKey()

    // step 2: 排序并取 TopN
    val sortedRDD = groupRDD.map(x => {
      val rawRows = x._2.toBuffer
      val sortedRows = rawRows.sortBy(_._2.asInstanceOf[Integer])
      // 取TopN元素
      if (sortedRows.size > N) {
        sortedRows.remove(0, sortedRows.length - N)
      }
      (x._1, sortedRows.toIterator)
    })

    // step 3: 展开
    val flatRDD = sortedRDD.flatMap(x => {
      val y = x._2
      for (w <- y) yield (x._1, w._1, w._2)
    })

    import spark.implicits._
    flatRDD.toDF("category", "name", "score").show(false)
  }

}
