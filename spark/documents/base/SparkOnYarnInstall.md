# 【Spark】Spark On Yarn 安装

[TOC]

## 一、下载和解压

 https://archive.apache.org/dist/spark/spark-2.3.1/ 

```shell
tar zxv -f spark-2.3.1-bin-hadoop2.7.tgz
mv spark-2.3.1-bin-hadoop2.7/ spark-2.3.1
```

## 二、配置

### 2.1、配置 spark-default.conf，按需调整

```conf
spark.eventLog.enabled             true
spark.eventLog.dir                 hdfs://myha01/user/spark/eventLogs
spark.eventLog.compress            true
spark.history.fs.logDirectory      hdfs://myha01/user/spark/eventLogs
spark.yarn.historyServer.address   ds075:18080

spark.serializer                 org.apache.spark.serializer.KryoSerializer

spark.master                    yarn 
spark.driver.cores              2
spark.driver.memory             5g 
spark.executor.cores            2
spark.executor.memory           4g
spark.executor.instances        4

spark.sql.warehouse.dir         hdfs://myha01/user/hive/warehouse

# 用来存放spark的依赖jar包
spark.yarn.jars=hdfs://myha01/user/spark/spark_jars/*
```

* spark.eventLog.enabled：设置true开启日志记录.
*  spark.eventLog.dir：存储日志路径，Application 在运行过程中所有的信息均记录在该属性指定的路径下,我这里设置的是 hdfs 路径(也可以是本地路径如file:///val/log/sparkEventLog)
* spark.yarn.historyServer.address：设置 History Server 的地址和端口，这个链接将会链接到 YARN 检测界面上的 Tracking UI
* spark.history.fs.logDirectory：日志目录和 spark.eventLog.dir 保持一致，Spark History Server 页面只展示该指定路径下的信息
*  spark.eventLog.compress：是否压缩记录Spark事件信息，前提spark.eventLog.enabled 为 true，默认使用的是snappy

### 2.2、配置 spark-env.sh

```sh
export JAVA_HOME=/usr/local/jdk1.8.0_231
export HADOOP_HOME=/home/hadoop/hadoop-2.9.2
export HADOOP_CONF_DIR=${HADOOP_HOME}/etc/hadoop
export SPARK_MASTER_IP=ds072

# spark 日志保存时间
export SPARK_HISTORY_OPTS="-Dspark.history.retainedApplications=10"
```

### 2.3、配置 slaves

```sh
cp slaves.template slaves
vim slaves
```

在文件末尾直接添加配置内容即可，配置示例如下： 

### 2.4、创建目录

 ```shell
hdfs dfs -mkdir -p /user/spark/jobs/history
hdfs dfs -mkdir -p /user/spark/spark_jars
hdfs dfs -mkdir -p /user/spark/eventLogs
 ```

 spark-defaults.conf 中配置的目录，用来存放 spark 的依赖jar包，需要进入 Spark 的 jars 目录，执行如下命令上传 jar 包：

```shell
hdfs dfs -put ./* /user/spark/spark_jars
```

### 2.5、拷贝到其他节点

```shell
scp -r spark-2.3.1 ds073:/home/hadoop
scp -r spark-2.3.1 ds074:/home/hadoop
scp -r spark-2.3.1 ds075:/home/hadoop
```

### 2.6、配置环境变量

```shell
vim ~/.bashrc 

# spark
export SPARK_HOME=/home/hadoop/spark-2.3.1
export PATH=$PATH:$SPARK_HOME/bin

source ~/.bashrc 
```

## 三、启动与测试

### 3.1、启动 Standalone 模式

```shell
${SPARK_HOME}/sbin/start-all.sh
```

然后登陆 8080 页面查看 ui，如果访问不了可以查看日志确定失败原因。

测试一下 Standalone 模式：

```shell
spark-submit  \
--class org.apache.spark.examples.SparkPi   \
--master spark://ds072:7077   \
${SPARK_HOME}/examples/jars/spark-examples_2.11-2.3.1.jar 100
```

### 3.2、 启动 spark 的 history-server 



### 3.3、测试 Yarn 模式

```shell
spark-shell --master yarn-client
```

如果有报错类似：

> Caused by: java.io.IOException: Failed to send RPC 6405368361626935580 to /192.168.11.73:31107: java.nio.channels.ClosedChannelException
> 	at org.apache.spark.network.client.TransportClient.lambda$sendRpc$2(TransportClient.java:237)
> 	at io.netty.util.concurrent.DefaultPromise.notifyListener0(DefaultPromise.java:507)
> 	at io.netty.util.concurrent.DefaultPromise.notifyListenersNow(DefaultPromise.java:481)
> 	at io.netty.util.concurrent.DefaultPromise.access$000(DefaultPromise.java:34)
> 	at io.netty.util.concurrent.DefaultPromise$1.run(DefaultPromise.java:431)
> 	at io.netty.util.concurrent.AbstractEventExecutor.safeExecute(AbstractEventExecutor.java:163)
> 	at io.netty.util.concurrent.SingleThreadEventExecutor.runAllTasks(SingleThreadEventExecutor.java:403)
> 	at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:463)
> 	at io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:858)
> 	at io.netty.util.concurrent.DefaultThreadFactory$DefaultRunnableDecorator.run(DefaultThreadFactory.java:138)
> 	at java.lang.Thread.run(Thread.java:748)
> Caused by: java.nio.channels.ClosedChannelException
> 	at io.netty.channel.AbstractChannel$AbstractUnsafe.write(...)(Unknown Source)
> java.lang.IllegalStateException: Spark context stopped while waiting for backend

在 yarn.xml 中添加如下配置：

```xml
<property>
  <name>yarn.nodemanager.pmem-check-enabled</name>
  <value>false</value>
</property>

<property>
  <name>yarn.nodemanager.vmem-check-enabled</name>
  <value>false</value>
</property>
```

也可以运行如下：

```shell
spark-submit \
--class org.apache.spark.examples.SparkPi \
--master yarn \
${SPARK_HOME}/examples/jars/spark-examples_2.11-2.3.1.jar 10
```

