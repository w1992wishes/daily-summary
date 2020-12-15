package me.w1992wishes.spark.es.conf

import com.typesafe.config.{Config, ConfigFactory, ConfigParseOptions, ConfigResolveOptions}

object ConfigArgsHive2Es {
  val parent: Config = ConfigFactory.load("application.conf")

  val configExport: Config = ConfigFactory.load("matrix-output-hive2es.conf",
    ConfigParseOptions.defaults,
    ConfigResolveOptions.defaults.setAllowUnresolved(true))

  val config: Config = parent
    .withFallback(configExport)
    .resolve()

  val defaultDaysAgo: String = config.getString("hive2es.default.daysAgo")

  def hive2esSourceSql(key: String): String = {
    config.getString(s"hive2es.$key.source.sql")
  }

  def hive2esResultIndex(key: String): String = {
    config.getString(s"hive2es.$key.result.index")
  }

  def hive2esResultType(key: String): String = {
    config.getString(s"hive2es.$key.result.type")
  }

  def hive2esSourceTable(key : String): String = {
    config.getString(s"hive2es.$key.source.table")
  }

  def hive2esResultSaveMode(key : String): String = {
    config.getString(s"hive2es.$key.result.save.mode")
  }

  def hive2esResultWriteOperation(key : String): String = {
    config.getString(s"hive2es.$key.result.write.operation")
  }

  def hive2esResultMappingId(key : String): String = {
    config.getString(s"hive2es.$key.result.mapping.id")
  }

}

