package me.w1992wishes.spark.etl.demo

import org.apache.spark.sql.SparkSession


/**
  * spark 连接 greenplum 例子
  *
  * @author w1992wishes 2018/9/18 16:38
  */
object GpExample {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession
      .builder()
      .appName("gptest")
      .master("local")
      .getOrCreate()

    val jdbcDF = spark.read
      .format("jdbc")
      .option("url", "jdbc:pivotal:greenplum://192.168.11.72:5432;DatabaseName=testdb")
      .option("dbtable", "public.t_archive_person")
      .option("user", "gpadmin")
      .option("password", "gpadmin")
      .load()

    jdbcDF.where("person_id < 10").show()
  }
}
