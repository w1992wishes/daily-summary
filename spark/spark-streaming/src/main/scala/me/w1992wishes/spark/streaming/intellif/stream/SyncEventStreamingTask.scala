package me.w1992wishes.spark.streaming.intellif.stream

import java.time.LocalDateTime

import me.w1992wishes.common.util.DateUtil
import me.w1992wishes.spark.streaming.intellif.conf.{ConfigArgs, ConfigArgsStreaming}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.HasOffsetRanges

/**
  * @author w1992wishes 2020/11/16 14:53
  */
class SyncEventStreamingTask(config: Map[String, String]) extends StreamingTask(config: Map[String, String]) {
  override def handle(params: Any*): DataFrame = null
}

object SyncEventStreamingTask {
  def main(args: Array[String]): Unit = {

    val config = new ConfigArgsStreaming

    val streamingMap = Map("kafka.group.id" -> config.streamingSyncEventMultiKafkaGroupId,
      "kafka.input.topic" -> config.streamingSyncEventMultiKafkaInputTopic,
      "kafka.input.servers" -> config.streamingSyncEventMultiKafkaInputServers,
      "kafka.receive.buffer.bytes" -> config.streamingSyncEventMultiKafkaReceiveBuffer,
      "kafka.max.partition.fetch.bytes" -> config.streamingSyncEventMultiKafkaMaxFetch,
      "kafka.auto.offset.reset" -> config.streamingSyncEventMultiKafkaOffsetReset,
      "kafka.enable.auto.commit" -> config.streamingSyncEventMultiKafkaAutoCommit,
      "streaming.zk.servers" -> config.streamingSyncEventMultiZkServers,
      "spark.streaming.kafka.maxRatePerPartition" -> config.streamingSyncEventMultiKafkaMaxRatePerPartition,
      "spark.log.level" -> ConfigArgs.logLevel,
      "streaming.task.name" -> config.streamingSyncEventMultiName,
      "streaming.task.batch.duration" -> config.streamingSyncEventMultiBatchDuration
    )

    val task = new SyncEventStreamingTask(streamingMap)
    val stream: InputDStream[ConsumerRecord[String, String]] = task.createStream()

    // 消费数据
    val spark = SparkSession.builder().config(task.ssc.sparkContext.getConf).getOrCreate()
    import spark.implicits._
    val resultUrl = config.streamingSyncEventMultiResultUrl
    val resultTable = config.streamingSyncEventMultiResultTable
    val dbUser = "gpadmin"
    val dbPassword = "gpadmin"
    val dbDriver = "org.postgresql.Driver"
    stream
      .foreachRDD(kafkaRdd => {
        if (!kafkaRdd.isEmpty()) {
          val offsetRanges = kafkaRdd.asInstanceOf[HasOffsetRanges].offsetRanges
          task.logInfo(s"****** Start processing RDD data ******")

          val eventDF = kafkaRdd.map(json => (json.value(), DateUtil.today(DateUtil.DF_YMD_NO_LINE))).toDF("contents", "dt")

          task.saveToGP(eventDF, resultTable, dbUser, dbPassword, resultUrl, SaveMode.Overwrite, "jdbc", dbDriver)

          task.logInfo("****** End processing RDD data ******")

          task.kafkaOffset.commitOffset(offsetRanges)
        }
      })

    task.getStreamingContext.start()
    //task.addCloseServer()
    //task.getStreamingContext.awaitTermination()
    if (config.streamingSyncEventMultiStopEnable.toBoolean) {
      task.stopAtTime(LocalDateTime.now().withHour(23).withMinute(58))
    } else {
      task.getStreamingContext.awaitTermination()
    }
  }

  def apply(config: Map[String, String]): SyncEventStreamingTask =
    new SyncEventStreamingTask(config)

}
