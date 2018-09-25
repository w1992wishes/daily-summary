## Spark ETL 例子

* GpExample : SparkSQL 连接 Greenplum
* FeatureLoader : Spark 从 Greenplum 并行加载数据

## 运行示例

bin/spark-submit \
--master spark://192.168.11.72:7077 \
--class com.intellif.analysis.sparketl.core.FeatureLoader \
--executor-memory 4G \
--total-executor-cores 112 \
--executor-cores 2 \
spark-etl.jar \
3333333 \
224