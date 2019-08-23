package me.w1992wishes.spark.example.sql

import java.time.{Duration, LocalDateTime}

import com.alibaba.fastjson.JSON
import me.w1992wishes.common.util.DateUtils
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

import scala.collection.mutable.ArrayBuffer

/**
  * @author w1992wishes 2019/7/26 11:10
  */
object DifferenceSetCalculate {

  def main(args: Array[String]): Unit = {

    args.foreach(println(_))

    val commandLineArgs: CommandLineArgs = new CommandLineArgs(args)

    val spark: SparkSession =
      if (commandLineArgs.isLocal)
        SparkSession.builder()
          .master("local[2]")
          .appName("DifferenceSetCalculate")
          .getOrCreate()
      else
        SparkSession.builder()
          .appName("DifferenceSetCalculate")
          .getOrCreate()
    spark.sparkContext.setLogLevel("info")

    // 1.档案源
    val archiveDF: DataFrame = getArchiveDF(spark, commandLineArgs)
    // 档案数据表
    archiveDF.createOrReplaceTempView("archive_table")
    println(s"档案数据: ${archiveDF.count()}")

    // 2.档案事件源中的身份证号
    val archiveEventIdentityNoDF: DataFrame = getArchiveEventDF(spark, commandLineArgs)
    // 档案事件源中的身份证号表
    archiveEventIdentityNoDF.createOrReplaceTempView("event_identity_table")
    println(s"档案事件数据: ${archiveEventIdentityNoDF.count()}")
    // 保存从档案事件中的 field1 json 中抽出来的 identity_no
    saveDF(archiveEventIdentityNoDF, commandLineArgs, "identity_table")

    // 3.获取两个身份证 id 的差集
    val exceptIdentityNoDF = /*spark.sql("select identity_no from archive_table except select identity_no from event_identity_table")*/
    archiveDF.select("identity_no").except(archiveEventIdentityNoDF.select("identity_no"))
    // 档案表中的身份证号 同 档案事件表中的身份证号 的差集
    exceptIdentityNoDF.createOrReplaceTempView("identity_except_table")
    println(s"差集数据: ${exceptIdentityNoDF.count()}")
    // 保存差集数据，用于校验是否正确
    saveDF(exceptIdentityNoDF, commandLineArgs, "identity_except_table")

    // 4.将 差集档案信息 存入数据表
    val exceptArchiveDF: DataFrame = spark.sql("select b.* from identity_except_table a left join archive_table b on a.identity_no = b.identity_no")
    exceptArchiveDF.write
      .mode(SaveMode.Append)
      .format("jdbc")
      .option("driver", "com.pivotal.jdbc.GreenplumDriver")
      .option("url", s"jdbc:pivotal:greenplum://${commandLineArgs.gpIp}:5432;DatabaseName=bigdata_odl")
      .option("dbtable", commandLineArgs.archiveExceptTable)
      .option("user", "gpadmin")
      .option("password", "gpadmin")
      .option("batchsize", 5000)
      .save()
  }

  private def getArchiveEventDF(spark: SparkSession, commandLineArgs: CommandLineArgs) = {
    //import spark.implicits._
    val archiveEventParalleledTimes: Array[String] = getParalleledTimes(
      DateUtils.strToDateTime(commandLineArgs.archiveEventStartTime).minusMinutes(1),
      DateUtils.strToDateTime(commandLineArgs.archiveEventEndTime),
      commandLineArgs.partitions)

    import spark.implicits._
    Range(0, commandLineArgs.partitions)
      .map(index => {
        spark
          .read
          .format("jdbc")
          .option("driver", "com.pivotal.jdbc.GreenplumDriver")
          .option("url", s"jdbc:pivotal:greenplum://${commandLineArgs.gpIp}:5432;DatabaseName=bigdata_dwd")
          .option("dbtable", s"(SELECT * FROM ${commandLineArgs.archiveEventTable} WHERE ${archiveEventParalleledTimes(index)}) AS t_archive_event_$index")
          .option("user", "gpadmin")
          .option("password", "gpadmin")
          .option("fetchsize", 5000)
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
      .map(row => row.getAs[String]("field1"))
      .map(field => JSON.parseObject(field).getString("identityNo"))
      .toDF("identity_no")
  }

  private def getArchiveDF(spark: SparkSession, commandLineArgs: CommandLineArgs): DataFrame = {

    val archiveParalleledTimes: Array[String] = getParalleledTimes(
      DateUtils.strToDateTime(commandLineArgs.archiveStartTime).minusMinutes(1),
      DateUtils.strToDateTime(commandLineArgs.archiveEndTime),
      commandLineArgs.partitions)

    Range(0, commandLineArgs.partitions)
      .map(index => {
        spark
          .read
          .format("jdbc")
          .option("driver", "com.pivotal.jdbc.GreenplumDriver")
          .option("url", s"jdbc:pivotal:greenplum://${commandLineArgs.gpIp}:5432;DatabaseName=bigdata_odl")
          .option("dbtable", s"(SELECT * FROM ${commandLineArgs.archiveTable} WHERE ${archiveParalleledTimes(index)}) AS t_archive_$index")
          .option("user", "gpadmin")
          .option("password", "gpadmin")
          .option("fetchsize", 5000)
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
  }

  // 保存 DF
  private def saveDF(archiveEventDF: DataFrame, commandLineArgs: CommandLineArgs, table: String): Unit = {
    if (commandLineArgs.isSaveTempData) {
      archiveEventDF.write
        .mode(SaveMode.Overwrite)
        .format("jdbc")
        .option("driver", "com.pivotal.jdbc.GreenplumDriver")
        .option("url", s"jdbc:pivotal:greenplum://${commandLineArgs.gpIp}:5432;DatabaseName=bigdata_odl")
        .option("dbtable", table)
        .option("user", "gpadmin")
        .option("password", "gpadmin")
        .option("batchsize", 5000)
        .save()
    }
  }

  /**
    * 获取 spark 并行查询的时间条件数组
    *
    * @param startTime      并行的开始时间
    * @param endTime        并行的结束时间
    * @param realPartitions 实际分区数
    * @return
    */
  protected def getParalleledTimes(startTime: LocalDateTime, endTime: LocalDateTime, realPartitions: Int): Array[String] = {

    val timePart = ArrayBuffer[(String, String)]()

    // 查询的开始时间和结束时间的间隔秒数
    val durationSeconds: Long = Duration.between(startTime, endTime).toMillis / 1000
    // 计算每个分区实际提取数据的时间间隔
    val step = if (realPartitions > 1) durationSeconds / (realPartitions - 1) else 1
    for (x <- 1 until realPartitions) {
      timePart += DateUtils.dateTimeToStr(startTime.plusSeconds((x - 1) * step)) -> DateUtils.dateTimeToStr(startTime.plusSeconds(x * step))
    }
    timePart += DateUtils.dateTimeToStr(startTime.plusSeconds((realPartitions - 1) * step)) -> DateUtils.dateTimeToStr(endTime)

    // 转换为 spark sql where 字句中的查询条件
    timePart.map {
      case (start, end) =>
        s"create_time > '$start' AND create_time <= '$end'"
    }.toArray
  }

}
