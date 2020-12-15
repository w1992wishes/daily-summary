package me.w1992wishes.common.analysis

import org.apache.spark.sql.{DataFrame, SparkSession}

trait Analysis extends Serializable {
  /**
   * 如需要，需具体分析任务实现
   * @param spark
   * @param params
   * @return
   */
  def load(spark : SparkSession, params: Map[String, Any]): DataFrame = {
    null
  }

  /**
   * 业务处理逻辑，具体分析任务实现
   * @param params
   * @return
   */
  def handle(params: Any*): DataFrame

  /**
   * 如需要，需具体分析任务实现
   * @param df
   * @param params
   */
  def save(df: DataFrame,params: Map[String, Any]): Unit = {

  }
}
