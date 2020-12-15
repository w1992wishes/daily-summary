package me.w1992wishes.common.analysis

import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

trait GPAnalysis extends Analysis {

  /**
   * (select
   * *
   * from tableName
   * where  #paralleledCondition#) as t_tmp_#index#
   * 通用
   * @param spark
   * @return
   */
  def loadFromGP(spark : SparkSession, partitionFieldName : String ,partitionNum : Int, dbtable : String, user : String, password : String ,url : String, fetchsize : Int = 5000, foramt : String = "jdbc" ,driver : String = "com.pivotal.jdbc.GreenplumDriver") : DataFrame = {
    val inputDF = Range(0, partitionNum)
      .map(index => {
        spark.read
          .format(foramt)
          .option("driver", driver)
          .option("url", url)
          .option("dbtable", dbtable.replace("#paralleledCondition#", getParalleledCondition(partitionFieldName, partitionNum)(index)))
          .option("user", user)
          .option("password", password)
          .option("fetchsize", fetchsize)
          .load()
      }).reduce((rdd1, rdd2) => rdd1.union(rdd2))
    inputDF
  }



  /**
   * 通用
   * @param df
   * @param tableName
   * @param user
   * @param password
   * @param url
   * @param saveMode
   * @param format
   * @param driver
   */
  def saveToGP(df: DataFrame, tableName : String, user : String, password : String ,url : String, saveMode: SaveMode = SaveMode.Overwrite, format: String = "jdbc", driver : String = "com.pivotal.jdbc.GreenplumDriver"): Unit = {
    df.write
      .mode(saveMode)
      .format(format)
      .option("driver", driver)
      .option("url", url)
      .option("dbtable", tableName)
      .option("user", user)
      .option("password", password)
      .save()
  }



  private def getParalleledCondition(partitionFieldName : String, partitionNum: Int): Array[String] = {
    Range(0, partitionNum).map(partition => s"cast(hash_code($partitionFieldName) as numeric) % $partitionNum = $partition").toArray
  }
}
