package me.w1992wishes.spark.sql.intellif.task

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import me.w1992wishes.spark.sql.intellif.config.{PersonArchiveBatchEtlArgsTool, PropertiesTool}
import org.apache.spark.sql.functions.{max, min}
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * @author w1992wishes 2019/11/25 15:01
  */
class PersonArchiveBatchEtlTask(propsTool: PropertiesTool) extends BatchEtlTask(propsTool: PropertiesTool) {

  private def getArchiveParalleledCondition(realPartitions: Int, time: Option[Timestamp]): Array[String] = {
    time match {
      case None => Range(0, realPartitions).map(partition => s"CAST(hash_code(archive_id) as numeric) % $realPartitions = $partition").toArray
      case Some(timestamp) => Range(0, realPartitions).map(partition => s"create_time > '$timestamp' and CAST(hash_code(archive_id) as numeric) % $realPartitions = $partition").toArray
    }
  }

  private def archiveTable(sourceTable: String, index: Int, paralleledCondition: Array[String]): String = {
    s"(SELECT archive_id as aid, create_time FROM $sourceTable WHERE ${paralleledCondition(index)}) AS t_tmp_$index"
  }

  private def getArchives(spark: SparkSession, partitions: Int) = {
    createHistoryTable()
    val start = getStartTime()
    val archiveParalleledCondition = getArchiveParalleledCondition(partitions, start)
    val table = propsTool.getString("source.table")
    Range(0, partitions)
      .map(index => {
        spark
          .read
          .format("jdbc")
          .option("driver", propsTool.getString("source.driver"))
          .option("url", propsTool.getString("source.url"))
          .option("dbtable", archiveTable(table, index, archiveParalleledCondition))
          .option("user", propsTool.getString("source.user"))
          .option("password", propsTool.getString("source.password"))
          .option("fetchsize", propsTool.getInt("source.fetchsize"))
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
  }
}

object PersonArchiveBatchEtlTask {

  def main(args: Array[String]): Unit = {
    val argsTool = new PersonArchiveBatchEtlArgsTool(args)
    val propsTool = PropertiesTool(argsTool.confName)
    val task = PersonArchiveBatchEtlTask(propsTool)

    // 初始化 spark
    val spark = SparkSession.builder()
      //.master("local[20]")
      .appName(getClass.getSimpleName)
      .config("spark.debug.maxToStringFields", "100")
      .config("spark.sql.shuffle.partitions", argsTool.shufflePartitions)
      .getOrCreate()

    val archives = task.getArchives(spark, argsTool.partitions)
    archives.cache()
    archives.createOrReplaceTempView("t_archive")

    val bizCode = propsTool.getString("biz.code", "bigdata")

    spark.udf.register("makeNowTime", makeNowTime _)
    spark.sql(s"select aid, 'person' as data_type, create_time, makeNowTime() as modify_time, '$bizCode' as biz_code from t_archive")
      .write
      .mode(SaveMode.Append)
      .format("jdbc")
      .option("driver", propsTool.getString("sink.driver"))
      .option("url", propsTool.getString("sink.url"))
      .option("dbtable", propsTool.getString("sink.table"))
      .option("user", propsTool.getString("sink.user"))
      .option("password", propsTool.getString("sink.password"))
      .option("batchsize", propsTool.getInt("sink.batchsize"))
      .save()

    val createTimeColumn = archives.col("create_time")
    val time = archives.agg(min(createTimeColumn), max(createTimeColumn)).head.getAs[Timestamp](1)
    archives.unpersist()
    task.updateStartTime(time = time)
  }

  private def makeNowTime: Timestamp = Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))

  def apply(propsTool: PropertiesTool): PersonArchiveBatchEtlTask = new PersonArchiveBatchEtlTask(propsTool)
}
