## PreprocessJob

1.根据时间并行加载数据
2.计算每条数据的 feature_quality
3.根据 thumbnail_id 重新分区
4.写入预处理后的表

APP_MAINCLASS="me.w1992wishes.spark.streaming.job.StreamingPreProcessJob"
spark-submit \
--master spark://127.0.0.1:7077 \
--class $APP_MAINCLASS \
--executor-memory 3G \
--total-executor-cores 12 \
--executor-cores 3 \
--files config.properties \
etl-streaming-preprocess.jar --partitions 36