package me.w1992wishes.spark.es.conf

import com.typesafe.config.{Config, ConfigFactory, ConfigParseOptions, ConfigResolveOptions}

class ConfigArgsGp2Es {
  val parent: Config = ConfigFactory.load("application.conf")

  val configExport: Config = ConfigFactory.load("matrix-output-gp2es.conf",
    ConfigParseOptions.defaults,
    ConfigResolveOptions.defaults.setAllowUnresolved(true))

  val config: Config = parent
    .withFallback(configExport)
    .resolve()

  val defaultDaysAgo: String = config.getString("gp2es.default.daysAgo")

  def gp2esSourceDbUrl(key: String): String = {
    config.getString(s"gp2es.$key.source.url")
  }

  def gp2esSourceTable(key: String): String = {
    config.getString(s"gp2es.$key.source.table")
  }

  def gp2esSourcePartitionField(key: String): String = {
    config.getString(s"gp2es.$key.source.partitionFieldName")
  }

  def gp2esSourcePartitionNum(key: String): String = {
    config.getString(s"gp2es.$key.source.partitionNum")
  }

  def gp2esSourceSql(key: String): String = {
    config.getString(s"gp2es.$key.source.sql")
  }

  def gp2esSinkIndex(key: String): String = {
    config.getString(s"gp2es.$key.sink.index")
  }

  def gp2esSinkType(key: String): String = {
    config.getString(s"gp2es.$key.sink.type")
  }

}

object ConfigArgsGp2Es {
  def apply: ConfigArgsGp2Es = new ConfigArgsGp2Es()
}