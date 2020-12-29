package me.w1992wishes.structured.streaming.intellif.helper

import java.sql.Timestamp

import me.w1992wishes.common.util.DefaultUidGenerator
import me.w1992wishes.structured.streaming.intellif.domain._
import me.w1992wishes.structured.streaming.intellif.schema.Schema
import org.apache.spark.TaskContext
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

/**
  * @author w1992wishes 2020/12/18 14:32
  */
trait ClusterHelper extends ArchiveHelper {

  def parseEvent(spark: SparkSession, kafkaDF: DataFrame): Dataset[SimplifiedUnArchivedEvent] = {
    import org.apache.spark.sql.functions._
    import spark.implicits._
    kafkaDF
      .selectExpr("topic", "CAST(value AS string)").as[(String, String)]
      .select(from_json($"value", Schema.eventSchema).alias("value"))
      .select("value.*")
      .na.drop(Array("dataCode", "dataType"))
      .as[OriginalEvent]
      .dropDuplicates(Seq("bizCode", "dataCode", "dataType")) // 事件去重只保留一份
      .map(event => SimplifiedUnArchivedEvent(event.bizCode, event.dataCode, event.dataType.toUpperCase, event.sysCode, event.props))
  }

  def cluster(spark: SparkSession, eventDs: Dataset[SimplifiedUnArchivedEvent], archiveDs: Dataset[SimplifiedArchive], sysCode: String): Dataset[Archive] = {
    import spark.implicits._
    eventDs
      .join(archiveDs, Seq("biz_code", "data_code", "data_type"), "left")
      .filter("aid is null") // aid 关联不上为新增档案
      .as[SimplifiedArchivedEvent]
      .mapPartitions(it => {
        val uuid = new DefaultUidGenerator(TaskContext.getPartitionId() + 1)
        it.map(archivedEvent => {
          val archiveCreateTime = new Timestamp(System.currentTimeMillis())
          val archiveModifyTime = Timestamp.valueOf("9999-12-31 00:00:00")
          Archive(archivedEvent.biz_code, String.valueOf(uuid.getUID), archivedEvent.data_code, 0,
            archiveCreateTime, archiveModifyTime, sysCode, archivedEvent.props, archivedEvent.data_type)
        })
      })
  }

}
