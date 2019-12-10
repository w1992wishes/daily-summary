package me.w1992wishes.spark.sql.hbase

import java.sql.Timestamp
import java.util.Objects

import ch.hsr.geohash.GeoHash
import org.apache.commons.lang3.{ArrayUtils, StringUtils}
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.mapred.TableOutputFormat
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.mapred.JobConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.col

/**
  * @author w1992wishes 2019/11/16 11:13
  */
object SparkSqlToHbase {

  def main(args: Array[String]): Unit = {

    val sourceTable = "t_source"
    val sinkTable = "t_sink"
    val partitions = args(2).toInt

    println("开始")

    val spark = SparkSession
      .builder()
      //.master("local")
      .appName("GpToHive")
      .getOrCreate()

    val condition = getParalleledCondition(partitions)
    // 并行处理
    val df = Range(0, partitions)
      .map(index => {
        spark
          .read
          .format("jdbc")
          .option("driver", "")
          .option("url", "")
          .option("dbtable", s"(SELECT * FROM $sourceTable WHERE ${condition(index)}) AS t_tmp_$index")
          .option("user", "")
          .option("password", "")
          .option("fetchsize", 5000)
          .load()
      })
      .reduce((rdd1, rdd2) => rdd1.union(rdd2))
      .drop(col("save_time"))

    val jobConf = new JobConf(HBaseConfiguration.create())
    jobConf.set("hbase.zookeeper.quorum", "192.168.11.72,192.168.11.73,192.168.11.74")
    jobConf.set("hbase.zookeeper.property.clientPort", "2181")
    jobConf.set(TableOutputFormat.OUTPUT_TABLE, sinkTable)
    jobConf.setOutputFormat(classOf[TableOutputFormat])
    df.rdd
      .filter(row => StringUtils.isNotEmpty(row.getAs[String]("site")))
      .map(row => {
        val site = row.getAs[String]("site")
        val time = row.getAs[Timestamp]("time")
        val thumbnail_id = row.getAs[String]("thumbnail_id")

       /* val timeStr = DateUtils.dateTimeToStr(time.toLocalDateTime, DateUtils.DF_NORMAL_NO_LINE)
        val ymd = timeStr.substring(0, 8)
        val hms = timeStr.substring(8, timeStr.length)*/

        val temp = site.replace("(", "").replace(")", "").split(" ")
        val lon = temp(0).toDouble
        val lat = temp(1).toDouble
        val geoHash = GeoHash.withCharacterPrecision(lat, lon, 12).toBase32
        val rowKey = Bytes.toBytes(makeGTRowKey(time, geoHash, thumbnail_id))

        val put = new Put(rowKey)

        val sys_code = row.getAs[String]("sys_code")
        if (StringUtils.isNotEmpty(sys_code)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("sys_code"), Bytes.toBytes(row.getAs[String]("sys_code")))
        }

        if (StringUtils.isNotEmpty(thumbnail_id)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("thumbnail_id"), Bytes.toBytes(thumbnail_id))
        }
        val thumbnail_url = row.getAs[String]("thumbnail_url")
        if (StringUtils.isNotEmpty(thumbnail_url)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("thumbnail_url"), Bytes.toBytes(thumbnail_url))
        }
        val image_id = row.getAs[String]("image_id")
        if (StringUtils.isNotEmpty(image_id)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("image_id"), Bytes.toBytes(image_id))
        }
        val image_url = row.getAs[String]("image_url")
        if (StringUtils.isNotEmpty(image_url)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("image_url"), Bytes.toBytes(image_url))
        }
        val feature_info = row.getAs[Array[Byte]]("feature_info")
        if (ArrayUtils.isNotEmpty(feature_info)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("feature_info"), feature_info)
        }
        put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("algo_version"), Bytes.toBytes(row.getAs[Int]("algo_version")))
        val gender_info = row.getAs[String]("gender_info")
        if (StringUtils.isNotEmpty(gender_info)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("gender_info"), Bytes.toBytes(gender_info))
        }
        val age_info = row.getAs[String]("age_info")
        if (StringUtils.isNotEmpty(age_info)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("age_info"), Bytes.toBytes(age_info))
        }
        val hairstyle_info = row.getAs[String]("hairstyle_info")
        if (StringUtils.isNotEmpty(hairstyle_info)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("hairstyle_info"), Bytes.toBytes(hairstyle_info))
        }
        val hat_info = row.getAs[String]("hat_info")
        if (StringUtils.isNotEmpty(hat_info)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("hat_info"), Bytes.toBytes(hat_info))
        }
        val glasses_info = row.getAs[String]("glasses_info")
        if (StringUtils.isNotEmpty(glasses_info)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("glasses_info"), Bytes.toBytes(glasses_info))
        }
        val race_info = row.getAs[String]("race_info")
        if (StringUtils.isNotEmpty(race_info)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("race_info"), Bytes.toBytes(race_info))
        }
        val mask_info = row.getAs[String]("mask_info")
        if (StringUtils.isNotEmpty(mask_info)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("mask_info"), Bytes.toBytes(mask_info))
        }
        val skin_info = row.getAs[String]("skin_info")
        if (StringUtils.isNotEmpty(skin_info)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("skin_info"), Bytes.toBytes(skin_info))
        }
        val pose_info = row.getAs[String]("pose_info")
        if (StringUtils.isNotEmpty(pose_info)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("pose_info"), Bytes.toBytes(pose_info))
        }

        val quality_info = row.getAs[Double]("quality_info")
        put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("quality_info"), Bytes.toBytes(quality_info))

        val target_rect = row.getAs[String]("target_rect")
        if (StringUtils.isNotEmpty(target_rect)) {
          put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("target_rect"), Bytes.toBytes(target_rect))
        }
        val target_rect_float = row.getAs[String]("target_rect_float")
        if (StringUtils.isNotEmpty(target_rect_float)) put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("target_rect_float"), Bytes.toBytes(target_rect_float))

        val land_mark_info = row.getAs[String]("land_mark_info")
        if (StringUtils.isNotEmpty(land_mark_info)) put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("land_mark_info"), Bytes.toBytes(land_mark_info))

        val target_thumbnail_rect = row.getAs[String]("target_thumbnail_rect")
        if (StringUtils.isNotEmpty(target_thumbnail_rect)) put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("target_thumbnail_rect"), Bytes.toBytes(target_thumbnail_rect))

        val source_id = row.getAs[String]("source_id")
        if (StringUtils.isNotEmpty(source_id)) put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("source_id"), Bytes.toBytes(source_id))

        put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("source_type"), Bytes.toBytes(row.getAs[Int]("source_type")))

        if (StringUtils.isNotEmpty(site)) put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("site"), Bytes.toBytes(site))

        put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("feature_quality"), Bytes.toBytes(row.getAs[Double]("feature_quality")))

        if (Objects.nonNull(time)) put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("time"), Bytes.toBytes(time.getTime))

        val create_time = row.getAs[Timestamp]("create_time")
        if (Objects.nonNull(create_time)) put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("create_time"), Bytes.toBytes(create_time.getTime))

        val column1 = row.getAs[String]("column1")
        if (StringUtils.isNotEmpty(column1)) put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("column1"), Bytes.toBytes(column1))

        val column2 = row.getAs[String]("column2")
        if (StringUtils.isNotEmpty(column2)) put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("column2"), Bytes.toBytes(column2))

        val column3 = row.getAs[String]("column3")
        if (StringUtils.isNotEmpty(column3)) put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("column3"), Bytes.toBytes(column3))

        val field1 = row.getAs[String]("field1")
        if (StringUtils.isNotEmpty(field1)) put.addColumn(Bytes.toBytes("basic_info"), Bytes.toBytes("field1"), Bytes.toBytes(field1))

        (new ImmutableBytesWritable, put)

      }).saveAsHadoopDataset(jobConf)
  }

  def makeGTRowKey(time: Timestamp, geoHash: String, uid: String): String = {
    val rowKey = geoHash.concat(time.getTime/1000 + "").concat(uid)
    rowKey
  }

  def getRandomNumber: String = {
    val ranStr = Math.random + ""
    val pointIndex = ranStr.indexOf(".")
    ranStr.substring(pointIndex + 1, pointIndex + 3)
  }

  /**
    * 获取 spark 并行查询的条件数组
    *
    * @param realPartitions 实际分区数
    * @return
    */
  protected def getParalleledCondition(realPartitions: Int): Array[String] = {

    // 转换为 spark sql where 字句中的查询条件
    Range(0, realPartitions).map(partition => s"CAST(thumbnail_id as numeric) % $realPartitions = $partition"
    ).toArray
  }
}
