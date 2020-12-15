package com.arangodb.spark

import com.arangodb.model.DocumentImportOptions
import com.arangodb.model.DocumentImportOptions.OnDuplicate
import com.arangodb.spark.vpack.VPackUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row}

import scala.collection.JavaConverters.seqAsJavaListConverter


/**
 * ArangoSpark增强，已存在的数据会update
 */
object ArangoSparkEnhance {

  def saveDF(dataframe: DataFrame, collection: String, options: WriteOptions): Unit =
    saveRDD[Row](dataframe.rdd, collection, options, (x: Iterator[Row]) => x.map { y => VPackUtils.rowToVPack(y) })

  private def saveRDD[T](rdd: RDD[T], collection: String, options: WriteOptions, map: Iterator[T] => Iterator[Any]): Unit = {
    val writeOptions = createWriteOptions(options, rdd.sparkContext.getConf)
    rdd.foreachPartition { p =>
      if (p.nonEmpty) {
        val arangoDB = createArangoBuilder(writeOptions).build()
        val col = arangoDB.db(writeOptions.database).collection(collection)
        val op = new DocumentImportOptions()
        op.onDuplicate(OnDuplicate.update)
        col.importDocuments(map(p).toList.asJava, op)
        arangoDB.shutdown()
      }
    }
  }
}