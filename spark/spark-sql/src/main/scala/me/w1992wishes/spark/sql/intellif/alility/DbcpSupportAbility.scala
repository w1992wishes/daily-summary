package me.w1992wishes.spark.sql.intellif.alility

import java.util.Properties

import me.w1992wishes.spark.sql.intellif.config.PropertiesTool


/**
  * @author w1992wishes 2019/5/20 17:40
  */
trait DbcpSupportAbility extends Serializable{

  def initDbcpProperties(properties: PropertiesTool): Properties = {
    val props: Properties = new Properties()
    props.put("initialSize", properties.getValidProperty("executor.pool.initialSize"))
    props.put("maxActive", properties.getValidProperty("executor.pool.maxActive"))
    props.put("maxIdle", properties.getValidProperty("executor.pool.maxIdle"))
    props.put("minIdle", properties.getValidProperty("executor.pool.minIdle"))
    props.put("maxWait", properties.getValidProperty("executor.pool.maxWait"))
    props.put("validationQuery", properties.getValidProperty("executor.pool.validationQuery"))
    props.put("testWhileIdle", properties.getValidProperty("executor.pool.testWhileIdle"))
    props.put("testOnBorrow", properties.getValidProperty("executor.pool.testOnBorrow"))
    props.put("timeBetweenEvictionRunsMillis", properties.getValidProperty("executor.pool.timeBetweenEvictionRunsMillis"))
    props.put("minEvictableIdleTimeMillis", properties.getValidProperty("executor.pool.minEvictableIdleTimeMillis"))
    props.put("numTestsPerEvictionRun", properties.getValidProperty("executor.pool.numTestsPerEvictionRun"))
    props.put("connectionProperties", properties.getValidProperty("executor.pool.connectionProperties"))
    props.put("username", properties.getValidProperty("db.default.user"))
    props.put("password", properties.getValidProperty("db.default.password"))
    props.put("driverClassName", properties.getValidProperty("db.default.driver"))
    props.put("url", properties.getValidProperty("db.default.url"))
    props
  }

}
