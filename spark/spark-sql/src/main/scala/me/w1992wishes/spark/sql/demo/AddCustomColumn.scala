package me.w1992wishes.spark.sql.demo

import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * @author w1992wishes 2019/2/26 15:56
  */
object AddCustomColumn {

  def main(args: Array[String]): Unit = {

    // spark
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("test")
      .getOrCreate()

    // core
    val code :(Float => Float) = (quality: Float) => getFeatureQuality(quality)
    import org.apache.spark.sql.functions._
    val addCol = udf(code)

    val df = spark
      .read
      .format("jdbc")
      .option("driver", "com.pivotal.jdbc.GreenplumDriver")
      .option("url", "jdbc:pivotal:greenplum://127.0.0.1:5432;DatabaseName=bigdata_odl")
      .option("dbtable", "table1")
      .option("user", "")
      .option("password", "")
      .option("fetchsize", 5000)
      .load()

    df.withColumn("feature_quality", addCol(col("quality_info")))
      .filter(row => row.getAs[Float]("feature_quality") >= -1.0)
      .write
      .mode(SaveMode.Append)
      .format("jdbc")
      .option("driver", "com.pivotal.jdbc.GreenplumDriver")
      .option("url", "jdbc:pivotal:greenplum://127.0.0.1:5432;DatabaseName=bigdata_dwd")
      .option("dbtable", "table2")
      .option("user", "")
      .option("password", "")
      .save()
  }

  private def getFeatureQuality(quality: Float): Float = {

    /**
      * 计算特征值质量
      *
      * @param quality 质量分值
      * @return 特征值质量
      */
    def calculateFeatureQuality(quality: Float): Float = {
      var featureQuality = .0f
      if (clusterQualityFilter(quality)) {
        featureQuality = quality
      } else if (classQualityFilter(quality)) {
        featureQuality = -1.0f
      } else {
        featureQuality = -2.0f
      }
      featureQuality
    }

    def clusterQualityFilter(quality: Float): Boolean = quality >= 0.77

    def classQualityFilter(quality: Float): Boolean = quality >= 0.31

    val featureQuality = calculateFeatureQuality(quality)

    featureQuality.formatted("%.2f").toFloat
  }
}
