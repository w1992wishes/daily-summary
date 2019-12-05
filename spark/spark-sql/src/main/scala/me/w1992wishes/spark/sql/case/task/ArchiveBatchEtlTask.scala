package me.w1992wishes.spark.sql.`case`.task

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import me.w1992wishes.spark.sql.`case`.config.{ArchiveBatchEtlArgsTool, PropertiesTool}
import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.functions.{max, min}
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * @author w1992wishes 2019/11/25 15:01
  */
class ArchiveBatchEtlTask(propsTool: PropertiesTool, eventType: String) extends BatchEtlTask(propsTool: PropertiesTool) {

  private def getEventParalleledCondition(realPartitions: Int, time: Option[Timestamp]): Array[String] = {
    time match {
      case None => Range(0, realPartitions).map(partition => s"CAST(hash_code(id) as numeric) % $realPartitions = $partition").toArray
      case Some(timestamp) => Range(0, realPartitions).map(partition => s"create_time > '$timestamp' and CAST(hash_code(id) as numeric) % $realPartitions = $partition").toArray
    }
  }

  private def eventTable(sourceTable: String, index: Int, paralleledCondition: Array[String]): String = {
    s"(SELECT data_type, aid, biz_code, create_time FROM $sourceTable WHERE ${paralleledCondition(index)}) AS t_tmp_$index"
  }

  private def getEvents(spark: SparkSession, partitions: Int, eventType: String) = {
    createHistoryTable()
    val start = getStartTime()
    val eventParalleledCondition = getEventParalleledCondition(partitions, start)
    val table = propsTool.getString("source.table").replace("#type", eventType)
    Range(0, partitions)
      .map(index => {
        spark
          .read
          .format("jdbc")
          .option("driver", propsTool.getString("source.driver"))
          .option("url", propsTool.getString("source.url"))
          .option("dbtable", eventTable(table, index, eventParalleledCondition))
          .option("user", propsTool.getString("source.user"))
          .option("password", propsTool.getString("source.password"))
          .option("fetchsize", propsTool.getInt("source.fetchsize"))
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
  }

  private def getAidParalleledCondition(realPartitions: Int): Array[String] = {
    Range(0, realPartitions).map(partition => s"CAST(hash_code(aid) as numeric) % $realPartitions = $partition").toArray
  }

  private def aidTable(table: String, index: Int, paralleledCondition: Array[String]): String = {
    s"(SELECT aid FROM $table WHERE ${paralleledCondition(index)}) AS t_tmp_$index"
  }

  private def getAids(spark: SparkSession, partitions: Int, eventType: String) = {
    val aidParalleledCondition = getAidParalleledCondition(partitions)
    val table = propsTool.getString("sink.table").replace("#type", eventType)
    Range(0, partitions)
      .map(index => {
        spark
          .read
          .format("jdbc")
          .option("driver", propsTool.getString("sink.driver"))
          .option("url", propsTool.getString("sink.url"))
          .option("dbtable", aidTable(table, index, aidParalleledCondition))
          .option("user", propsTool.getString("sink.user"))
          .option("password", propsTool.getString("sink.password"))
          .option("fetchsize", propsTool.getInt("sink.batchsize"))
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
  }

  override def getEventType: String = {
    eventType
  }
}

object ArchiveBatchEtlTask {

  def main(args: Array[String]): Unit = {

    val argsTool = new ArchiveBatchEtlArgsTool(args)
    val propsTool = PropertiesTool(argsTool.confName)

    if (StringUtils.isEmpty(argsTool.eventType) ||
      !(argsTool.eventType.equals("car") || argsTool.eventType.equals("mac") || argsTool.eventType.equals("imsi") || argsTool.eventType.equals("imei")) ) {
      System.err.println("eventType is need be car|max|imsi|imei.")
      System.exit(1)
    }

    val task = ArchiveBatchEtlTask(propsTool, argsTool.eventType)

    // 初始化 spark
    val spark = SparkSession.builder()
      //.master("local[20]")
      .appName(getClass.getSimpleName)
      .config("spark.debug.maxToStringFields", "100")
      .config("spark.sql.shuffle.partitions", argsTool.shufflePartitions)
      .getOrCreate()

    val aids = task.getAids(spark, argsTool.partitions, argsTool.eventType)
    aids.createOrReplaceTempView("t_aid")

    val originEvents = task.getEvents(spark, argsTool.partitions, argsTool.eventType)
    originEvents.cache()
    // 先去重
    val events = originEvents.na.drop(cols = Array("aid")).dropDuplicates("aid")

    // 取差集
    val waitToAddAids = events.select("aid").except(aids.select("aid"))

    spark.udf.register("makeNowTime", makeNowTime _)
    val archives = events.join(org.apache.spark.sql.functions.broadcast(waitToAddAids), "aid")
    archives.createOrReplaceTempView("t_archive")

    // import spark.implicits._
    val waitToAddArchives = spark.sql(s"select aid, data_type, biz_code, makeNowTime() as create_time, makeNowTime() as modify_time " +
      s"from t_archive")

    val sinkTable = propsTool.getString("sink.table").replace("#type", argsTool.eventType)
    waitToAddArchives
      .write
      .mode(SaveMode.Append)
      .format("jdbc")
      .option("driver", propsTool.getString("sink.driver"))
      .option("url", propsTool.getString("sink.url"))
      .option("dbtable", sinkTable)
      .option("user", propsTool.getString("sink.user"))
      .option("password", propsTool.getString("sink.password"))
      .option("batchsize", propsTool.getInt("sink.batchsize"))
      .save()

    val createTimeColumn = originEvents.col("create_time")
    val time = originEvents.agg(min(createTimeColumn), max(createTimeColumn)).head.getAs[Timestamp](1)
    originEvents.unpersist()
    task.updateStartTime(time = time)

    spark.stop()

  }

  private def makeNowTime: Timestamp = {
    Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
  }

  def apply(propsTool: PropertiesTool, eventType: String): ArchiveBatchEtlTask = new ArchiveBatchEtlTask(propsTool, eventType)

}