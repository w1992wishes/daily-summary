package me.w1992wishes.common.tool

import com.typesafe.config.{Config, ConfigFactory}
import me.w1992wishes.common.helper.SparkHelper
import me.w1992wishes.common.util.Log
import org.apache.hadoop.fs.Path
import org.apache.spark.sql.SparkSession

import scala.collection.immutable._
import scala.collection.mutable.ArrayBuffer

object MergeFile extends SparkHelper with Log {
  def main(args: Array[String]): Unit = {

    val config: Config = ConfigFactory.load("application.conf")

    // spark 参数设置
    val spark = buildSpark

    val hconf = spark.sparkContext.hadoopConfiguration
    val fs = org.apache.hadoop.fs.FileSystem.get(hconf)

    val path = config.getString("tool.merge.file.path")
    val files = fs.listFiles(new org.apache.hadoop.fs.Path(path), true)
    // path
    var dirs = ArrayBuffer[org.apache.hadoop.fs.Path]()
    while (files.hasNext) {
      val next = files.next()
      if (next.getPath.getName.startsWith("part")) dirs += next.getPath.getParent
    }

    // path, count
    val dirCountMap: Map[Path, Int] = dirs.map((_, 1)).groupBy(_._1).mapValues(_.size).filter(_._2 > 1)


    val blockSize = 128 * 1024 * 1024
    val dirSizeMap = dirCountMap.keys.map { p => (p, fs.getContentSummary(p).getLength / blockSize + 1) }

    logInfo(s"------------- ${path.toString} need to merge ${dirSizeMap.size} dir path ------------------------")

    dirSizeMap.foreach { case (k, v) =>
      logInfo(s"--------------- merge ${k.toString} to $v file ------------------")
      merge(spark, k, v.toInt)
    }

    fs.delete(new Path("/tmp/merge"), true)
    spark.close()
  }

  def merge(spark: SparkSession, path: Path, coalesceNum: Int): Unit = {
    val ps = path.toString
    val tmpPath = "/tmp/merge" + ps.substring(ps.indexOf("/user"))
    try {
      spark.read.parquet(ps)
        .coalesce(coalesceNum)
        .write.format("parquet")
        .mode("overwrite")
        .save(tmpPath)
      spark.read.parquet(tmpPath)
        .write.format("parquet")
        .mode("overwrite")
        .save(ps)
    } catch {
      case ex: Exception =>
        logError(s"--------------- $ps merge failed, ${ex.getMessage} ------------------")
    }
  }

}

