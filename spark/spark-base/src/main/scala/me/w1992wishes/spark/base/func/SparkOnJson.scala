package me.w1992wishes.spark.base.func

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{StringType, StructType, TimestampType}

/**
  * @author w1992wishes 2020/12/29 10:35
  */
object SparkOnJson {

  val eventSchema: StructType = new StructType()
    .add("bizCode", StringType)
    .add("id", StringType)
    .add("dataType", StringType)
    .add("dataCode", StringType)
    .add("time", TimestampType)
    .add("location", StringType)
    .add("guid", StringType)
    .add("createTime", TimestampType)
    .add("modifyTime", TimestampType)
    .add("sysCode", StringType)
    .add("props", StringType)

  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession.builder().config("spark.master", "local[8]").enableHiveSupport().getOrCreate()

    val sourceDF = spark.sql("select contents,dt from matrix_odl.odl_matrix_event_multi where dt <= '20201216' limit 100")

    import org.apache.spark.sql.functions._
    import spark.implicits._

    sourceDF
      .select(from_json($"contents", eventSchema).alias("contents"), $"dt")
      .select($"contents.*", $"dt")
      .na.drop(Array("time", "dataCode", "dataType"))
      .withColumn("props",
        when(upper($"dataType").equalTo("CAR"), to_json(
          struct(get_json_object($"props", "$.sourceId").alias("sourceId"),
            get_json_object($"props", "$.backgroundImage").alias("backgroundImage"))))
          .otherwise($"props"))
      .show(false)

  }


}
