package me.w1992wishes.spark.hive.intellif.task

import java.sql.{Date, Timestamp}
import java.time.temporal.ChronoUnit
import java.util.UUID

import com.alibaba.fastjson.JSONObject
import me.w1992wishes.common.util.{DateUtil, PropertiesTool}
import me.w1992wishes.spark.hive.intellif.param.ArchivePersonGp2MidTransferCLParam
import org.apache.commons.lang3.StringUtils
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, Row, SaveMode, SparkSession}

/**
  * 全量提取并覆盖
  *
  * @author w1992wishes 2019/11/25 15:01
  */
object ArchivePersonGp2MidTransferTask {

  def main(args: Array[String]): Unit = {
    args.foreach(println(_))

    val clParam = ArchivePersonGp2MidTransferCLParam(args)
    val propsTool = PropertiesTool(clParam.confName)

    // gp 中档案表 create_time 为当前插入时间（凌晨运行），是从昨天的抓拍中聚出来的档案，所以从该任务想抽出昨天新增的档案，需取当天的 create_time
    val startTime = DateUtil.strToDateTime(clParam.dt.concat("000000"), DateUtil.DF_NORMAL_NO_LINE).truncatedTo(ChronoUnit.DAYS).plusDays(1)
    val endTime = startTime.plusDays(1)
    val startTimeStr = if (StringUtils.isNotEmpty(clParam.startTime)) DateUtil.dateTimeToStr(DateUtil.strToDateTime(clParam.startTime, DateUtil.DF_NORMAL_NO_LINE)) else DateUtil.dateTimeToStr(startTime)
    val endTimeStr = if (StringUtils.isNotEmpty(clParam.endTime)) DateUtil.dateTimeToStr(DateUtil.strToDateTime(clParam.endTime, DateUtil.DF_NORMAL_NO_LINE)) else DateUtil.dateTimeToStr(endTime)
    println(startTimeStr)
    println(endTimeStr)

    // 命令行参数
    val bizCode = clParam.bizCode
    val debug = clParam.debug
    val partition = clParam.numPartitions
    val archiveMidTable = s"${bizCode}_mid.mid_${bizCode}_archive_multi"
    val updateArchiveMidTable = s"${bizCode}_mid.mid_${bizCode}_archive_multi_update"
    // location
    val archiveMidTableDir = s"/user/hive/warehouse/mid_${bizCode}_archive_multi/archive"
    // archiveMidTableDataDir = s"$archiveMidTableDir/data_type=PERSON" // 实体保存位置
    val updateArchiveMidTableDir = s"/user/hive/warehouse/mid_${bizCode}_archive_multi/archive_update"
    val updateArchiveMidDataDir = s"$updateArchiveMidTableDir/data_type=PERSON"

    // spark 参数设置
    val conf = new SparkConf()
      .setIfMissing("spark.master", "local[16]")
      .set("spark.sql.adaptive.enabled", "true")
      .set("spark.sql.adaptive.shuffle.targetPostShuffleInputSize", "67108864b")
      .set("spark.sql.adaptive.join.enabled", "true")
      .set("spark.sql.autoBroadcastJoinThreshold", "20971520")
      .set("spark.sql.broadcastTimeout", "600000ms")
      .set("spark.hadoopRDD.ignoreEmptySplits", "true")
      .set("spark.debug.maxToStringFields", "100")
      .set("spark.sql.warehouse.dir", "hdfs://nameservice1/user/hive/warehouse")
      .set("spark.sql.catalogImplementation", "hive")

    // 初始化 spark
    val spark = SparkSession.builder()
      .appName(getClass.getSimpleName)
      .enableHiveSupport() // 启用 hive
      .config(conf)
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    // 1.从 gp 加载档案
    val personDF = getArchivesFromGp(spark, partition, propsTool, startTimeStr, endTimeStr)
    println("加载 gp 的档案")
    personDF.show(false)

    // 2.写入临时更新表中，方便发送到 es
    import spark.implicits._
    personDF.map { row => {
      val biz_code = bizCode
      val aid = UUID.randomUUID().toString.replace("-", "")
      val data_code = row.getAs[String]("aid")
      val status = row.getAs[Int]("status")
      val create_time = row.getAs[Timestamp]("create_time")
      val modify_time = Timestamp.valueOf(DateUtil.strToDateTime("9999-12-31 00:00:00"))
      val sys_code = "SkyNet"
      val props = formatProps(row)
      MidArchivePerson(biz_code, aid, data_code, status.toByte, create_time, modify_time, sys_code, props)
    }
    }.write.mode(SaveMode.Overwrite).parquet(s"hdfs://nameservice1$updateArchiveMidDataDir")
    import spark.sql
    sql(s"CREATE DATABASE IF NOT EXISTS ${bizCode}_mid")
    sql(s"USE ${bizCode}_mid")
    sql(
      s"""
         | CREATE TABLE IF NOT EXISTS $updateArchiveMidTable(
         |  biz_code string,
         |  aid string,
         |  data_code string,
         |  status tinyint,
         |  create_time timestamp,
         |  modify_time timestamp,
         |  sys_code string,
         |  props string
         |  )
         | PARTITIONED BY (data_type string comment "按类型分区")
         | STORED AS PARQUET
         | LOCATION 'hdfs://nameservice1$updateArchiveMidTableDir'
      """.stripMargin)
    // load
    sql(s"ALTER TABLE $updateArchiveMidTable DROP IF EXISTS PARTITION(data_type ='PERSON')")
    sql(s"ALTER TABLE $updateArchiveMidTable ADD PARTITION(data_type ='PERSON') LOCATION '$updateArchiveMidDataDir'")
    println("临时档案表")
    spark.sql(s"SELECT * FROM $updateArchiveMidTable").show()

    // 3.增量插入档案数仓
    sql(
      s"""
         | CREATE TABLE IF NOT EXISTS $archiveMidTable(
         |  biz_code string,
         |  aid string,
         |  data_code string,
         |  status tinyint,
         |  create_time timestamp,
         |  modify_time timestamp,
         |  sys_code string,
         |  props string
         |  )
         | PARTITIONED BY (data_type string comment "按类型分区")
         | STORED AS PARQUET
         | LOCATION 'hdfs://nameservice1$archiveMidTableDir'
      """.stripMargin)
    sql(
      s"""
         | INSERT INTO TABLE $archiveMidTable PARTITION(data_type='PERSON')
         | SELECT
         |   biz_code,
         |   aid,
         |   data_code,
         |   status,
         |   create_time,
         |   modify_time,
         |   sys_code,
         |   props
         | FROM $updateArchiveMidTable WHERE data_type = 'PERSON'
       """.stripMargin)
    println("增量插入全量档案表成功")

    spark.stop()
  }

  private def formatProps(row: Row): String = {
    val person_name = row.getAs[String]("person_name")
    val marriage_status = row.getAs[Int]("marriage_status")
    val nation = row.getAs[String]("nation")
    val height = row.getAs[String]("height")
    val current_residential_address = row.getAs[String]("current_residential_address")
    val birthday = row.getAs[Date]("birthday")
    val age = row.getAs[String]("age")
    val avatar_url = row.getAs[String]("avatar_url")
    val thumbnail_url = row.getAs[String]("thumbnail_url")
    val image_url = row.getAs[String]("image_url")
    val image_id = row.getAs[String]("image_id")
    val image_type = row.getAs[String]("image_type")
    val target_rect = row.getAs[String]("target_rect")
    val gender = row.getAs[Int]("gender")
    val occupation = row.getAs[String]("occupation")
    val highest_degree = row.getAs[String]("highest_degree")
    val identity_no = row.getAs[String]("identity_no")
    val phone_no = row.getAs[String]("phone_no")
    val email = row.getAs[String]("email")
    val source_type = row.getAs[String]("source_type")
    val mark_info = row.getAs[String]("mark_info")
    val column1 = row.getAs[String]("column1")
    val column2 = row.getAs[String]("column2")
    val column3 = row.getAs[String]("column3")
    val time = row.getAs[Timestamp]("time")
    val archive_type = row.getAs[Int]("archive_type")
    val json = new JSONObject()
    json.put("person_name", person_name)
    json.put("marriage_status", marriage_status)
    json.put("nation", nation)
    json.put("height", height)
    json.put("current_residential_address", current_residential_address)
    json.put("birthday", birthday)
    json.put("age", age)
    json.put("avatar_url", avatar_url)
    json.put("thumbnail_url", thumbnail_url)
    json.put("image_url", image_url)
    json.put("image_id", image_id)
    json.put("image_type", image_type)
    json.put("target_rect", target_rect)
    json.put("gender", gender)
    json.put("occupation", occupation)
    json.put("highest_degree", highest_degree)
    json.put("identity_no", identity_no)
    json.put("phone_no", phone_no)
    json.put("email", email)
    json.put("source_type", source_type)
    json.put("mark_info", mark_info)
    json.put("column1", column1)
    json.put("column2", column2)
    json.put("column3", column3)
    json.put("time", time)
    json.put("archive_type", archive_type)
    json.toJSONString
  }

  private def getArchivesFromGp(spark: SparkSession, partitions: Int, propsTool: PropertiesTool, startTimeStr: String, endTimeStr: String): DataFrame = {
    val archiveParalleledCondition = getArchiveParalleledCondition(partitions)
    val table = propsTool.getString("source.table")
    Range(0, partitions)
      .map(index => {
        val dbtable = archiveTable(table, index, archiveParalleledCondition, startTimeStr, endTimeStr)
        println(dbtable)
        spark
          .read
          .format("jdbc")
          .option("driver", propsTool.getString("source.driver"))
          .option("url", propsTool.getString("source.url"))
          .option("dbtable", dbtable)
          .option("user", propsTool.getString("source.user"))
          .option("password", propsTool.getString("source.pass"))
          .option("fetchsize", propsTool.getInt("source.fetchsize"))
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
  }

  private def getArchiveParalleledCondition(realPartitions: Int): Array[String] = {
    Range(0, realPartitions).map(partition => s"CAST(hash_code(archive_id) as numeric) % $realPartitions = $partition").toArray
  }

  private def archiveTable(sourceTable: String, index: Int, paralleledCondition: Array[String], startTimeStr: String, endTimeStr: String): String = {
    s"""(SELECT
       |  archive_id AS aid,
       |  person_name,
       |  marriage_status,
       |  nation,
       |  height,
       |  current_residential_address,
       |  birthday,
       |  age,
       |  avatar_url,
       |  thumbnail_url,
       |  image_url,
       |  image_id,
       |  image_type,
       |  target_rect,
       |  gender,
       |  occupation,
       |  highest_degree,
       |  identity_no,
       |  phone_no,
       |  email,
       |  source_type,
       |  mark_info,
       |  column1,
       |  column2,
       |  column3,
       |  time,
       |  create_time,
       |  update_time AS modify_time,
       |  delete_flag AS status,
       |  archive_type
       | FROM $sourceTable WHERE ${paralleledCondition(index)} AND create_time > '$startTimeStr' AND create_time <= '$endTimeStr') AS t_tmp_$index""".stripMargin
  }

  case class MidArchivePerson(biz_code: String, aid: String, data_code: String, status: Byte, create_time: Timestamp, modify_time: Timestamp, sys_code: String, props: String)

}

