package me.w1992wishes.structured.streaming.intellif.conf

import com.typesafe.config.{Config, ConfigFactory}

/**
  * @author w1992wishes 2020/11/16 15:13
  */
object ConfigArgs {

  val config: Config = ConfigFactory.load("application.conf")

  // project
  val bizCode: String = config.getString("common.biz.code")

  // spark
  val logLevel: String = config.getString("common.spark.log.level")
  val shufflePartition: String = config.getString("common.spark.shuffle.partition")

  // kafka
  val kafkaBootstrapServers: String = config.getString("common.kafka.bootstrap.servers")

  // zk
  val zkHosts: String = config.getString("common.zk.hosts")

  // db-gp
  val sourceDriver: String = config.getString("common.db.gp.driver")
  val dbUser: String = config.getString("common.db.gp.user")
  val dbPassword: String = config.getString("common.db.gp.password")
  val dbFetchSize: Int = config.getString("common.db.gp.fetchsize").toInt
  val parallelPartition: String = config.getString("common.db.gp.parallel.partition")

  // db-es
  val esNodes: String = config.getString("common.db.es.nodes_")
  val esPort: String = config.getString("common.db.es.port")

  // tiantu-es
  val tianTuEsNodes: String = config.getString("common.db.tiantu.es.nodes_")
  val tianTuEsPort: String = config.getString("common.db.tiantu.es.port")
  val tianTuEsNodesWanOnly: String = config.getString("common.db.tiantu.es.nodes.wan.only")
  val tianTuEsWriteOperation: String = config.getString("common.db.tiantu.es.write.operation")
  val tianTuEsMappingId: String = config.getString("common.db.tiantu.es.mapping.id")
  val tianTuEsIndexAutoCreate: String = config.getString("common.db.tiantu.es.index.auto.create")
  val tianTuEsMappingRichDate: String = config.getString("common.db.tiantu.es.mapping.rich.date")
  val tianTuEsBatchWriteRetryCount: String = config.getString("common.db.tiantu.es.batch.write.retry.count")
  val tianTuEsBatchWriteRetryWait: String = config.getString("common.db.tiantu.es.batch.write.retry.wait")
  val tianTuEsHttpTimeout: String = config.getString("common.db.tiantu.es.http.timeout")

}
