package me.w1992wishes.structured.streaming.intellif.task

import java.time.{LocalDateTime, ZoneOffset}

import me.w1992wishes.common.util.{Log, ZkKafkaOffset}
import me.w1992wishes.structured.streaming.intellif.conf.{ConfigArgs, ConfigArgsDynamicStream}
import me.w1992wishes.structured.streaming.intellif.domain.{Archive, SimplifiedArchive, SimplifiedUnArchivedEvent}
import me.w1992wishes.structured.streaming.intellif.helper.{ClusterHelper, OffsetHelper, SparkHelper}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.apache.spark.sql.streaming.{OutputMode, StreamingQuery, Trigger}
import org.elasticsearch.hadoop.cfg.ConfigurationOptions

/**
  * @author w1992wishes 2020/12/24 10:41
  */
object ClusterStream extends ClusterHelper with SparkHelper with OffsetHelper with Log {

  def main(args: Array[String]): Unit = {
    val config = new ConfigArgsDynamicStream

    val groupId: String = config.clusterStreamGroupId
    val topics: String = config.streamCommonKafkaEventTopic
    val zkHosts: String = ConfigArgs.zkHosts
    val esNode: String = ConfigArgs.esNodes
    val esPort: String = ConfigArgs.esPort
    val sysCode: String = config.clusterStreamSystemCode
    val hiveSourceArchiveSql: String = config.streamCommonHiveArchiveSql
    val startTimeMils = String.valueOf(LocalDateTime.now().minusDays(3).toInstant(ZoneOffset.of("+8")).toEpochMilli)
    val endTimeMils = String.valueOf(LocalDateTime.now().plusDays(3).toInstant(ZoneOffset.of("+8")).toEpochMilli)
    val esSourceQuery = config.streamCommonEsArchiveQuery.replace("#start_time_mills#", startTimeMils).replace("#end_time_mills#", endTimeMils)
    val esArchiveSource = s"${config.streamCommonEsArchiveIndex}/${config.streamCommonEsArchiveType}"
    // init ZkKafkaOffset
    val zkKafkaOffset: ZkKafkaOffset = ZkKafkaOffset(zkHosts, topics, groupId)
    zkKafkaOffset.initOffset()

    logInfo(s"groupId: $groupId")
    logInfo(s"topics: $topics")
    logInfo(s"zkHosts: $zkHosts")
    logInfo(s"streamCommonHiveArchiveSql: ${config.streamCommonHiveArchiveSql}")
    logInfo(s"streamCommonEsArchiveQuery: $esSourceQuery")
    logInfo(s"clusterStreamEsArchiveSource: $esArchiveSource")

    val sparkConf = new SparkConf()
      .setIfMissing("spark.master", "local[12]")
      .set("hive.exec.dynamic.partition.mode", "nonstrict")
      .set("hive.exec.dynamic.partition", "true")
      .setAppName(config.clusterStreamName)
      .set(ConfigurationOptions.ES_NODES, esNode)
      .set(ConfigurationOptions.ES_PORT, esPort)
      .set(ConfigurationOptions.ES_NODES_WAN_ONLY, "true")
    val spark = buildSpark(sparkConf)
    // 设置日志级别
    spark.sparkContext.setLogLevel(ConfigArgs.logLevel)
    import spark.implicits._

    // static archive
    var archiveDs: Dataset[SimplifiedArchive] = loadArchive(spark, hiveSourceArchiveSql, esArchiveSource, esSourceQuery)

    val kafkaDF: DataFrame = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", ConfigArgs.kafkaBootstrapServers)
      .option("subscribe", config.streamCommonKafkaEventTopic)
      .option("startingOffsets", "earliest")
      .load()
    // unarchived stream event
    val eventDs: Dataset[SimplifiedUnArchivedEvent] = parseEvent(spark: SparkSession, kafkaDF)

    // cluster
    val resultDs: Dataset[Archive] = cluster(spark, eventDs, archiveDs, sysCode)

    // new archive
    val newArchive: Dataset[SimplifiedArchive] = resultDs
      .map(e => SimplifiedArchive(e.biz_code, e.aid, e.data_type, e.data_code))

    // update static archive
    archiveDs = archiveDs.union(newArchive)

    // 指定参数根据指定ID更新写入ES
    val esOptions = Map(
      "es.write.operation" -> "upsert",
      "es.mapping.id" -> "aid"
    )
    spark.streams.addListener(new KafkaOffsetListener(zkKafkaOffset))
    val query1: StreamingQuery = resultDs
      .writeStream.options(esOptions)
      .outputMode(OutputMode.Append())
      .trigger(Trigger.ProcessingTime(30000))
      .format("org.elasticsearch.spark.sql")
      .option("checkpointLocation", config.clusterStreamCheckpoint)
      .start(s"${config.streamCommonEsArchiveIndex}/${config.streamCommonEsArchiveType}")

    /*    val query2: StreamingQuery = resultDs
          .writeStream.options(esOptions)
          .outputMode(OutputMode.Append())
          .trigger(Trigger.ProcessingTime(30000))
          .format("org.elasticsearch.spark.sql")
          .option("checkpointLocation", config.clusterStreamCheckpoint)
          .start(s"${config.streamCommonEsArchiveIndex}/${config.streamCommonEsArchiveType}")*/


    query1.awaitTermination()
    //query2.awaitTermination()
  }

}
