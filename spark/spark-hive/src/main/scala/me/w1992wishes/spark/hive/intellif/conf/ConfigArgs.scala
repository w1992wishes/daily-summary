package me.w1992wishes.spark.hive.intellif.conf

import com.typesafe.config.{Config, ConfigFactory}

/**
 * 每种类型的分析任务对应一个配置文件，一个ConfigArgs对象，配置隔离，避免互相影响，每个分析任务的配置文件共同依赖父配置文件
 */
object ConfigArgs {
  val config: Config = ConfigFactory.load("application.conf") // 父配置文件

  val bizCode: String = config.getString("parent.bizCode")

  // spark
//  val shufflePartition: String = ConfigArgs.getString("parent.spark.shuffle.partition")
//  val logLevel: String = ConfigArgs.getString("parent.spark.log.level")

  val arangoHost: String = config.getString("parent.db.arango.host")
  val arangoPort: String = config.getString("parent.db.arango.port")
  val arangoUser: String = config.getString("parent.db.arango.user")
  val arangoPwd: String = config.getString("parent.db.arango.pwd")
  val arangoDb: String = config.getString("parent.db.arango.db")
  val arangoMaxConnections: String = config.getString("parent.db.arango.maxConnections")

  // es
  val esNodes: String = ConfigArgs.getString("parent.db.es.nodes_")
  val esPort: String = ConfigArgs.getString("parent.db.es.port")
  val esNodesWanOnly: String = ConfigArgs.getString("parent.db.es.nodes.wan.only")
  val esWriteOperation: String = ConfigArgs.getString("parent.db.es.write.operation")
  val esMappingId: String = ConfigArgs.getString("parent.db.es.mapping.id")
  val esIndexAutoCreate: String = ConfigArgs.getString("parent.db.es.index.auto.create")
  val esMappingRichDate: String = ConfigArgs.getString("parent.db.es.mapping.rich.date")
  val esBatchWriteRetryCount: String = ConfigArgs.getString("parent.db.es.batch.write.retry.count")
  val esBatchWriteRetryWait: String = ConfigArgs.getString("parent.db.es.batch.write.retry.wait")
  val esHttpTimeout: String = ConfigArgs.getString("parent.db.es.http.timeout")

  def getString(path: String): String = {
    config.getString(path)
  }

  def main(args: Array[String]): Unit = {
    println("aaa")
  }

}
