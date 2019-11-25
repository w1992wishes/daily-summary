package me.w1992wishes.structured.streaming.demo

import org.apache.spark.sql.SparkSession

/**
  * 结构化查询各种模式Demo
  */
object StructuredStreamingQueryModeDemo {

  def main(args: Array[String]): Unit = {
    //创建Spark SQL切入点
    val spark = SparkSession.builder().appName("Structured-Streaming").getOrCreate()
    //读取服务器端口发来的行数据，格式是DataFrame
    val linesDF = spark.readStream.format("socket").option("host", "localhost").option("port", 9999).load()
    //隐士转换（DataFrame转DataSet）
    import spark.implicits._
    //行转换成一个个单词
    val words = linesDF.as[String].flatMap(_.split(" "))
    //按单词计算词频
    val wordCounts = words.groupBy("value").count()

    val query = wordCounts.writeStream
      .outputMode("complete") //与.outputMode(OutputMode.Complete)等价
      .format("console")
      .start()

    query.awaitTermination()

  }

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