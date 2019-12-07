package me.w1992wishes.spark.sql.phoenix

import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * @author w1992wishes 2019/12/7 11:42
  */
object SparkSqlOnPhoenix {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      //.config("phoenix.schema.isNamespaceMappingEnabled", value = true)
      .appName("phoenix-test")
      .master("local")
      .getOrCreate()

    val fromPhx = spark.sqlContext
      .read
      .format("jdbc")
      .options(Map(
          "phoenix.schema.isNamespaceMappingEnabled" -> "true",
          "driver" -> "org.apache.phoenix.jdbc.PhoenixDriver",
          "url" -> "jdbc:phoenix:ds072,ds073,ds074:2181:/hbase",
          "dbtable" -> "T_EMPLOYEE"
        ))
      .load

    // load from phoenix
    val sessions = fromPhx.select("SESSION_ID", "AGE").limit(10)

    //sessions.show(10)

    sessions
      .write
      .format("org.apache.phoenix.spark")
      .mode(SaveMode.Overwrite)
      .options(Map(
        "driver" -> "org.apache.phoenix.jdbc.PhoenixDriver",
        "zkUrl" -> "jdbc:phoenix:ds072,ds073,ds074:2181",
        "table" -> "SPARK_RESULT"  // CREATE TABLE SPARK_RESULT (SESSION_ID varchar not null primary key , AGE INTEGER);
      ))
      .save()

  }

}
