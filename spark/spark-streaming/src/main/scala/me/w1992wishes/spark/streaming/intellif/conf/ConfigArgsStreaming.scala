package me.w1992wishes.spark.streaming.intellif.conf

import com.typesafe.config.{Config, ConfigFactory, ConfigParseOptions, ConfigResolveOptions}

/**
  * @author w1992wishes 2020/11/16 15:13
  */
class ConfigArgsStreaming {

  val common: Config = ConfigFactory.load("application.conf")

  val configStreaming: Config = ConfigFactory.load("matrix-streaming.conf",
    ConfigParseOptions.defaults,
    ConfigResolveOptions.defaults.setAllowUnresolved(true))

  val config: Config = common
    .withFallback(configStreaming)
    .resolve()

  // spark
  val streamingSyncEventMultiKafkaMaxRatePerPartition: String = config.getString("streaming.StreamingSyncEventMulti.spark.matrix-streaming.conf")

  // kafka
  val streamingSyncEventMultiKafkaGroupId: String = config.getString("streaming.StreamingSyncEventMulti.kafka.group.id")
  val streamingSyncEventMultiKafkaInputTopic: String = config.getString("streaming.StreamingSyncEventMulti.kafka.input.topic")
  val streamingSyncEventMultiKafkaInputServers: String = config.getString("streaming.StreamingSyncEventMulti.kafka.input.servers")
  val streamingSyncEventMultiKafkaReceiveBuffer: String = config.getString("streaming.StreamingSyncEventMulti.kafka.receive.buffer.bytes")
  val streamingSyncEventMultiKafkaMaxFetch: String = config.getString("streaming.StreamingSyncEventMulti.kafka.max.partition.fetch.bytes")
  val streamingSyncEventMultiKafkaOffsetReset: String = config.getString("streaming.StreamingSyncEventMulti.kafka.auto.offset.reset")
  val streamingSyncEventMultiKafkaAutoCommit: String = config.getString("streaming.StreamingSyncEventMulti.kafka.enable.auto.commit")

  // zk
  val streamingSyncEventMultiZkServers: String = config.getString("streaming.StreamingSyncEventMulti.zk.servers")

  // task
  val streamingSyncEventMultiName: String = config.getString("streaming.StreamingSyncEventMulti.name")
  val streamingSyncEventMultiStopEnable: String = config.getString("streaming.StreamingSyncEventMulti.stop.enable")
  val streamingSyncEventMultiClosePort: String = config.getString("streaming.StreamingSyncEventMulti.close.port")
  val streamingSyncEventMultiBatchDuration: String = config.getString("streaming.StreamingSyncEventMulti.batch.duration")
  val streamingSyncEventMultiResultUrl: String = config.getString("streaming.StreamingSyncEventMulti.result.url")
  val streamingSyncEventMultiResultTable: String = config.getString("streaming.StreamingSyncEventMulti.result.table")
}

object ConfigArgsStreaming {
  def apply: ConfigArgsStreaming = new ConfigArgsStreaming()
}
