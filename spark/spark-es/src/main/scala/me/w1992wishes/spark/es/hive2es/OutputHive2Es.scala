package me.w1992wishes.spark.es.hive2es

import me.w1992wishes.common.analysis.HiveAnalysis
import me.w1992wishes.common.conf.{ParamOptions, ParamParser}
import me.w1992wishes.common.util.Log
import me.w1992wishes.spark.es.conf.{ConfigArgs, ConfigArgsHive2Es}
import org.apache.commons.lang3.StringUtils
import me.w1992wishes.common.enhance.StrEnhance._
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}

object OutputHive2Es extends Hive2es with HiveAnalysis with Log {

  def main(args: Array[String]): Unit = {
    // 参数配置准备
    val params = ParamParser(args).getConf

    val esKey = params.getProperty(ParamOptions.ES_KEY)

    if (StringUtils.isEmpty(esKey)) {
      logError("esKey can not be null")
      System.exit(1)
    }

    val bizCode = params.getProperty(ParamOptions.BIZ_CODE, ConfigArgs.bizCode)
    val shufflePartition = params.getProperty(ParamOptions.SHUFFLE_PARTITION, ConfigArgs.shufflePartition)
    val dataType = params.getProperty(ParamOptions.DATA_TYPE,"")

    // sql
    val sql = ConfigArgsHive2Es.hive2esSourceSql(esKey)

    val (startTime: String, endTime: String) = getQueryTime(params, sql)

    //源数据
    val sourceSql = sql
      .replaceNotNull("#start_time#", startTime)
      .replaceNotNull("#end_time#", endTime)
      .replaceNotNull("#biz_code#", bizCode)
      .replaceNotNull("#data_type#", dataType)

    //结果表名称
    val index = ConfigArgsHive2Es.hive2esResultIndex(esKey).replaceLower("#data_type#", dataType).replaceNotNull("#biz_code#", bizCode)
    val type_ = ConfigArgsHive2Es.hive2esResultType(esKey).replaceLower("#data_type#", dataType).replaceNotNull("#biz_code#", bizCode)

    val esSaveMode = params.getProperty(ParamOptions.ES_SAVE_MODE, ConfigArgsHive2Es.hive2esResultSaveMode(esKey))
    val esWriteOperation = params.getProperty(ParamOptions.ES_WRITE_OPERATION, ConfigArgsHive2Es.hive2esResultWriteOperation(esKey))
    val esMappingId = params.getProperty(ParamOptions.ES_MAPPING_ID, ConfigArgsHive2Es.hive2esResultMappingId(esKey))

    val expandSourceId = params.getProperty(ParamOptions.EXPAND_SOURCE_ID, "false").toBoolean

    logInfo("-------- param --------")
    logInfo("bizCode : " + bizCode)
    logInfo("shufflePartition : " + shufflePartition)
    logInfo("sourceSql : " + sourceSql)
    logInfo("index : " + index)
    logInfo("type_ : " + type_)
    logInfo("esWriteOperation : " + esWriteOperation)
    logInfo("esMappingId : " + esMappingId)
    logInfo("esKey : " + esKey)
    logInfo("esSaveMode : " + esSaveMode)
    logInfo("nodes : " + ConfigArgs.esNodes)
    logInfo("port : " + ConfigArgs.esPort)

    val conf: SparkConf = new SparkConf()
      .setIfMissing("spark.master", "local[16]")
      .set("spark.io.compression.codec", "snappy")
      .set("spark.sql.shuffle.partitions", shufflePartition)
      .set("es.nodes", ConfigArgs.esNodes)
      .set("es.port", ConfigArgs.esPort)
      .set("es.nodes.wan.only", ConfigArgs.esNodesWanOnly)
      .set("es.index.auto.create", ConfigArgs.esIndexAutoCreate)
      .set("es.mapping.rich.date", ConfigArgs.esMappingRichDate)
      .set("es.write.operation", esWriteOperation)
      .set("es.batch.write.retry.count", ConfigArgs.esBatchWriteRetryCount) // 默认是重试3次,为负值的话为无限重试(慎用)
      .set("es.batch.write.retry.wait", ConfigArgs.esBatchWriteRetryWait) // 默认重试等待时间是10s.可适当加大
      .set("es.http.timeout", ConfigArgs.esHttpTimeout) // 连接es的超时时间设置,默认1m,Connection error时可适当加大此参数
    if (esMappingId != null) {
      conf.set("es.mapping.id", esMappingId) // 唯一 id
    }

    val spark = SparkSession.builder().appName(getClass.getName).enableHiveSupport().config(conf).getOrCreate()
    spark.sparkContext.setLogLevel(ConfigArgs.logLevel)

    val sourceDF = loadFromHive(spark, sourceSql)

    val map = Map("spark" -> spark,
      "sourceDF" -> sourceDF,
      "expandSourceId" -> expandSourceId
    )
    val result: DataFrame = handle(map)

    saveToES(result, s"$index/$type_", esSaveMode, "org.elasticsearch.spark.sql")

    spark.stop()
  }


}
