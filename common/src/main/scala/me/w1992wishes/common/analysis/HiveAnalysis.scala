package me.w1992wishes.common.analysis

import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

trait HiveAnalysis extends Analysis {
  /**
   * 通用
   * @param spark
   * @param sql
   * @return
   */
  def loadFromHive(spark : SparkSession, sql : String): DataFrame = {
    val inputDF = spark.sql(sql)
    inputDF
  }

  /**
   * 通用
   * @param df
   * @param tableName
   * @param saveMode
   * @param format
   */
  def saveToHive(df: DataFrame, tableName : String, saveMode: SaveMode = SaveMode.Overwrite, format: String = "parquet"): Unit = {
    df.write.mode(saveMode).format(format).insertInto(tableName)
  }
}
