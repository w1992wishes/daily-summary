package me.w1992wishes.spark.hive.intellif.conf

import com.typesafe.config.{Config, ConfigFactory, ConfigParseOptions, ConfigResolveOptions}

object ConfigArgsDrugRelated {

  val parent: Config = ConfigFactory.load("application.conf") // 父配置文件

  val configDrugRelated: Config = ConfigFactory.load("matrix-drug-related.conf",
    ConfigParseOptions.defaults,
    ConfigResolveOptions.defaults.setAllowUnresolved(true))

  val config: Config = parent
    .withFallback(configDrugRelated)
    .resolve()

  val bizCode: String = config.getString("parent.bizCode")

  val defaultDaysAgo: String =config.getString("drugRelated.analysis.default.daysAgo")


  //各规则权重
  val peerScoreWeight: String = config.getString("parent.task.hive.drugRelated.analysis.analysisResult.peerScoreWeight")
  val carScoreWeight: String = config.getString("parent.task.hive.drugRelated.analysis.analysisResult.carScoreWeight")
  val sameSiteScoreWeight: String = config.getString("parent.task.hive.drugRelated.analysis.analysisResult.sameSiteScoreWeight")
  val siteSateScoreWeight: String = config.getString("parent.task.hive.drugRelated.analysis.analysisResult.siteSateScoreWeight")
  val nocturnalScoreWeight: String = config.getString("parent.task.hive.drugRelated.analysis.analysisResult.nocturnalScoreWeight")


  val frequencyStartTime: String = config.getString("parent.task.hive.drugRelated.analysis.frequency.startTime")
  val hiveFrequencyPercent: String = config.getString("parent.task.hive.drugRelated.analysis.analysisResult.siteSateScoreWeight")
  val hiveFrequencySourceTable: String = config.getString("drugRelated.analysis.hive.frequency.source.table")
  val hiveFrequencyRelationTable: String = config.getString("drugRelated.analysis.hive.frequency.relation.table")
  val hiveFrequencyLabelTable: String = config.getString("drugRelated.analysis.hive.frequency.label.table")
  val hiveFrequencyHandleTable: String = config.getString("drugRelated.analysis.hive.frequency.handle.table")
  val hiveFrequencyResultTable: String = config.getString("parent.task.hive.drugRelated.analysis.frequency.result.table")

  val frequencyAggStartTime: String = config.getString("parent.task.hive.drugRelated.analysis.frequency.agg.startTime")
  val hiveFrequencyAggSourceTable: String = config.getString("drugRelated.analysis.hive.frequency.agg.source.table")
  val hiveFrequencyAggDeviceTable: String = config.getString("drugRelated.analysis.hive.frequency.agg.device.table")
  val hiveFrequencyAggLabelTable: String = config.getString("drugRelated.analysis.hive.frequency.agg.label.table")
  val hiveFrequencyAggHandleTable: String = config.getString("drugRelated.analysis.hive.frequency.agg.handle.table")
  val hiveFrequencyAggResultTable: String = config.getString("parent.task.hive.drugRelated.analysis.frequency.agg.result.table")

  //涉毒同行
  val hivePeerAnalysisAggResultTable: String = config.getString("parent.task.hive.drugRelated.analysis.peerAnalysisAgg.result.table")
  val hivePeerAnalysisAggSourceTable: String = config.getString("drugRelated.analysis.hive.peerAnalysisAgg.source.table")

  val hivePeerAnalysisResultTable: String = config.getString("parent.task.hive.drugRelated.analysis.peerAnalysis.result.table")
  val hivePeerAnalysisSourceTable: String = config.getString("drugRelated.analysis.hive.peerAnalysis.source.table")

  //涉毒同车
  val hiveCarAnalysisAggResultP2CTable: String = config.getString("parent.task.hive.drugRelated.analysis.carAnalysisAgg.result.p2c.table")
  val hiveCarAnalysisAggResultP2PTable: String = config.getString("parent.task.hive.drugRelated.analysis.carAnalysisAgg.result.p2p.table")
  val hiveCarAnalysisAggSourceP2CTable: String = config.getString("drugRelated.analysis.hive.carAnalysisAgg.source.p2c.table")
  val hiveCarAnalysisAggSourceP2PTable: String = config.getString("drugRelated.analysis.hive.carAnalysisAgg.source.p2p.table")

  val hiveCarAnalysisResultTable: String = config.getString("parent.task.hive.drugRelated.analysis.carAnalysis.result.table")
  val hiveCarAnalysisSourceP2CTable: String = config.getString("drugRelated.analysis.hive.carAnalysisAgg.source.p2c.table")
  val hiveCarAnalysisSourceP2PTable: String = config.getString("drugRelated.analysis.hive.carAnalysisAgg.source.p2p.table")

  val hiveEntryExitAnalysisDailyLabelSiteSql: String = config.getString("drugRelated.analysis.hive.sameEntryExitAnalysisDaily.source.labelSite.sql")
  val hiveEntryExitAnalysisDailyLabelPersonSql: String = config.getString("drugRelated.analysis.hive.sameEntryExitAnalysisDaily.source.labelPerson.sql")
  val hiveEntryExitAnalysisDailyEventSql: String = config.getString("drugRelated.analysis.hive.sameEntryExitAnalysisDaily.source.event.sql")
  val hiveEntryExitAnalysisDailyResultTable: String = config.getString("parent.task.hive.drugRelated.analysis.sameEntryExitAnalysisDaily.result.table")

  val hiveEntryExitAnalysisAggSourceSql: String = config.getString("drugRelated.analysis.hive.sameEntryExitAnalysisAgg.source.sql")
  val hiveEntryExitAnalysisAggResultTable: String = config.getString("parent.task.hive.drugRelated.analysis.sameEntryExitAnalysisAgg.result.table")

  val hiveEntryExitAnalysisSourceSql: String = config.getString("drugRelated.analysis.hive.sameEntryExitAnalysis.source.sql")
  val hiveEntryExitAnalysisResultTable: String = config.getString("parent.task.hive.drugRelated.analysis.sameEntryExitAnalysis.result.table")

  //昼伏夜出分析
  val hiveNocturnalAnalysisSourceLabelPersonSql: String = config.getString("drugRelated.analysis.hive.nocturnalAnalysisAgg.source.labelPerson.sql")
  val hiveNocturnalAnalysisAggSourceEventSql: String = config.getString("drugRelated.analysis.hive.nocturnalAnalysisAgg.source.event.sql")
  val hiveNocturnalAnalysisAggResultTable: String = config.getString("parent.task.hive.drugRelated.analysis.nocturnalAnalysisAgg.result.table")
  val hiveNocturnalAnalysisAggDayPeriods: String = config.getString("parent.task.hive.drugRelated.analysis.nocturnalAnalysisAgg.dayPeriods")
  val hiveNocturnalAnalysisAggNightPeriods: String = config.getString("parent.task.hive.drugRelated.analysis.nocturnalAnalysisAgg.nightPeriods")
  val hiveNocturnalAnalysisAggDayNightMultiple: String = config.getString("parent.task.hive.drugRelated.analysis.nocturnalAnalysisAgg.dayNightMultiple")

  //昼伏夜出聚合
  val hiveNocturnalAnalysisSourceSql: String = config.getString("drugRelated.analysis.hive.nocturnalAnalysis.source.sql")
  val hiveNocturnalAnalysisResultTable: String = config.getString("parent.task.hive.drugRelated.analysis.nocturnalAnalysis.result.table")

  //各规则分数合并
  val hiveAnalysisResultSourceTable: String = config.getString("drugRelated.analysis.hive.analysisResult.source.table")
  val hiveAnalysisResultResultTable: String = config.getString("parent.task.hive.drugRelated.analysis.analysisResult.result.table")

  //各规则合并,供接口使用
  val hiveDrugRelatedInterfaceSourcePeerDrugRelated: String = config.getString("drugRelated.analysis.hive.analysisResultInterface.source.peerDrugRelated")
  val hiveDrugRelatedInterfaceSourceCarDrugRelated: String = config.getString("drugRelated.analysis.hive.analysisResultInterface.source.carDrugRelated")
  val hiveDrugRelatedInterfaceSourceFreqPlaceDrugRelated: String = config.getString("drugRelated.analysis.hive.analysisResultInterface.source.freqPlaceDrugRelated")
  val hiveDrugRelatedInterfaceSourceConPlaceDrugRelated: String = config.getString("drugRelated.analysis.hive.analysisResultInterface.source.conPlaceDrugRelated")
  val hiveDrugRelatedInterfaceSourceNightAndDazedDrugRelated: String = config.getString("drugRelated.analysis.hive.analysisResultInterface.source.nightAndDazedDrugRelated")
  val hiveDrugRelatedInterfaceSourceDrugRelated: String = config.getString("drugRelated.analysis.hive.analysisResultInterface.source.drugRelated")
  val hiveAnalysisResultInterfaceResultTable: String = config.getString("parent.task.hive.drugRelated.analysis.analysisResultInterface.result.table")


  def main(args: Array[String]): Unit = {
    println(hiveAnalysisResultInterfaceResultTable)
  }
}