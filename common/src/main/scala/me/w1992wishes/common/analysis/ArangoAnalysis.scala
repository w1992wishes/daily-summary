package me.w1992wishes.common.analysis

import com.arangodb.ArangoDB
import com.arangodb.spark.{ArangoSpark, ArangoSparkEnhance, WriteOptions}
import com.arangodb.spark.rdd.ArangoRDD
import me.w1992wishes.common.util.Log
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.reflect.ClassTag

trait ArangoAnalysis extends Analysis with Log {
  /**
   * 通用
   * @param spark
   * @param collectionName
   * @param condition
   * @tparam T
   * @return
   */
  def loadFromArango[T: ClassTag](spark : SparkSession, collectionName : String, condition : String): ArangoRDD[T] = {
    val inputRDD = ArangoSpark.load[T](spark.sparkContext, collectionName).filter(condition)
    inputRDD
  }

  /**
   * 通用,加载全量数据
   * @param spark
   * @param collectionName
   * @tparam T
   * @return
   */
  def loadFromArango[T: ClassTag](spark : SparkSession, collectionName : String): ArangoRDD[T] = {
    val inputRDD = ArangoSpark.load[T](spark.sparkContext, collectionName)
    inputRDD
  }

  /**
   * 通用，追加写
   * @param df
   * @param collectionName
   * @param writeOptions
   * @param update
   */
  def saveToArango(df: DataFrame, collectionName : String, writeOptions : WriteOptions, update : Boolean): Unit = {
    update match {
      case true => {
        ArangoSparkEnhance.saveDF(df, collectionName, writeOptions)
      }
      case false => {
        ArangoSpark.saveDF(df, collectionName, writeOptions)
      }
    }
  }

  /**
   * 通用，覆盖写（先清空，再写）
   * @param df
   * @param server
   * @param dbName
   * @param collectionName
   * @param writeOptions
   * @param update
   */
  def saveToArango(df: DataFrame, server : ArangoDB, dbName : String, collectionName : String, writeOptions : WriteOptions, update : Boolean): Unit = {
    server.db(dbName).collection(collectionName).truncate()
    logInfos("清空集合 : " + collectionName)
    server.shutdown()
    update match {
      case true => {
        ArangoSparkEnhance.saveDF(df, collectionName, writeOptions)
      }
      case false => {
        ArangoSpark.saveDF(df, collectionName, writeOptions)
      }
    }
  }

}
