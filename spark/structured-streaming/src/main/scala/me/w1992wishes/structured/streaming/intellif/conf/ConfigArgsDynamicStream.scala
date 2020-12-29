package me.w1992wishes.structured.streaming.intellif.conf

import com.typesafe.config.{Config, ConfigFactory, ConfigParseOptions, ConfigResolveOptions}

/**
  * @author w1992wishes 2020/11/16 15:13
  */
class ConfigArgsDynamicStream {

  val common: Config = ConfigFactory.load("application.conf")

  val configStream: Config = ConfigFactory.load("matrix-handle-stream.conf",
    ConfigParseOptions.defaults,
    ConfigResolveOptions.defaults.setAllowUnresolved(true))

  val config: Config = common
    .withFallback(configStream)
    .resolve()

  // common
  val streamCommonEsArchiveIndex: String = config.getString("dynamic.stream.common.es.archive.index")
  val streamCommonEsArchiveType: String = config.getString("dynamic.stream.common.es.archive.type")
  val streamCommonEsEventIndex: String = config.getString("dynamic.stream.common.es.event.index")
  val streamCommonEsEventType: String = config.getString("dynamic.stream.common.es.event.type")
  val streamCommonKafkaEventTopic: String = config.getString("dynamic.stream.common.kafka.event.topic")
  val streamCommonHiveArchiveSql: String = config.getString("dynamic.stream.common.hive.archive.sql")
  val streamCommonEsArchiveQuery: String = config.getString("dynamic.stream.common.es.archive.query")

  // clusterStream task
  val clusterStreamName: String = config.getString("dynamic.stream.ClusterStream.name")
  val clusterStreamSystemCode: String = config.getString("dynamic.stream.ClusterStream.system.code")
  val clusterStreamGeoLength: String = config.getString("dynamic.stream.ClusterStream.geo.length")
  val clusterStreamCheckpoint: String = config.getString("dynamic.stream.ClusterStream.checkpoint")
  val clusterStreamGroupId: String = config.getString("dynamic.stream.ClusterStream.group.id")

  // classifyStream task
  val classifyStreamGeoLength: String = config.getString("dynamic.stream.ClassifyStream.geo.length")
  val classifyStreamName: String = config.getString("dynamic.stream.ClassifyStream.name")
  val classifyStreamCode: String = config.getString("dynamic.stream.ClassifyStream.system.code")
  val classifyStreamCheckpoint: String = config.getString("dynamic.stream.ClassifyStream.checkpoint")
  val classifyStreamGroupId: String = config.getString("dynamic.stream.ClassifyStream.group.id")

}

object ConfigArgsDynamicStream {
  def apply: ConfigArgsDynamicStream = new ConfigArgsDynamicStream()
}
