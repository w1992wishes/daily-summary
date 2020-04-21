# Spark Phoenix 读写 Hbase 的示例

+ phonenix-spark:通过spark代码访问 Phoenix，然后读写Hbase;
```
Phoenix 是内嵌在HBase中的JDBC驱动，能够让用户使用标准的JDBC来操作HBase。
```

## phonenix-spark

### 代码说明

SparkReadOnPhoenix1: spark 通过 phoenix 提供的 phoenixTableAsDataFrame 从 hbase 读取数据
SparkReadOnPhoenix2: spark 通过 spark jdbc 方式用 phoenix 从 hbase 读取数据
SparkWriteOnPhoenix1: spark 通过 spark jdbc 方式用 phoenix 写入数据至 hbase
SparkWriteOnPhoenix2: spark 通过 phoenix 提供的 saveToPhoenix 写入数据至 hbase

### 运行流程

正常运行需注意：
resource 目录下需放置 hbase-site.xml 和 hdfs-site.xml 配置文件（从集群中拷贝）。

1. 本地模式直接设置 master 为 local。

```scala
val spark = SparkSession.builder()
      .master("local[16]")
      .getOrCreate()`
```    

2. 集群模式运行 spark read on phoenix ：

```scala
val spark = SparkSession.builder()
      //.master("local[16]")
      .getOrCreate()`
```    

需将如下 jar 包置于集群所有节点 $SPARK_HOME/jars 和 $HBASE_HOME/lib 目录下，jar 包可在 hbase 和 phoenix lib 目录找到：

```
phoenix-4.14.3-HBase-1.4-client.jar
phoenix-core-4.14.3-HBase-1.4.jar
phoenix-spark-4.14.3-HBase-1.4.jar
disruptor-3.4.2.jar
hbase-server-1.4.11.jar
hbase-client-1.4.11.jar
```

```bash
spark-submit \
--master yarn \
--deploy-mode client \
--class com.intellif.dataplatform.phoenix.spark.SparkReadOnPhoenix2 \
--num-executors 8 \
--executor-cores 4 \
--executor-memory 6G \
phoenix-spark-demo.jar
```

如果运行还是报错，可以尝试通过 --jars 指定

```bash
spark-submit \
--master yarn \
--deploy-mode client \
--jars hdfs://bigdata-ha/user/spark/spark_jars/phoenix-spark-4.14.3-HBase-1.4.jar,hdfs://bigdata-ha/user/spark/spark_jars/phoenix-4.14.3-HBase-1.4-client.jar  \
--class com.intellif.dataplatform.phoenix.spark.SparkReadOnPhoenix2 \
--num-executors 8 \
--executor-cores 4 \
--executor-memory 6G \
phoenix-spark-demo.jar
```

运行过程中如果还出现  java.lang.ClassNotFoundException: org.apache.twill.zookeeper.ZKClient 错误，将：

```
twill-common-0.9.0.jar
twill-discovery-api-0.9.0.jar
twill-discovery-core-0.9.0.jar
twill-zookeeper-0.9.0.jar 
```

放入各节点 $SPARK_HOME/jars 目录，如果配置了 hdfs 放置spark jar 包，也可上传至指定位置，jar 包可通过 maven 下载。

继续出现问题：Caused by: java.lang.ClassNotFoundException: com.yammer.metrics.core.Gauge

正常如果在 spark-env.sh 中引入了 hbase lib 下的 jar 包，不应该出现该问题，没找到原因，直接将 metrics-core-*.jar（hbase lib 目录下） 拷贝到 spark 的 jars 目录下。

终于成功。

综上，运行 spark read on phoenix，需将如下 jar 包引入 spark 的 jars 目录：

```
phoenix-4.14.3-HBase-1.4-client.jar (phoenix lib 目录)
phoenix-core-4.14.3-HBase-1.4.jar (phoenix lib 目录)
phoenix-spark-4.14.3-HBase-1.4.jar (phoenix lib 目录)
disruptor-3.4.2.jar (hbase lib 目录)
hbase-server-1.4.11.jar (hbase lib 目录)
hbase-client-1.4.11.jar (hbase lib 目录)
twill-common-0.9.0.jar（需下载）
twill-discovery-api-0.9.0.jar（需下载）
twill-discovery-core-0.9.0.jar（需下载）
twill-zookeeper-0.9.0.jar（需下载）
metrics-core-*.jar (hbase lib 目录)
```

3. 集群模式运行 spark write on phoenix ：

```bash
spark-submit \
--master yarn \
--deploy-mode client \
--jars hdfs://bigdata-ha/user/spark/spark_jars/phoenix-spark-4.14.3-HBase-1.4.jar,hdfs://bigdata-ha/user/spark/spark_jars/phoenix-4.14.3-HBase-1.4-client.jar  \
--class com.intellif.dataplatform.phoenix.spark.SparkWriteOnPhoenix1 \
--num-executors 8 \
--executor-cores 4 \
--executor-memory 6G \
phoenix-spark-demo.jar
```

出错：

Inconsistent namespace mapping properties. Cannot initiate connection as SYSTEM:CATALOG is found but client does not have phoenix.schema.isNamespaceMappingEnabled enabled

删除 hbase 和phoenix conf 目录下的 hbase-site.xml 中关于命名空间启用的设置。

重启hbase，进入hbase shell

修改hbase中表 SYSTEM:CATALOG 名为 SYSTEM.CATALOG
1）disable 'SYSTEM:CATALOG'
2）snapshot 'SYSTEM:CATALOG', 'cata_tableSnapshot'
3）clone_snapshot 'cata_tableSnapshot', 'SYSTEM.CATALOG'
4）drop 'SYSTEM:CATALOG'

重启phoenix