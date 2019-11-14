# 【Spark】Spark 之 HiveSupport 连接

[TOC]

## 一、spark-shell

### 1.1、拷贝配置文件

1. 拷贝 hive/conf/hdfs-site.xml  到 spark/conf/  下
2. 拷贝 hive/lib/mysql  到 spark/jars/ 下

### 1.2、启动 spark-shell

运行命令：

```shell
spark-shell --master yarn --deploy-mode client
```

启动后，运行如下代码：

```scala
spark.sql("show databases").show()
```

如果出现如下错误：

> Caused by: org.apache.hadoop.hive.metastore.api.MetaException: Hive Schema version 1.2.0 does not match metastore's schema version 2.3.0 Metastore is not upgraded or corrupt

可在 hive-site.xml 中添加（复制到 spark/conf 下的也需要修改）：

```xml
  <property>
    <name>hive.metastore.schema.verification</name>
    <value>false</value>
  </property>
```

再次启动后运行代码：

```shell
spark.sql("show databases").show()
spark.sql("use test")
spark.sql("select * from student").show()
```

## 二、IDEA 连接 Hive

### 2.1、引入依赖

spark.version 是 2.3.1

```xml
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-core_2.11</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql_2.11</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-hive_2.11</artifactId>
    <version>${spark.version}</version>
</dependency>
<dependency><!--数据库驱动：Mysql-->
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>5.1.47</version>
</dependency>
```

### 2.2、拷贝配置文件

 拷贝 hive-site.xml 到项目的 resources 目录下即可，如果是高可用集群，可能需要将 hdfs-site.xml 和 core-xite.xml 一同拷贝到 resources 目录下。

### 2.3、运行代码实例

运行一段代码，注意 pom 中是 provided，直接运行可能会找不到包，可以先注释，打包时再加上：

```scala
object SparkHivePartition {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("SparkHive")
      .master("local")
      .config("spark.sql.parquet.writeLegacyFormat", value = true)
      .enableHiveSupport()
      .getOrCreate()

    import spark.sql

    val data = Array(("001", "张三", 21, "2018"), ("002", "李四", 18, "2017"))

    val df = spark.createDataFrame(data).toDF("id", "name", "age", "year")
    //创建临时表
    df.createOrReplaceTempView("temp_table")

    //切换hive的数据库
    sql("use wqf")
    // 1、创建分区表，可以将append改为overwrite，这样如果表已存在会删掉之前的表，新建表
    df.write.mode("append").partitionBy("year").saveAsTable("new_test_partition")
    //2、向Spark创建的分区表写入数据
    df.write.mode("append").partitionBy("year").saveAsTable("new_test_partition")
    sql("insert into new_test_partition select * from temp_table")
    df.write.insertInto("new_test_partition")

    //开启动态分区
    sql("set hive.exec.dynamic.partition.mode=nonstrict")
    //3、向在Hive里用Sql创建的分区表写入数据，抛出异常
    //    df.write.mode("append").partitionBy("year").saveAsTable("test_partition")

    // 4、解决方法
    df.write.mode("append").format("Hive").partitionBy("year").saveAsTable("test_partition")

    sql("insert into test_partition select * from temp_table")
    df.write.insertInto("test_partition")
    //这样会抛出异常
    //    df.write.partitionBy("year").insertInto("test_partition")
    spark.stop
  }
}
```

如果运行中发现缺少权限，可以再 VM 上添加如下：

```
-DHADOOP_USER_NAME=hadoop
```

## 三、参考

 https://www.jianshu.com/p/983ecc768b55 