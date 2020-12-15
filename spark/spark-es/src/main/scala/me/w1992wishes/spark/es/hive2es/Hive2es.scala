package me.w1992wishes.spark.es.hive2es

import java.util.Properties

import me.w1992wishes.common.analysis.ESAnalysis
import me.w1992wishes.common.conf.{ConfigArgsGlobal, ParamOptions}
import me.w1992wishes.common.util.DateUtil
import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * @author w1992wishes 2020/11/19 9:57
  */
trait Hive2es extends ConfigArgsGlobal with ESAnalysis {

  /**
    * 业务处理逻辑，具体分析任务实现
    */
  override def handle(params: Any*): DataFrame = {
    val map = params.toList.head.asInstanceOf[Map[String, Any]]
    val spark = map("spark").asInstanceOf[SparkSession]
    val sourceDF = map("sourceDF").asInstanceOf[DataFrame]
    val expandSourceId = map("expandSourceId").asInstanceOf[Boolean]

    var result: DataFrame = sourceDF
    if (expandSourceId) {
      import org.apache.spark.sql.functions._
      import spark.implicits._
      result = sourceDF
        .withColumn("source_id", get_json_object($"props", "$.sourceId"))
        .withColumn("source_type", get_json_object($"props", "$.sourceType"))
    }
    result
  }

  def getQueryTime(params: Properties, dbSql: String): (String, String) = {

    var (startTime, endTime) = (startTimeGlobal, endTimeGlobal)

    if (StringUtils.isEmpty(endTimeGlobal) || "''".equals(endTimeGlobal)) {
      endTime = params.getProperty(ParamOptions.END_TIME, DateUtil.today(DateUtil.DF_YMD_NO_LINE))
    }
    if (StringUtils.isEmpty(startTimeGlobal) || "''".equals(startTimeGlobal)) {
      startTime = params.getProperty(ParamOptions.START_TIME,
        DateUtil.dateAgo(endTime, params.getProperty(ParamOptions.DAYS_AGO, "1").toInt, DateUtil.DF_YMD_NO_LINE_DEFAULT, DateUtil.DF_YMD_NO_LINE))
    }

    if (dbSql.contains("dt >= '#start_time#'") || dbSql.contains("dt>='#start_time#'") || dbSql.contains("dt >='#start_time#'") || dbSql.contains("dt>= '#start_time#'")) {
      (startTime, endTime)
    } else {
      (DateUtil.transform(startTime, DateUtil.DF_YMD_NO_LINE_DEFAULT, DateUtil.DF_NORMAL), DateUtil.transform(endTime, DateUtil.DF_YMD_NO_LINE_DEFAULT, DateUtil.DF_NORMAL))
    }
  }

}
