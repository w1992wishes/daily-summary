# daily-summary

学习新技术过程的demo，也有常用工具总结...

## 目录

* [算法](algorithm/README.md)
    * [聚类算法](algorithm/cluster/Cluster.md)
    * [排序算法](algorithm/sort/README.md)
        * [快速排序算法](algorithm/sort/QuickSort.md)
        * [归并排序算法](algorithm/sort/MergeSort.md)
* [Hadoop](hadoop/README.md)
    * [Hadoop 相关](hadoop/README.md)
        * [Hadoop 2.9.1 集群安装](hadoop/Hadoop-Installation.md)
        * [Hadoop 小文件处理](hadoop/Hadoop-Small-File.md)
    * [Hadoop HBase 相关](hadoop/hbase-common/README.md)
        * [HBase 安装](hadoop/hbase-common/HBaseInstall.md)
        * [HBase 列族属性配置](hadoop/hbase-common/HBaseColumnProperties.md)
        * [HBase 宽表和高表](hadoop/hbase-common/HBaseTable.md)
        * [HBase 行健设计](hadoop/hbase-common/HBaseRowKey.md)
        * [HBase 自动拆分和预分区](hadoop/hbase-common/HBaseSplit.md)
        * [HBase 过滤器](hadoop/hbase-common/HBaseFilter.md)
        * [HBase 协处理器](hadoop/hbase-coprocessor/README.md)
    * [Hadoop Hive 相关](hadoop/hive-common/README.md)
        * [Hive Join 介绍](hadoop/hive-common/HiveJoin.md)
        * [Hive UDF](hadoop/hive-udf/README.md)
* [java](java/README.md)
    * [Java 并发知识](java/concurrency/README.md)
        * [Java 并发包基石-AQS详解](java/concurrency/AQS.md)
    * [JVM 知识](java/jvm/README.md)
        * [JVM 运行时数据区域](java/jvm/jvm-运行时区域内存.md)
        * [JVM 垃圾回收](java/jvm/jvm-垃圾回收.md)
        * [JVM 内存溢出异常测试](java/jvm/jvm-内存溢出测试.md)
* [Spark](spark/README.md)
    * spark rpc
        * [Spark 通信架构](spark/spark-base/documents/rpc/SparkRpc.md)
        * [Spark 启动通信](spark/spark-base/documents/rpc/SparkStartRpc.md)
        * [Spark 运行通信](spark/spark-base/documents/rpc/SparkRunRpc.md)
    * Spark base
        * [Spark RDD、DataFrame 和 DataSet](spark/spark-base/documents/base/RDD-DS-DF.md)
        * [Spark 提交作业](spark/spark-base/documents/base/RunJob.md)
        * [Spark 划分调度阶段](spark/spark-base/documents/base/CreateStage.md)
        * [Spark 提交调度阶段](spark/spark-base/documents/base/SubmitStage.md)
* [LeetCode](leet-code)
    * [初级算法](leet-code/easy-collection/README.md)
    * [二叉树](leet-code/binary-tree/README.md)
* [Zookeeper 相关](zookeeper/README.md)
    * [zk api 使用例子](zookeeper/zk-example/README.md)
    * [zk 配置中心简单原理](zookeeper/zk-config-server1/README.md)
    * [zk 可靠地服务配置](zookeeper/zk-config-server2/README.md)
    * [zk 实现分布式锁](zookeeper/zk-lock-server/README.md)
    * [zk master 选举](zookeeper/zk-leader-election/README.md)
* [Kafka 相关](kafka)
    * [消费者简单例子](kafka/kafaka-consumer)
    * [生产者简单例子](kafka/kafka-producer)
    * [kafka同spring整合例子](kafka/kafka-spring-integration)
* [通用代码](my-common)
* [脚本整理](my-script)
    * [greenplum 数据仓库分区](my-script/dw-partition-gp/README.md)
    * [使用 datax 增量同步数据](my-script/etl-sync-datax/README.md)
    * [flyway 动态支持多个库](my-script/script-flyway/README.md)