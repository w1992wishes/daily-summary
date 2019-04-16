package me.w1992wishes.spark.offline.preprocess.demo

import java.io.File

import org.apache.spark.sql.SparkSession

/**
  * 特征值加载类，使用 spark Sql  partitionColumn/lowerBound/upperBound
  *
  * @author w1992wishes 2018/9/21 16:10
  */
object FeatureLoader1 {
  def main(args: Array[String]): Unit = {

    // person_id
    var person_id = 0
    // spark sql 用于分区的下界
    val lowerBound = 0
    // spark sql 用于分区所用的上界
    var upperBound = 0
    // spark sql 中用于指明分区的数量
    var numPartitions = 0
    if (args.length > 0) {
      person_id = args(0).toInt
      upperBound = args(0).toInt
      numPartitions = args(1).toInt
    }

    // warehouseLocation points to the default location for managed databases and tables
    val warehouseLocation = new File("spark-warehouse").getAbsolutePath

    val spark = SparkSession
      .builder()
      .config("spark.sql.warehouse.dir", warehouseLocation)
      .appName("load data from gp test")
      .getOrCreate()

    // 开始时间
    val startTime = System.currentTimeMillis()

    val gpRDF = spark.read
      .format("jdbc")
      .option("driver", "com.pivotal.jdbc.GreenplumDriver")
      .option("url", "jdbc:pivotal:greenplum://192.168.11.72:5432;DatabaseName=testdb")
      .option("partitionColumn", "person_id")
      .option("lowerBound", lowerBound)
      .option("upperBound", upperBound)
      .option("numPartitions", numPartitions)
      .option("dbtable", "public.t_timing_face_person")
      .option("user", "gpadmin")
      .option("password", "gpadmin")
      .load()

    // 1.createOrReplaceTempView
    gpRDF.createOrReplaceTempView("person")

    // spark sql 初始化时间
    var tempTime = System.currentTimeMillis()
    printf("======> spark sql time = %d s\n", (tempTime - startTime)/1000)

    // 2.create DataFrame
    val countDF = gpRDF.sqlContext.sql("select feature from person where person_id < " + person_id)

    // 3.count() 触发从 gp 中加载数据
    val count = countDF.count()
    printf("======> load data from gp time = %d s\n", (System.currentTimeMillis() - tempTime)/1000)

    printf("======> data size = %d \n", count)

  }
}
