package me.w1992wishes.spark.streaming.ability

import java.util.Properties

import me.w1992wishes.spark.streaming.config.StreamingConfig

/**
  * @author w1992wishes 2019/5/20 17:40
  */
trait DbcpSupportAbility extends Serializable{

  def initDbcpProperties(streamingConfig: StreamingConfig): Properties = {
    val props: Properties = new Properties()
    props.put("initialSize", streamingConfig.getValidProperty("executor.pool.initialSize"))
    props.put("maxActive", streamingConfig.getValidProperty("executor.pool.maxActive"))
    props.put("maxIdle", streamingConfig.getValidProperty("executor.pool.maxIdle"))
    props.put("minIdle", streamingConfig.getValidProperty("executor.pool.minIdle"))
    props.put("maxWait", streamingConfig.getValidProperty("executor.pool.maxWait"))
    props.put("validationQuery", streamingConfig.getValidProperty("executor.pool.validationQuery"))
    props.put("testWhileIdle", streamingConfig.getValidProperty("executor.pool.testWhileIdle"))
    props.put("testOnBorrow", streamingConfig.getValidProperty("executor.pool.testOnBorrow"))
    props.put("timeBetweenEvictionRunsMillis", streamingConfig.getValidProperty("executor.pool.timeBetweenEvictionRunsMillis"))
    props.put("minEvictableIdleTimeMillis", streamingConfig.getValidProperty("executor.pool.minEvictableIdleTimeMillis"))
    props.put("numTestsPerEvictionRun", streamingConfig.getValidProperty("executor.pool.numTestsPerEvictionRun"))
    props.put("connectionProperties", streamingConfig.getValidProperty("executor.pool.connectionProperties"))
    props.put("username", streamingConfig.getValidProperty("streaming.sink.user"))
    props.put("password", streamingConfig.getValidProperty("streaming.sink.password"))
    props.put("driverClassName", streamingConfig.getValidProperty("streaming.sink.driver"))
    props.put("url", streamingConfig.getValidProperty("streaming.sink.url"))
    props
  }

}
