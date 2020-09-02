package me.w1992wishes.flink.details.cache

import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.scala.{DataSet, ExecutionEnvironment}
import org.apache.flink.configuration.Configuration

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.io.Source
import org.apache.flink.streaming.api.scala._

/**
  * @author w1992wishes 2020/9/2 15:56
  */
object DistributeCacheDetail {

  def main(args: Array[String]): Unit = {
    val env = ExecutionEnvironment.getExecutionEnvironment
    //1"开启分布式缓存
    val path = "hdfs://hadoop01:9000/score"
    env.registerCachedFile(path, "Distribute_cache")

    //2：加载本地数据
    val clazz: DataSet[Clazz] = env.fromElements(
      Clazz(1, "class_1"),
      Clazz(2, "class_1"),
      Clazz(3, "class_2"),
      Clazz(4, "class_2"),
      Clazz(5, "class_3"),
      Clazz(6, "class_3"),
      Clazz(7, "class_4"),
      Clazz(8, "class_1")
    )

    //3:开始进行关联操作
    clazz.map(new MyJoinmap()).print()
  }
}

class MyJoinmap() extends RichMapFunction[Clazz, ArrayBuffer[INFO]] {
  private var myLine = new ListBuffer[String]

  override def open(parameters: Configuration): Unit = {
    val file = getRuntimeContext.getDistributedCache.getFile("Distribute_cache")
    val lines: Iterator[String] = Source.fromFile(file.getAbsoluteFile).getLines()
    lines.foreach(line => {
      myLine.append(line)
    })
  }

  //在map函数下进行关联操作
  override def map(value: Clazz): ArrayBuffer[INFO] = {
    var stoNO = 0
    var subject = ""
    var score = 0.0
    var array = new collection.mutable.ArrayBuffer[INFO]()
    //(学生学号---学科---分数)
    for (str <- myLine) {
      val tokens = str.split(",")
      stoNO = tokens(0).toInt
      subject = tokens(1)
      score = tokens(2).toDouble
      if (tokens.length == 3) {
        if (stoNO == value.stu_no) {
          array += INFO(value.stu_no, value.clazz_no, subject, score)
        }
      }
    }
    array
  }

}

case class INFO(stu_no: Int, clazz_no: String, subject: String, score: Double)

case class Clazz(stu_no: Int, clazz_no: String)