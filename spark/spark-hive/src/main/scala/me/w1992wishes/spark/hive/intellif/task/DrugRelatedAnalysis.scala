package me.w1992wishes.spark.hive.intellif.task

import java.util.Properties

import me.w1992wishes.common.conf.{ConfigArgsGlobal, ParamOptions}
import me.w1992wishes.common.util.{DateUtil, Log}
import org.apache.commons.lang3.StringUtils

/**
  * @author w1992wishes 2020/11/30 10:28
  */
trait DrugRelatedAnalysis extends ConfigArgsGlobal with Log {

  def getQueryTime(params: Properties): (String, String) = {
    var (startTime, endTime) = (startTimeGlobal, endTimeGlobal)
    if (StringUtils.isEmpty(endTimeGlobal)) {
      // hvie dt 时间格式是yyyyMMdd , gp create_time 是 yyyy-MM-dd HH:mm:ss
      endTime = params.getProperty(ParamOptions.END_TIME, DateUtil.today(DateUtil.DF_YMD_NO_LINE))
    }

    if (StringUtils.isEmpty(startTimeGlobal)) {
      startTime = params.getProperty(ParamOptions.START_TIME,
        DateUtil.dateAgo(endTime, params.getProperty(ParamOptions.DAYS_AGO, "1").toInt, DateUtil.DF_YMD_NO_LINE_DEFAULT, DateUtil.DF_YMD_NO_LINE))
    }

    (startTime, endTime)
  }

}
