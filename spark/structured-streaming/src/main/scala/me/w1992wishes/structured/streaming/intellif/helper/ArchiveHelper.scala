package me.w1992wishes.structured.streaming.intellif.helper

import me.w1992wishes.structured.streaming.intellif.domain.SimplifiedArchive
import org.apache.spark.sql.{Dataset, SparkSession}

/**
  * @author w1992wishes 2020/12/24 15:24
  */
trait ArchiveHelper extends EsHelper {

  def loadArchive(spark: SparkSession, hiveSourceArchiveSql: String, esArchiveSource: String, esSourceQuery: String): Dataset[SimplifiedArchive] = {
    loadStaticArchive(spark, hiveSourceArchiveSql)
      .union(loadNewArchive(spark, esArchiveSource, esSourceQuery))
      .dropDuplicates(Seq("biz_code", "data_code", "data_type"))
  }

  def loadStaticArchive(spark: SparkSession, hiveSourceArchiveSql: String): Dataset[SimplifiedArchive] = {
    //  导入隐式转换
    // static hive archive
    spark.sql(hiveSourceArchiveSql).as[SimplifiedArchive]
  }

  def loadNewArchive(spark: SparkSession, esArchiveSource: String, esSourceQuery: String): Dataset[SimplifiedArchive] = {
    //  导入隐式转换
    loadFromES(spark, esArchiveSource, esSourceQuery)
      .select("biz_code", "aid", "data_type", "data_code")
      .as[SimplifiedArchive]
  }

}
