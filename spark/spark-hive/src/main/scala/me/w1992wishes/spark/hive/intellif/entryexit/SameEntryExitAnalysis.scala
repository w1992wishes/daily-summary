package me.w1992wishes.spark.hive.intellif.entryexit

import me.w1992wishes.common.analysis.HiveAnalysis
import me.w1992wishes.common.conf.{ParamOptions, ParamParser}
import me.w1992wishes.common.util.DateUtil
import me.w1992wishes.spark.hive.intellif.conf.{ConfigArgs, ConfigArgsDrugRelated}
import me.w1992wishes.spark.hive.intellif.task.DrugRelatedAnalysis
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

/**
  * @author w1992wishes 2020/7/6 11:25
  */
object SameEntryExitAnalysis extends DrugRelatedAnalysis with HiveAnalysis {

  def main(args: Array[String]): Unit = {
    args.foreach(logInfo(_))

    val params = ParamParser(args).getConf

    // 参数设置
    //分析数据时间范围的结束时间
    val bizCode = params.getProperty(ParamOptions.BIZ_CODE, ConfigArgs.bizCode)
    val (startTime, endTime) = getQueryTime(params)
    val outStartTime = DateUtil.transform(startTime, DateUtil.DF_YMD_NO_LINE_DEFAULT, DateUtil.DF_NORMAL)
    val outEndTime = DateUtil.transform(endTime, DateUtil.DF_YMD_NO_LINE_DEFAULT, DateUtil.DF_NORMAL)
    // 读取配置文件
    val sourceSql = ConfigArgsDrugRelated.hiveEntryExitAnalysisSourceSql
      .replace("#biz_code#", bizCode)
      .replace("#start_time#", outStartTime)
      .replace("#end_time#", outEndTime)
    val resultTable = ConfigArgsDrugRelated.hiveEntryExitAnalysisResultTable.replace("#biz_code#", bizCode)

    logInfos(
      "bizCode : " + bizCode,
      "startTime : " + startTime,
      "endTime : " + endTime,
      "sourceSql : " + sourceSql,
      "resultTable : " + resultTable
    )

    val conf = new SparkConf()
      .setIfMissing("spark.master", "local[16]")
      .set("spark.hadoopRDD.ignoreEmptySplits", "true")
      .set("spark.sql.catalogImplementation", "hive")
      .set("spark.io.compression.codec", "snappy")
      .set("hive.exec.dynamic.partition.mode", "nonstrict")
      .set("hive.exec.dynamic.partition", "true")
    val spark = SparkSession
      .builder()
      .config(conf)
      .appName(getClass.getSimpleName)
      .getOrCreate()

    spark.sparkContext.setLogLevel("INFO")

    logInfo(s"$sourceSql")
    val resultDF = loadFromHive(spark, sourceSql)

    saveToHive(resultDF, resultTable, SaveMode.Overwrite)

    spark.stop()
  }

  /**
    * 业务处理逻辑，具体分析任务实现
    */
  override def handle(params: Any*): DataFrame = ???
}
