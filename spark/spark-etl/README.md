## Spark ETL 例子

* GpExample : SparkSQL 连接 Greenplum
* FeatureLoader : Spark 从 Greenplum 并行加载数据
* job1 : spark 根据时间并行加载数据
* job2 : spark 根据时间并行加载数据进行清洗过滤后再并行保存数据
* job3 : spark 根据时间并行加载数据并groupBy再并行保存数据

## 打包

maven 运行 mvn clean package 即可

## 提交spark 集群

bin/spark-submit \
--master spark://192.168.11.72:7077 \
--class com.intellif.analysis.sparketl.core.FeatureLoader \
--executor-memory 4G \
--total-executor-cores 112 \
--executor-cores 2 \
spark-etl.jar \
3333333 \
224

### job2:

spark/bin/spark-submit \
--master spark://192.168.11.72:7077 \
--class com.intellif.dataplatform.core.PreprocessJob \
--executor-memory 4G \
--total-executor-cores 42 \
--executor-cores 2 \
--files /home/spark/config.properties
x-data-preprocess.jar --filter-night-enable true --start-time 23:00:00 --end-time 01:00:00

因为 job2 采用了 spark-submit --files 的形式上传配置文件，所以运行前先将配置文件放到 --files 指定的地址