package me.w1992wishes.spark.arangodb.hive2arangodb

import com.arangodb.ArangoDB
import com.arangodb.spark.WriteOptions
import me.w1992wishes.common.analysis.{ArangoAnalysis, HiveAnalysis}
import me.w1992wishes.common.conf.{ConfigArgsGlobal, ParamOptions, ParamParser}
import me.w1992wishes.common.util.{DateUtil, Log}
import me.w1992wishes.spark.arangodb.conf.{ConfigArgs, ConfigArgsHive2Arango}
import org.apache.spark.SparkConf
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SparkSession}

object ExportHive2Arango extends HiveAnalysis with ArangoAnalysis with ConfigArgsGlobal with Log{

  def main(args: Array[String]): Unit = {
    if (args.length % 2 != 0){
      logError("参数有误，请检查！")
      System.exit(1)
    }

    // 参数配置准备
    val params = ParamParser(args).getConf

    //bizcode
    val bizCode = params.getProperty(ParamOptions.BIZ_CODE, ConfigArgs.bizCode)
    
    //获取关键字
    val key = params.getProperty(ParamOptions.ARANGODB_KEY)

    //分区数量
    val partitionNum = ConfigArgsHive2Arango.matrixExportHive2ArangoSourcePartitionNum(key).toInt

    //数据源
    val sourceTableTmp = ConfigArgsHive2Arango.matrixExportHive2ArangoSourceTable(key)

    logInfos("startTimeGlobal : " + startTimeGlobal,
      "endTimeGlobal : " + endTimeGlobal
    )

    val (endTime, startTime) = if (startTimeGlobal.equals("") || endTimeGlobal.equals("")) {
      //分析数据时间范围的结束时间 dt  如果是dt 时间格式是yyyyMMdd 否则时间格式yyyy-MM-dd HH:mm:ss
      if(sourceTableTmp.contains("dt >= '#start_time#'") ||
        sourceTableTmp.contains("dt>='#start_time#'")||
        sourceTableTmp.contains("dt >='#start_time#'")||
        sourceTableTmp.contains("dt>= '#start_time#'")
      ){
        //分析数据时间范围的结束时间，取传入结束时间，如果不传参数则默认为当天日期
        val endTime = params.getProperty(ParamOptions.END_TIME, DateUtil.today(DateUtil.DF_YMD_NO_LINE))
        //计算范围，当前时间的多少天之前
        val daysAgo = params.getProperty(ParamOptions.DAYS_AGO, ConfigArgsHive2Arango.defaultDaysAgo).toInt
        val dateAgo = DateUtil.dateAgo(endTime, daysAgo, DateUtil.DF_YMD_NO_LINE, DateUtil.DF_YMD_NO_LINE)

        //分析数据时间范围的起始时间，如果设置了ago天数，则开始时间end_time-ago,否则必须设置开始时间，如果没有设置开始时间，默认是计算90天的数据
        val startTime = if (params.getProperty(ParamOptions.DAYS_AGO) == null) {
          params.getProperty(ParamOptions.START_TIME, dateAgo)
        } else {
          dateAgo
        }
        (endTime, startTime)
      }else {
        //分析数据时间范围的结束时间，取传入结束时间，如果不传参数则默认为当天日期
        val endTimeTmp = params.getProperty(ParamOptions.END_TIME, DateUtil.today(DateUtil.DF_YMD_NO_LINE))
        val endTime = DateUtil.dateEndSecond(endTimeTmp, DateUtil.DF_YMD_NO_LINE)
        //计算范围，当前时间的多少天之前
        val daysAgo = params.getProperty(ParamOptions.DAYS_AGO, ConfigArgsHive2Arango.defaultDaysAgo).toInt
        val dateAgo = DateUtil.dateAgo(endTimeTmp, daysAgo, DateUtil.DF_YMD_NO_LINE, DateUtil.DF_YMD_NO_LINE)

        //分析数据时间范围的起始时间，如果设置了ago天数，则开始时间end_time-ago,否则必须设置开始时间，如果没有设置开始时间，默认是计算90天的数据
        val startTimeTmp = if (params.getProperty(ParamOptions.DAYS_AGO) == null) {
          params.getProperty(ParamOptions.START_TIME, dateAgo)
        } else {
          dateAgo
        }
        val startTime = DateUtil.transform(startTimeTmp, DateUtil.DF_YMD_NO_LINE, DateUtil.DF_NORMAL)
        (endTime, startTime)
      }
    }else{
      if(sourceTableTmp.contains("dt >= '#start_time#'") ||
        sourceTableTmp.contains("dt>='#start_time#'")||
        sourceTableTmp.contains("dt >='#start_time#'")||
        sourceTableTmp.contains("dt>= '#start_time#'")
      ){
        (endTimeGlobal, startTimeGlobal)
      }else{
        val startTime = DateUtil.transform(startTimeGlobal, DateUtil.DF_YMD_NO_LINE, DateUtil.DF_NORMAL)
        val endTime = DateUtil.transform(endTimeGlobal, DateUtil.DF_YMD_NO_LINE, DateUtil.DF_NORMAL)
        (endTime, startTime)
      }
    }

    val sourceTable = sourceTableTmp.replace("#biz_code#", bizCode).replace("#start_time#", startTime).replace("#end_time#", endTime)
    //是否清空集合
    val clear = params.getProperty(ParamOptions.CLEAR, ConfigArgsHive2Arango.matrixExportHive2ArangoResultClear(key)).toBoolean
    //是否upsert
    val upsert = params.getProperty(ParamOptions.UPSERT, ConfigArgsHive2Arango.matrixExportHive2ArangoResultUpsert(key)).toBoolean
    //操作类型
    val type_ = params.getProperty(ParamOptions.ARANGODB_RESULT_TYPE, ConfigArgsHive2Arango.matrixExportHive2ArangoResultType(key))
    //keyField
    val keyField = params.getProperty(ParamOptions.ARANGODB_RESULT_KEY_FIELD, ConfigArgsHive2Arango.matrixExportHive2ArangoResultKeyField(key))
    //arangoDB集合名
    val collectionName = params.getProperty(ParamOptions.ARANGODB_RESULT_COLLECTION_NAME, ConfigArgsHive2Arango.matrixExportHive2ArangoResultCollectionName(key)).replace("#biz_code#", bizCode)
    //arango信息
    val arangoHost = ConfigArgs.arangoHost
    val arangoPort = ConfigArgs.arangoPort
    val arangoUser = ConfigArgs.arangoUser
    val arangoPwd = ConfigArgs.arangoPwd
    val arangodb = ConfigArgs.arangoDb.replace("#biz_code#", bizCode)
    val arangoMaxConnections = ConfigArgs.arangoMaxConnections.toInt
    val writeOptions = WriteOptions(arangodb, Option(arangoHost + ":" + arangoPort), Option(arangoUser), Option(arangoPwd), maxConnections = Option(arangoMaxConnections))

    val conf: SparkConf = new SparkConf()
    conf.set("spark.io.compression.codec", "snappy")
    val spark = SparkSession.builder().appName(getClass.getName).config(conf).enableHiveSupport.getOrCreate()
    spark.sparkContext.setLogLevel("INFO")

    //打印日志，方便排错
    logInfos("bizCode : " + bizCode,
      "key : " + key,
      "startTime : " + startTime,
      "endTime : " + endTime,
      "sourceTableTmp : " + sourceTable,
      "clear : " + clear,
      "upsert : " + upsert,
      "type_ : " + type_,
      "keyField : " + keyField,
      "collectionName : " + collectionName,
      "arangoHost : " + arangoHost,
      "arangoPort : " + arangoPort,
      "arangoUser : " + arangoUser,
      "arangoPwd : " + arangoPwd,
      "arangodb : " + arangodb,
      "arangoMaxConnections : " + arangoMaxConnections
    )

    //加载hive数据
    val sourceDF = type_ match {
      case "0" =>
        loadFromHive(spark, sourceTable).withColumnRenamed(keyField, "_key").repartition(partitionNum, col("_key"))
      case "1" =>
        loadFromHive(spark, sourceTable).withColumnRenamed(keyField, "_key").withColumnRenamed("from", "_from").withColumnRenamed("to", "_to").repartition(partitionNum, col("_key"))
    }

    //写数据到arango
    if(clear){
      val server = new ArangoDB.Builder().host(arangoHost, arangoPort.toInt).user(arangoUser).password(arangoPwd).build
      saveToArango(sourceDF, server, arangodb, collectionName, writeOptions, upsert)
    }else {
      saveToArango(sourceDF, collectionName, writeOptions, upsert)
    }

  }

  override def handle(params: Any*): DataFrame = ???
}
