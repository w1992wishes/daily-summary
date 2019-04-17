## etl-offline-preprocess

### 运行参数解释

--isLocal 是否使用本地模式，默认 false，为 true 多用于测试
--preProcessImpl 预处理的实现，取值 1/2，1 PreProcessOnRDBMS 2 PreProcessOnGpPartitions，默认是1，如使用分区表，使用2
--partitionType 取值 d/h，d 表名分区表按天， h 表示按小时分区
--partitions 分区数
--preProcessStartTime 取值 yyyyMMddHHmmss，预处理开始时间（如果不设置，默认取 preProcessedTable 最大时间）
--preProcessEndTime 取值 yyyyMMddHHmmss，预处理结束时间（如果不设置，默认取 preProcessTable 最大时间）
--minutesPerPartition 单个分区处理的最小时间范围，单位分钟
--preProcessTable 预处理数据源表（如果不设置，默认从配置中读取）
--preProcessedTable 预处理后的数据（如果不设置，默认从配置中读取）
--timeProtection 时间防护，开启后最多取 运行时间的前一天0时0分0秒至运行时间 内的数据，可以避免大数据量一次预处理大量的数据

### 打包

maven 运行 mvn clean package 即可

### 提交spark 集群

spark-submit \
--master spark://127.0.0.1:7077 \
--class com.intellif.dataplatform.preprocess.PreprocessApp \
--executor-memory 4G \
--total-executor-cores 18 \
--executor-cores 2 \
--files system.properties \
etl-preprocess.jar --partitions 54

spark-submit \
--master yarn \
--deploy-mode cluster \
--class com.intellif.dataplatform.preprocess.PreprocessApp \
--executor-memory 4G \
--executor-cores 2 \
--num-executors 9 \
--files system.properties \
etl-preprocess.jar --partitions 54


因为 job2 采用了 spark-submit --files 的形式上传配置文件，所以运行前先将配置文件放到 --files 指定的地址