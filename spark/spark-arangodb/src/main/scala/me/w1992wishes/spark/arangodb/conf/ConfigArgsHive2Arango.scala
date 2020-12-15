package me.w1992wishes.spark.arangodb.conf

import com.typesafe.config.{Config, ConfigFactory, ConfigParseOptions, ConfigResolveOptions}

object ConfigArgsHive2Arango {
  val parent: Config = ConfigFactory.load("application.conf") // 父配置文件

  val configCollision: Config = ConfigFactory.load("matrix-output-hive2arango.conf",
    ConfigParseOptions.defaults,
    ConfigResolveOptions.defaults.setAllowUnresolved(true))

  val config: Config = parent
    .withFallback(configCollision)
    .resolve()

  def matrixExportHive2ArangoSourcePartitionNum(key : String): String = {
    config.getString(s"hive2arangodb.$key.source.partitionNum")
  }

  def defaultDaysAgo: String = config.getString("hive2arangodb.default.daysAgo")

  def matrixExportHive2ArangoSourceTable(key : String): String = {
    config.getString(s"hive2arangodb.$key.source.table")
  }

  def matrixExportHive2ArangoResultType(key : String): String = {
    config.getString(s"hive2arangodb.$key.result.type")
  }

  def matrixExportHive2ArangoResultClear(key : String): String = {
    config.getString(s"hive2arangodb.$key.result.clear")
  }

  def matrixExportHive2ArangoResultUpsert(key : String): String = {
    config.getString(s"hive2arangodb.$key.result.upsert")
  }

  def matrixExportHive2ArangoResultKeyField(key : String): String = {
    config.getString(s"hive2arangodb.$key.result.key.field")
  }

  def matrixExportHive2ArangoResultCollectionName(key : String): String = {
    config.getString(s"hive2arangodb.$key.result.collection.name")
  }

  def main(args: Array[String]): Unit = {
    println(matrixExportHive2ArangoSourcePartitionNum("familiarityPersonPerson"))
  }



}