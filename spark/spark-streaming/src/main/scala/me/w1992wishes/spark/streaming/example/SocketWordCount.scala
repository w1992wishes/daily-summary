package me.w1992wishes.spark.streaming.example

import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/11/13 20:05
  */
object SocketWordCount {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder
      .master("local")
      .appName("StructuredNetworkWordCount")
      .getOrCreate()

    // Create DataFrame representing the stream of input lines from connection to localhost:9999
    val lines = spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", 9999)
      .load()

    import spark.implicits._
    // Split the lines into words
    val words = lines.as[String].flatMap(_.split(" "))

    // Generate running word count
    val wordCounts = words.groupBy("value").count()

    // Start running the query that prints the running counts to the console
    val query = wordCounts.writeStream
      .outputMode("complete")
      .format("console")
      .start()

    query.awaitTermination()

    /**
      * 需要执行 Netcat 来向 localhost:9999 发送数据，比如
      *
      * linux
      * nc -lk 9999
      * apache spark
      * apache hadoop
      * ...
      *
      * windows
      * C:\Users\Administrator>nc -l -p 9999
      * apache spark
      * apache hadoop
      */
  }

}
