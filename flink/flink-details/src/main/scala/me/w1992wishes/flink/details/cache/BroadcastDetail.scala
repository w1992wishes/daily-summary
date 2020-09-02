package me.w1992wishes.flink.details.cache

import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.configuration.Configuration
import org.apache.flink.api.scala._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object BroadcastDetail {

  def main(args: Array[String]): Unit = {

    val env: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment

    //TODO data2 join data3 的数据，使用广播变量完成 的数据，使用广播变量完成
    val data2 = new mutable.MutableList[(Int, Long, String)]
    data2.+=((1, 1L, "Hi"))
    data2.+=((2, 2L, "Hello"))
    data2.+=((3, 2L, "Hello world"))
    val ds1 = env.fromCollection(Random.shuffle(data2))

    val data3 = new mutable.MutableList[(Int, Long, Int, String, Long)]
    data3.+=((1, 1L, 0, "Hallo", 1L))
    data3.+=((2, 2L, 1, "Hallo Welt", 2L))
    data3.+=((2, 3L, 2, "Hallo Welt wie", 1L))
    val ds2 = env.fromCollection(Random.shuffle(data3))

    //todo 使用内部类 RichMapFunction ，提供 open 和 map ，可以完成 join 的操作 的操作
    val result = ds1.map(new RichMapFunction[(Int, Long, String), ArrayBuffer[(Int, Long, String, String)]] {

      var broadCast: mutable.Buffer[(Int, Long, Int, String, Long)] = _

      override def open(parameters: Configuration): Unit = {
        import scala.collection.JavaConverters._
        //asScala 需要使用隐式转换
        broadCast = this.getRuntimeContext.getBroadcastVariable[(Int, Long, Int, String, Long)]("ds2").asScala
      }

      override def map(value: (Int, Long, String)): ArrayBuffer[(Int, Long, String, String)] = {
        val toArray: Array[(Int, Long, Int, String, Long)] = broadCast.toArray
        val array = new mutable.ArrayBuffer[(Int, Long, String, String)]
        var index = 0

        var a: (Int, Long, String, String) = null
        while (index < toArray.length) {
          if (value._2 == toArray(index)._5) {
            a = (value._1, value._2, value._3, toArray(index)._4)
            array += a
          }

          index = index + 1
        }
        array
      }

    }).withBroadcastSet(ds2, "ds2")

    println(result.collect())

  }

}