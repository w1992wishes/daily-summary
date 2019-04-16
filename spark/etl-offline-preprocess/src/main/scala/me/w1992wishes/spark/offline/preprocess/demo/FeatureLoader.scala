package me.w1992wishes.spark.offline.preprocess.demo

import java.io.File

import org.apache.spark.sql.SparkSession

/**
  * 特征值加载类，自定义sql 加载
  *
  * @author w1992wishes 2018/9/25 15:12
  */
object FeatureLoader {
  def main(args: Array[String]): Unit = {
    // person_id
    var person_id = 0
    // spark sql 中用于指明分区的数量
    var numPartitions = 0
    if (args.length > 0) {
      person_id = args(0).toInt
      numPartitions = args(1).toInt
    }

    // warehouseLocation points to the default location for managed databases and tables
    val warehouseLocation = new File("spark-warehouse").getAbsolutePath

    // 开始时间
    val startTime = System.currentTimeMillis()

    // 为了不丢失数据，向上取整，将数据分成 numPartitions 份
    val stride = Math.ceil(person_id / numPartitions).toInt

    val spark = SparkSession
      .builder()
      .config("spark.sql.warehouse.dir", warehouseLocation)
      .appName("load data from gp")
      .getOrCreate()

    // 创建 numPartitions 个 task
    val registerDF = Range(0, numPartitions)
      .map(index => {
        spark
          .read
          .format("jdbc")
          .option("driver", "com.pivotal.jdbc.GreenplumDriver")
          .option("url", "jdbc:pivotal:greenplum://192.168.11.72:5432;DatabaseName=testdb")
          .option("dbtable", s"(SELECT feature FROM public.t_timing_face_person WHERE person_id > ${stride * index} AND person_id <= ${stride * (index + 1)}) AS t_tmp_${index}")
          .option("user", "gpadmin")
          .option("password", "gpadmin")
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
    val tempTime = System.currentTimeMillis()
    printf("======> spark env prepare time = %d s\n", (tempTime - startTime)/1000)

    val count = registerDF.count()
    // spark 加载时间 初始化时间
    printf("======> load data from gp time = %d s\n", (System.currentTimeMillis() - tempTime)/1000)
    printf("======> the data size is = %d s\n", count)
  }
}
