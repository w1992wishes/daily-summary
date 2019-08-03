# 【HBase】HBase 安装

[TOC]

## 一、下载安装

http://www.apache.org/dyn/closer.cgi/hbase

从上述页面，挑选一个 Apache Download Mirror（Apache下载镜像），下载一个Hbase 的稳定发布版本，注意看是否适配 Hadoop 版本。

解压：

```shell
tar -zxv -f hbase-2.0.5-bin.tar.gz
```

设置 Hbase 环境变量，并使其生效：

```sh
# hbase env
export HBASE_HOME=/home/hadoop/hbase
export PATH=$PATH:$HBASE_HOME/bin
```

获取 Hbase 选项列表及版本信息：

```shell
hbase version
```

## 二、配置 hbase

**修改及配置 hbase/conf 目录下的 hbase-env.sh 文件。**

```sh
export JAVA_HOME=/opt/jdk1.8.0_161
export HBASE_MANAGES_ZK=false
```

其中 HBASE_MANAGES_ZK=false 表示我们使用自己安装 zookeeper 集群而不是 hbase 自带的 zookeeper 集群。

**修改及配置 hbase/conf 目录下的 hbase-site.xml 文件。**

```xml
<configuration>
    <property>
        <name>hbase.zookeeper.property.clientPort</name>
        <value>2181</value>
    </property>
    <property>
        <name>hbase.zookeeper.quorum</name>
        <value>master</value>
    </property>
    <property>
        <name>hbase.zookeeper.property.dataDir</name>
        <value>/var/zookeeper</value>
    </property>
    <property>
        <name>hbase.rootdir</name>
        <value>hdfs://master:9000/hbase</value>
    </property>
    <property>
        <name>hbase.cluster.distributed</name>
        <value>true</value>
    </property>
</configuration>
```

**修改 regionservers，添加 hbase 集群服务器的 ip 或者 hostname。**

```
master
slave1
slave2
```

## 三、复制 hbase 到集群

```shell
scp -r hbase hadoop@slave1:/home/hadoop
scp -r hbase hadoop@slave2:/home/hadoop
```

## 四、启动 hbase

```shell
start-hbase.sh
```

在哪台服务器使用上述命令启动则那台服务器即为 master 节点，使用 jps 命令查看启动情况。

```
5600 Jps
2214 SecondaryNameNode
4984 HRegionServer
4808 HMaster
4348 QuorumPeerMain
2029 NameNode
2397 ResourceManager

```

在 master 上启动了 HMaster 和 HRegionServer。

hbase集群安装和启动完成，此时可以通过Web页面查看Hbase集群情况：http://master:16010

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g3rj3kzqogj21hc0d1t9q.jpg)

启动后，还可以在集群中找任意一台机器启动一个备用的 master。

```shell
bin/hbase-daemon.sh start master

```

