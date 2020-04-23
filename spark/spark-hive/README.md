## spark on hive demo

1. 引入依赖

```xml
<!-- spark -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql_2.11</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-core_2.11</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-hive_2.11</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>
```

2. resources 下引入 hive-site.xml

3. 编写程序

4. 根据 pom.xml 提供的 maven 插件打包，mvn clean package

5. 上传服务器运行，如果出现权限问题，切换到 hdfs 用户