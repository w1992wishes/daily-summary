package me.w1992wishes.structured.streaming.demo

import me.w1992wishes.structured.streaming.demo.sink.JDBCSink
import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql.types._
import org.apache.spark.sql._

/**
  * @author w1992wishes 2019/11/25 13:42
  */
object KafkaStructuredStreamingDemo {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .master("local[2]")
      .appName("appName")
      .getOrCreate()

    val df: Dataset[Row] = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "host1:port1,host2:port2")
      .option("subscribe", "topic.*")
      .option("startingOffsets", "earliest")
      .option("minPartitions", "10")
      .option("failOnDataLoss", "true")
      .load()

    df.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")

    /**
      * kafak json 数据参考 message.sjon
      */
    val schema = new StructType()
      .add("metadata", new StructType()
        .add("access_token", StringType)
        .add("client_version", IntegerType))
      .add("devices", new StructType()
        .add("thermostats", MapType(StringType, new StructType()
          .add("device_id", StringType)
          .add("locale", StringType)
          .add("software_version", StringType)
          .add("structure_id", StringType)
          .add("name", StringType)
          .add("name_long", StringType)
          .add("last_connection", TimestampType)
          .add("is_online", BooleanType)
          .add("can_cool", BooleanType)
          .add("can_heat", BooleanType)
          .add("is_using_emergency_heat", BooleanType)
          .add("has_fan", BooleanType)
          .add("fan_timer_active", BooleanType)
          .add("fan_timer_timeout", TimestampType)
          .add("has_leaf", BooleanType)
          .add("temperature_scale", StringType)
          .add("target_temperature_f", StringType)
          .add("target_temperature_c", StringType)
          .add("target_temperature_high_f", StringType)
          .add("target_temperature_high_c", StringType)
          .add("target_temperature_low_f", StringType)
          .add("target_temperature_low_c", StringType)
          .add("eco_temperature_high_f", StringType)
          .add("eco_temperature_high_c", StringType)
          .add("eco_temperature_low_f", StringType)
          .add("eco_temperature_low_c", StringType)
          .add("away_temperature_high_f", StringType)
          .add("away_temperature_high_c", StringType)
          .add("away_temperature_low_f", StringType)
          .add("away_temperature_low_c", StringType)
          .add("hvac_mode", StringType)
          .add("previous_hvac_mode", StringType)
          .add("ambient_temperature_f", StringType)
          .add("ambient_temperature_c", StringType)
          .add("humidity", StringType)
          .add("hvac_state", StringType)
          .add("where_id", StringType)
          .add("is_locked", BooleanType)
          .add("locked_temp_min_f", StringType)
          .add("locked_temp_max_f", StringType)
          .add("locked_temp_min_c", StringType)
          .add("locked_temp_max_c", StringType)
          .add("label", StringType)
          .add("where_name", StringType)
          .add("sunlight_correction_enabled", BooleanType)
          .add("sunlight_correction_active", BooleanType)
          .add("fan_timer_duration", StringType)
          .add("time_to_target", StringType)
          .add("time_to_target_training", StringType))
        )
        .add("smoke_co_alarms", MapType(StringType, new StructType()
          .add("device_id", StringType)
          .add("locale", StringType)
          .add("software_version", StringType)
          .add("structure_id", StringType)
          .add("name", StringType)
          .add("name_long", StringType)
          .add("last_connection", TimestampType)
          .add("is_online", BooleanType)
          .add("battery_health", StringType)
          .add("co_alarm_state", StringType)
          .add("smoke_alarm_state", StringType)
          .add("is_manual_test_active", BooleanType)
          .add("last_manual_test_time", TimestampType)
          .add("ui_color_state", StringType)
          .add("where_id", StringType)
          .add("where_name", StringType))
        )
        .add("cameras", MapType(StringType, new StructType()
          .add("device_id", StringType)
          .add("software_version", StringType)
          .add("structure_id", StringType)
          .add("where_id", StringType)
          .add("where_name", StringType)
          .add("name", StringType)
          .add("name_long", StringType)
          .add("is_online", BooleanType)
          .add("is_streaming", BooleanType)
          .add("is_audio_input_enabled", BooleanType)
          .add("last_is_online_change", TimestampType)
          .add("is_video_history_enabled", BooleanType)
          .add("web_url", StringType)
          .add("app_url", StringType)
          .add("is_public_share_enabled", BooleanType)
          .add("activity_zones", ArrayType(new StructType().add("name", StringType).add("id", StringType), containsNull = true))
          .add("public_share_url", StringType)
          .add("snapshot_url", StringType)
          .add("last_event", new StructType()
            .add("has_sound", BooleanType)
            .add("has_motion", BooleanType)
            .add("has_person", BooleanType)
            .add("start_time", TimestampType)
            .add("end_time", TimestampType)
            .add("urls_expire_time", TimestampType)
            .add("web_url", StringType)
            .add("app_url", StringType)
            .add("image_url", StringType)
            .add("animated_image_url", StringType)
            .add("activity_zone_ids", ArrayType(StringType, containsNull = true))
          )
        )
        )
      )
      .add("structures", MapType(StringType, new StructType()
        .add("structure_id", StringType)
        .add("thermostats", ArrayType(StringType, containsNull = true))
        .add("smoke_co_alarms", ArrayType(StringType, containsNull = true))
        .add("cameras", ArrayType(StringType, containsNull = true))
        .add("away", StringType)
        .add("name", StringType)
        .add("country_code", StringType)
        .add("postal_code", StringType)
        .add("peak_period_start_time", TimestampType)
        .add("peak_period_end_time", TimestampType)
        .add("time_zone", StringType)
        .add("eta", new StructType()
          .add("trip_id", StringType)
          .add("estimated_arrival_window_begin", TimestampType)
          .add("estimated_arrival_window_end", TimestampType)
        )
        .add("eta_begin", TimestampType)
        .add("co_alarm_state", StringType)
        .add("smoke_alarm_state", StringType)
        .add("rhr_enrollment", BooleanType)
        .add("wwn_security_state", StringType)
        .add("wheres", MapType(StringType, new StructType()
          .add("where_id", StringType)
          .add("name", StringType))
        ))
      )

    val nestTimestampFormat = "yyyy-MM-dd'T'HH:mm:ss.sss'Z'"
    val jsonOptions =  Map("timestampFormat" -> nestTimestampFormat)

    /**
      * 对于 Kafka发送过来的是 JSON 格式的数据，可以使用 functions 里面的 from_json() 函数解析
      * 并选择所需要的列，并做相对的 transformation 处理。
      */
    val parsed = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "net-logs")
      .option("startingOffsets", "earliest")
      .load()
      .select(functions.from_json(functions.col("value").cast("string"), schema, jsonOptions).alias("parsed_value"))

    import spark.implicits._

    val camera = parsed
      .select(functions.explode($"parsed_value.devices.cameras"))
      .select("value.*")

    camera.printSchema()

    val sightings: DataFrame = camera
      .select("device_id", "last_event.has_person", "last_event.start_time")

    val console = sightings.writeStream
      .format("console")
      .option("checkpointLocation", "/path/to/HDFS/dir")
      .outputMode(OutputMode.Append())

    val query = console.start()

    query.awaitTermination()

  }

  def toDB (sightings: DataFrame): Unit = {
    // 写入数据库
    val url="jdbc:mysql://127.0.0.1:6606/test?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&failOverReadOnly=false&useSSL=false"
    val user ="root"
    val pwd = "111608"

    val writer = new JDBCSink(url,user, pwd)
    val query = sightings
        .writeStream
        .foreach(writer)
        .outputMode("update")
        .trigger(Trigger.ProcessingTime("25 seconds"))
        .start()

    query.awaitTermination()
  }

}
