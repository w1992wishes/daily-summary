package me.w1992wishes.spark.hive.intellif.entryexit

import me.w1992wishes.common.analysis.HiveAnalysis
import me.w1992wishes.common.conf.{ParamOptions, ParamParser}
import me.w1992wishes.spark.hive.intellif.conf.{ConfigArgs, ConfigArgsDrugRelated}
import me.w1992wishes.spark.hive.intellif.task.DrugRelatedAnalysis
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.storage.StorageLevel

/**
  * 每日数据汇聚
  *
  * @author w1992wishes 2020/7/2 16:29
  */
object SameEntryExitAnalysisDaily extends DrugRelatedAnalysis with HiveAnalysis {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    val params = ParamParser(args).getConf

    // 参数设置
    val bizCode = params.getProperty(ParamOptions.BIZ_CODE, ConfigArgs.bizCode)
    //分析数据时间范围的结束时间
    val (startTime, endTime) = getQueryTime(params)

    // 配置
    val labelSiteSql = ConfigArgsDrugRelated.hiveEntryExitAnalysisDailyLabelSiteSql.replace("#biz_code#", bizCode)
    val labelPersonSql = ConfigArgsDrugRelated.hiveEntryExitAnalysisDailyLabelPersonSql.replace("#biz_code#", bizCode)
    val eventSql = ConfigArgsDrugRelated.hiveEntryExitAnalysisDailyEventSql.replace("#biz_code#", bizCode)
      .replace("#start_time#", startTime).replace("#end_time#", endTime)
    val resultTable = ConfigArgsDrugRelated.hiveEntryExitAnalysisDailyResultTable.replace("#biz_code#", bizCode)

    logInfos(
      "bizCode : " + bizCode,
      "startTime : " + startTime,
      "endTime : " + endTime,
      "labelSiteSql : " + labelSiteSql,
      "labelPersonSql : " + labelPersonSql,
      "eventSql : " + eventSql,
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

    val eventDF = loadFromHive(spark, eventSql).filter("source_id is not null")
    val labelSiteDF = loadFromHive(spark, labelSiteSql)
    val labelPersonDF = loadFromHive(spark, labelPersonSql)

    val map = Map("spark" -> spark,
      "eventDF" -> eventDF,
      "labelSiteDF" -> labelSiteDF,
      "labelPersonDF" -> labelPersonDF
    )

    val resultDF = handle(map)

    logInfo("--------------写 hive--------------")
    saveToHive(resultDF, resultTable, SaveMode.Overwrite)

    spark.stop()
  }

  /**
    * 业务处理逻辑，具体分析任务实现
    */
  override def handle(params: Any*): DataFrame = {
    val map = params.toList.head.asInstanceOf[Map[String, Any]]
    val eventDF = map("eventDF").asInstanceOf[DataFrame]
    val labelSiteDF = map("labelSiteDF").asInstanceOf[DataFrame]
    val labelPersonDF = map("labelPersonDF").asInstanceOf[DataFrame]

    // 关联标签，过滤出重点场所 biz_code, aid, data_type, dt, group_id
    logInfo("--------------重点场所事件--------------")
    val allWithLabelDF = eventDF.join(labelSiteDF, Seq("source_id"), "left").filter("group_id is not null")
      .select("biz_code", "aid", "data_type", "dt", "group_id")

    // 涉毒人员
    // aid, label_code
    import org.apache.spark.sql.functions.broadcast
    // 关联前科人员 biz_code, aid, data_type, dt, group_id -> biz_code, aid, data_type, dt, group_id, label_code
    val allWithDrugRelatedDF = allWithLabelDF.join(broadcast(labelPersonDF), Seq("aid"), "left")

    allWithDrugRelatedDF.persist(StorageLevel.MEMORY_AND_DISK)
    // target_aid, target_data_type, dt, group_id
    logInfo("--------------涉毒人员事件--------------")
    val resultTargetDF = allWithDrugRelatedDF
      .filter("label_code is not null")
      .withColumnRenamed("aid", "target_aid")
      .withColumnRenamed("data_type", "target_data_type")
      .select("target_aid", "target_data_type", "dt", "group_id")
      .dropDuplicates(Seq("group_id", "dt", "target_aid"))

    // biz_code, source_aid, source_data_type, dt, group_id
    logInfo("--------------同行人员事件--------------")
    val resultSourceDF = allWithDrugRelatedDF
      .filter("label_code is null")
      .withColumnRenamed("aid", "source_aid")
      .withColumnRenamed("data_type", "source_data_type")
      .select("biz_code", "source_aid", "source_data_type", "dt", "group_id")
      .dropDuplicates(Seq("group_id", "dt", "source_aid"))

    allWithDrugRelatedDF.unpersist()

    // group_id, dt, biz_code, source_aid, source_data_type, target_aid, target_data_type
    val resultDF = resultSourceDF.join(resultTargetDF, Seq("group_id", "dt"))

    resultDF.select("biz_code", "source_aid", "source_data_type", "target_aid", "target_data_type", "group_id", "dt")
  }
}
