# daily-summary

## 目录

* [通用常备](common)
    * [Redis 常问](common/document/Redis%20总结.md)
* [java](java/README.md)
    * [Java 类加载器](java/classloader/README.md)
    * [Java 并发知识](java/concurrency/README.md)
        * [Java 并发包基石-AQS详解](java/documents/concurrency/AQS.md)
    * [JVM 知识](java/jvm/README.md)
        * [JVM 运行时数据区域](java/documents/jvm/jvm-运行时区域内存.md)
        * [JVM 垃圾回收](java/documents/jvm/jvm-垃圾回收.md)
        * [JVM 内存溢出异常测试](java/documents/jvm/jvm-内存溢出测试.md)
    * [Java 新特性](java/new-feature/README.md)
    * [Java SPI](java/spi/README.md)
* [算法相关](algorithm/README.md)
    * [排序](algorithm/sort/README.md)
         * [快速排序](algorithm/documents/sort/QuickSort.md)
         * [归并排序](algorithm/documents/sort/MergeSort.md)
         * [堆排序](algorithm/documents/sort/HeapSort.md)
         * [计数排序](algorithm/documents/sort/CountingSort.md)
         * [桶排序](algorithm/documents/sort/BucketSort.md)
         * [基数排序](algorithm/documents/sort/RaidxSort.md)
    * [查找](algorithm/search/README.md)
        * [斐波那契查找](algorithm/documents/search/FibonacciSearch.md)
    * [图论](algorithm/graph/README.md)
        * [拓扑排序算法](algorithm/documents/graph/TopoSort.md)
        * [深度优先遍历(DFS)和广度优先遍历(BFS)](algorithm/documents/graph/DFS_BFS.md)
    * [聚类算法](algorithm/documents/cluster/Cluster.md)
* [flink](flink/README.md)
    * [flink quick start](flink/flink-quick-start)
    * [flink 用户行为分析样例](flink/flink-analysis)
    * [flink 架构原理](flink/document/flink-architecture)
        * [flink 运行架构](flink/document/flink-architecture/【Flink】Flink%20运行架构.md)
        * [Flink Watermark 机制浅析](flink/document/flink-architecture/【Flink】Flink%20Watermark%20机制浅析.md)
    * [flink 进阶](flink/document/flink-promotion)
        * [Flink cep](flink/document/flink-promotion/【Flink】Flink%20cep.md)
* [Spark](spark/README.md)
    * [Spark 基础实践](spark/spark-base)
        * [Spark On Yarn 安装](spark/documents/base/SparkOnYarnInstall.md)
        * [Spark 之 HiveSupport 连接](spark/documents/base/SparkHiveSupport.md)
        * [Spark RDD、DataFrame 和 DataSet](spark/documents/base/RDD-DS-DF.md)
        * [Spark UDF](spark/documents/base/SparkUDF.md)
    * [Spark 运行 demo](spark)
        * [Spark 写 ES](spark/spark-es)
        * [Spark Sql On Hive](spark/spark-hive)
        * [Spark Sql](spark/spark-sql)
        * [Spark Streaming](spark/spark-streaming)
    * [spark rpc]()
        * [Spark 通信架构](spark/documents/rpc/SparkRpc.md)
        * [Spark 启动通信](spark/documents/rpc/SparkStartRpc.md)
        * [Spark 运行通信](spark/documents/rpc/SparkRunRpc.md)
    * [Spark 核心原理]()
        * [Spark 提交任务-核心组件交互原理](spark/documents/core/Spark%20核心组件交互原理.md)
        * [Spark 提交作业](spark/documents/core/RunJob.md)
        * [Spark 划分调度阶段](spark/documents/core/CreateStage.md)
        * [Spark 提交调度阶段](spark/documents/core/SubmitStage.md)
        * [Spark 提交任务](spark/documents/core/SubmitTask.md)
        * [Spark 执行任务](spark/documents/core/RunTask.md)
        * [获取执行结果](spark/documents/core/ResultTask.md)
    * [Spark 容错处理]()
        * [Executor 异常](spark/documents/ha/ExecutorHA.md)
        * [Worker 异常](spark/documents/ha/WorkerHA.md)
        * [Master 异常](spark/documents/ha/MasterHA.md)
    * [Spark 存储原理]()
        * [通信层架构分析](spark/documents/storage/StorageStruct.md)
        * [存储层分析](spark/documents/storage/Storage.md)
        * [shuffle 过程](spark/documents/storage/SparkShuffle.md)
    * [Spark 运行架构]()
        * [Spark Yarn](spark/documents/yarn/YARNModel.md)
        * [Spark Yarn Client 模式](spark/documents/yarn/YARNClient.md)
        * [Spark Yarn Cluster 模式](spark/documents/yarn/YARNCluster.md)
    * [Spark 调优]()
        * [Spark 调优-基本调优](spark/documents/optimize/Spark-basic-optimize.md)
        * [Spark 调优-数据倾斜](spark/documents/optimize/DataSkewOptimize.md)
* [Hadoop](hadoop/README.md)
    * [base](hadoop/README.md)
        * [Hadoop 2.9.1 集群安装](hadoop/document/base/Hadoop-Installation.md)
        * [Hadoop 2.9.2 HA安装](hadoop/document/base/Hadoop-HA-Installa.md)
        * [Hadoop 小文件处理](hadoop/document/base/Hadoop-Small-File.md)
        * [MR 简单使用](hadoop/map-reduce/README.md)
        * [MR shuffle 过程详解](hadoop/document/base/MRShuffle.md) 
        * [Hadoop 集成 lzo](hadoop/document/base/hadoop-integrate-lzo.md)
    * [HBase](hadoop/hbase-common/README.md)
        * [HBase 安装](hadoop/document/hbase/HBaseInstall.md)
        * [HBase 架构](hadoop/document/hbase/HBaseArchitecture.md)
        * [HBase 列族属性配置](hadoop/document/hbase/HBaseColumnProperties.md)
        * [HBase 宽表和高表](hadoop/document/hbase/HBaseTable.md)
        * [HBase 行健设计](hadoop/document/hbase/HBaseRowKey.md)
        * [HBase 自动拆分和预分区](hadoop/document/hbase/HBaseSplit.md)
        * [HBase 过滤器](hadoop/document/hbase/HBaseFilter.md)
        * [HBase 协处理器](hadoop/document/hbase/hbase-coprocessor.md)
        * [HBase Phoenix 安装使用](hadoop/document/hbase/Hbase-Phoenix.md)
    * [Hive](hadoop/hive-common/README.md)
        * [Hive 2.3.4 安装](hadoop/document/hive/HiveInstall.md)
        * [Hive 建表语句详解](hadoop/document/hive/HiveCreateTable.md)
        * [Hive Join 介绍](hadoop/document/hive/HiveJoin.md)
        * [Hive UDF](hadoop/document/hive/hive-udf.md)
        * [Hive On HBase](hadoop/document/hive/hive-on-hbase.md)
        * [Hive 优化小结](hadoop/document/hive/Hive-optimization.md)
    * [Flume](hadoop/flume)
        * [Flume 启动关闭脚本](hadoop/flume/script/executeFlume.sh)
* [ES](elasticsearch/README.md)
    * [ElasticSearch 安装](elasticsearch/document/ESInstall.md)
    * [ElasticSearch 入门使用](elasticsearch/document/ESStarted.md)
    * [ElasticSearch 文档操作](elasticsearch/document/ESDocument.md)
    * [ElasticSearch 映射（mapping）](elasticsearch/document/ESMapping.md)
    * [ElasticSearch 结构化查询和过滤](elasticsearch/document/ESDSL.md)
    * [ElasticSearch analyzer 和 analyze API](elasticsearch/document/ESAnalyzer.md)
    * [ElasticSearch 深入分片](elasticsearch/document/ESShard.md)
* [Zookeeper](zookeeper/README.md)
    * [zk api 使用例子](zookeeper/zk-example/README.md)
    * [zk 配置中心简单原理](zookeeper/zk-config-server1/README.md)
    * [zk 可靠地服务配置](zookeeper/zk-config-server2/README.md)
    * [zk 实现分布式锁](zookeeper/zk-lock-server/README.md)
    * [zk master 选举](zookeeper/zk-leader-election/README.md)
* [Kafka](kafka)
    * [消费者简单例子](kafka/kafaka-consumer)
    * [生产者简单例子](kafka/kafka-producer)
    * [kafka同spring整合例子](kafka/kafka-spring-integration)
    * [kafka 总结](kafka/document/Kafka%20总结.md)
* [Scheduler](scheduler)
    * [大数据调度系统设计](scheduler/SchedulerSystemImpl.md)
* [LeetCode](leet-code)
    * [简单集合](leet-code/easy-collection/README.md)
    * [二叉树](leet-code/binary-tree/README.md)
    * [数据库](leet-code/database)
* [phoenix](phoenix/README.md)
    * [Spark-Phoenix demo](phoenix/phoenix-spark/README.md)