## etl-streaming-preprocess

按照自增序列提取抓拍库数据：

1. 计算加载数据的自增 id 区间：calculateSequenceRange
2. 先清除数据，防止重复插入：clearDataBySequence
3. Spark 并行加载数据
4. 并行处理 => foreachPartition

spark-submit \
--master spark://192.168.11.72:7077 \
--class com.intellif.dataplatform.streaming.preprocess.StreamingPreProcess \
--executor-memory 4G \
--total-executor-cores 42 \
--executor-cores 2 \
--files system.properties \
etl-streaming-preprocess.jar --partitions 96

spark-submit \
--master yarn \
--deploy-mode cluster \
--class com.intellif.dataplatform.streaming.preprocess.StreamingPreProcess \
--executor-memory 2G \
--executor-cores 2 \
--num-executors 16 \
--files system.properties \
etl-streaming-preprocess.jar --partitions 96