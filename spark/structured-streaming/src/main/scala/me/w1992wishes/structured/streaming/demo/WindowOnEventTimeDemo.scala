package me.w1992wishes.structured.streaming.demo

import java.sql.Timestamp

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.OutputMode

/**
  * DataFrame窗口操作Demo
  */
object WindowOnEventTimeDemo {

  case class TimeWord(word: String, timestamp: Timestamp)

  def main(args: Array[String]): Unit = {
    //输入参数个数
    if (args.length < 3) {
      System.err.println("Usage: WindowOnEventTimeDemo <host地址> <端口号>" +
        " <窗口长度/秒> [<窗口滑动距离/秒>]")
      System.exit(1)
    }
    //host地址
    val host = args(0)
    //端口号
    val port = args(1).toInt
    //窗口长度
    val windowSize = args(2).toInt
    //窗口滑动距离，默认是窗口长度
    val slideSize = if (args.length == 3) windowSize else args(3).toInt
    //窗口滑动距离应当小于或等于窗口长度
    if (slideSize > windowSize) {
      System.err.println("窗口滑动距离应当小于或等于窗口长度")
    }
    //以秒为单位
    val windowDuration = s"$windowSize seconds"
    val slideDuration = s"$slideSize seconds"

    //创建Spark SQL切入点
    val spark = SparkSession.builder()
      .master("local[2]")
      .appName("Structured-Streaming")
      .getOrCreate()

    import spark.implicits._

    // 创建DataFrame
    val lines = spark.readStream
      .format("socket")
      .option("host", host)
      .option("port", port)
      .option("includeTimestamp", value = true) //添加时间戳
      .load()

    // 分词
    val words = lines.as[(String, Timestamp)]
      .flatMap(line => line._1.split(" ").map(word => TimeWord(word, line._2)))
      .toDF()

    // 计数
    val windowedCounts = words
      .withWatermark("timestamp", "1 minute") // withWatermark 必须在水印聚合被使用前调用
      .groupBy(window($"timestamp", windowDuration, slideDuration), $"word")
      .count()
      //.orderBy("window")

    // 查询
    val query = windowedCounts.writeStream
      // .outputMode("complete")
      .outputMode(OutputMode.Update()) // 水印输出模式必须是追加（Append Mode）和更新模式（Update Mode）
      .format("console")
      .option("truncate", "false")
      .start()

    query.awaitTermination()
  }

  /**
    * +------------------------------------------+----+-----+
    * |window                                    |word|count|
    * +------------------------------------------+----+-----+
    * |[1970-01-19 13:24:00, 1970-01-19 13:24:10]|a   |2    |
    * |[1970-01-19 13:24:00, 1970-01-19 13:24:10]|bj  |1    |
    * |[1970-01-19 13:24:00, 1970-01-19 13:24:10]|sz  |1    |
    * |[1970-01-19 13:24:00, 1970-01-19 13:24:10]|love|3    |
    * |[1970-01-19 13:24:00, 1970-01-19 13:24:10]|wife|1    |
    * |[1970-01-19 13:24:00, 1970-01-19 13:24:10]|I   |5    |
    * |[1970-01-19 13:24:00, 1970-01-19 13:24:10]|man |1    |
    * |[1970-01-19 13:24:00, 1970-01-19 13:24:10]|have|1    |
    * |[1970-01-19 13:24:00, 1970-01-19 13:24:10]|cat |1    |
    * |[1970-01-19 13:24:00, 1970-01-19 13:24:10]|my  |1    |
    * |[1970-01-19 13:24:00, 1970-01-19 13:24:10]|good|1    |
    * |[1970-01-19 13:24:00, 1970-01-19 13:24:10]|am  |1    |
    * |[1970-01-19 13:24:05, 1970-01-19 13:24:15]|am  |1    |
    * |[1970-01-19 13:24:05, 1970-01-19 13:24:15]|cat |1    |
    * |[1970-01-19 13:24:05, 1970-01-19 13:24:15]|a   |2    |
    * |[1970-01-19 13:24:05, 1970-01-19 13:24:15]|my  |1    |
    * |[1970-01-19 13:24:05, 1970-01-19 13:24:15]|I   |5    |
    * |[1970-01-19 13:24:05, 1970-01-19 13:24:15]|man |1    |
    * |[1970-01-19 13:24:05, 1970-01-19 13:24:15]|bj  |1    |
    * |[1970-01-19 13:24:05, 1970-01-19 13:24:15]|wife|1    |
    * +------------------------------------------+----+-----+
    */
}