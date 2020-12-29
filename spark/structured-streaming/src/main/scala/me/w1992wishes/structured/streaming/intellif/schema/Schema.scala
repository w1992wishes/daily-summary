package me.w1992wishes.structured.streaming.intellif.schema

/**
  * @author w1992wishes 2020/12/16 14:19
  */
object Schema {

  import org.apache.spark.sql.types._

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

  val dataSchema: StructType = new StructType()
    .add("datas", StringType)
    .add("operation", StringType)
    .add("operator", StringType)
    .add("time", TimestampType)
    .add("type", StringType)
}
