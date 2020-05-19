# 【Hadoop】Hadoop 2.9.2 HA安装

环境：Ubuntu 16.04

Hadoop 版本：2.9.2

## 一、准备

**1.1、所有节点修改 /etc/hosts**

```
192.168.11.72 master
192.168.11.73 node1
192.168.11.74 node2
192.168.11.75 node3
192.168.11.76 node4
```

**1.2、所有的节点安装 JDK**

1.下载对应的 jdk 版本

2.解压到 /usr/local/lib 下， tar -zxvf jdk-xxx.tar.gz -C /usr/local/lib

3.配置系统环境变量，编辑/etc/profile文件，在文件的末尾添加如下：

```
export JAVA_HOME=/usr/local/lib/jdk1.8.0_181
export JRE_HOME=$JAVA_HOME/jre
export CLASSPATH=.:$CLASSPATH:$JAVA_HOME/lib:$JRE_HOME/lib
export PATH=$PATH:$JAVA_HOME/bin:$JRE_HOME/bin
```

4.source /etc/profile命令使刚才配置的信息生效

5.验证是否安装成功  java -version

**1.3、创建账户**

```
sudo useradd -m bigdata -s /bin/bash //添加用户
sudo passwd bigdata  //创建密码
sudo  adduser bigdata sudo   //sudo 授权
```

**1.4、免密 ssh**

```
# 在master 
sudo apt-get install openssh-server
ssh-keygen -t rsa 回车 回车 回车
cat $HOME/.ssh/id_rsa.pub >> $HOME/.ssh/authorized_keys

scp $HOME/.ssh/id_rsa.pub  bigdata@192.168.11.73:.ssh/
scp $HOME/.ssh/id_rsa.pub  bigdata@192.168.11.74:.ssh/

# 在node1,node2 
cat $HOME/.ssh/id_rsa.pub >> $HOME/.ssh/authorized_keys
```

## 二、修改配置

下载 hadoop-2.9.2.tar.gz，解压，修改配置：

hadoop/etc/hadoop/hadoop-env.sh

```sh
vi $HADOOP_HOME/etc/hadoop/hadoop-env.sh

export JAVA_HOME=/usr/local/lib/jdk1.8.0_181
```

将 export JAVA_HOME=${JAVA_HOME} 修改为正确的路径

hadoop/etc/hadoop/core-site.xml

```xml
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://bigdata-ha/</value>
    </property>

    <property>
        <name>hadoop.tmp.dir</name>
        <value>/home/bigdata/data/hadoopdata/</value>
    </property>

    <property>
        <name>ha.zookeeper.quorum</name>
        <value>intellif-bigdata-node1:2181,intellif-bigdata-node2:2181,intellif-bigdata-node3:2181</value>
    </property>

    <property>
        <name>ha.zookeeper.session-timeout.ms</name>
        <value>5000</value>
        <description>ms</description>
    </property>
</configuration>
```

hadoop/etc/hadoop/hdfs-site.xml

```xml
<configuration>

    <property>
        <name>dfs.replication</name>
        <value>3</value>
    </property>

    <!-- 配置namenode和datanode的工作目录-数据存储目录 -->
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>/home/bigdata/data/hadoopdata/dfs/name</value>
    </property>
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>/home/bigdata/data/hadoopdata/dfs/data</value>
    </property>

    <!--指定hdfs的nameservice为bigdata-ha，需要和core-site.xml中的保持一致 
                 dfs.ha.namenodes.[nameservice id]为在nameservice中的每一个NameNode设置唯一标示符。 
        配置一个逗号分隔的NameNode ID列表。这将是被DataNode识别为所有的NameNode。-->
    <property>
        <name>dfs.nameservices</name>
        <value>bigdata-ha</value>
    </property>

    <!-- nameservice 包含哪些namenode，爲各個namenode起名 -->
    <property>
        <name>dfs.ha.namenodes.bigdata-ha</name>
        <value>nn1,nn2</value>
    </property>
    <!-- nn1的RPC通信地址 -->
    <property>
        <!--  名爲nn1的namenode 的rpc地址和端口號，rpc用來和datanode通訊 -->
        <name>dfs.namenode.rpc-address.bigdata-ha.nn1</name>
        <value>intellif-bigdata-master:9000</value>
    </property>
    <property>
        <!-- 名爲nn2的namenode 的rpc地址和端口號，rpc用來和datanode通訊  -->
        <name>dfs.namenode.rpc-address.bigdata-ha.nn2</name>
        <value>intellif-bigdata-node1:9000</value>
    </property>
    <!-- nn1的http通信地址 -->
    <property>
        <!--名爲nn1的namenode 的http地址和端口號，web客戶端 -->
        <name>dfs.namenode.http-address.bigdata-ha.nn1</name>
        <value>intellif-bigdata-master:50070</value>
    </property>
    <property>
        <!--名爲nn2的namenode 的http地址和端口號，web客戶端 -->
        <name>dfs.namenode.http-address.bigdata-ha.nn2</name>
        <value>intellif-bigdata-node1:50070</value>
    </property>
    <!-- 指定NameNode的edits元数据的共享存储位置。也就是JournalNode列表 
                 该url的配置格式：qjournal://host1:port1;host2:port2;host3:port3/journalId 
        journalId推荐使用nameservice，默认端口号是：8485 -->
    <property>
        <name>dfs.namenode.shared.edits.dir</name>
        <value>qjournal://intellif-bigdata-master:8485;intellif-bigdata-node1:8485;intellif-bigdata-node2:8485/bigdata-ha</value>
    </property>
    <!-- 指定JournalNode在本地磁盘存放数据的位置 -->
    <property>
        <name>dfs.journalnode.edits.dir</name>
        <value>/home/bigdata/data/journaldata</value>
    </property>
    <!-- 开启NameNode失败自动切换 -->
    <property>
        <name>dfs.ha.automatic-failover.enabled</name>
        <value>true</value>
    </property>
    <!-- 配置失败自动切换实现方式 -->
    <property>
        <name>dfs.client.failover.proxy.provider.bigdata-ha</name>
        <value>org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider</value>
    </property>
    <!-- 配置隔离机制方法，多个机制用换行分割，即每个机制暂用一行 -->
    <property>
        <name>dfs.ha.fencing.methods</name>
        <value>
            sshfence
            shell(/bin/true)
        </value>
    </property>
    <!-- 使用sshfence隔离机制时需要ssh免登陆 -->
    <property>
        <name>dfs.ha.fencing.ssh.private-key-files</name>
        <value>/home/bigdata/.ssh/id_rsa</value>
    </property>
    <!-- 配置sshfence隔离机制超时时间 -->
    <property>
        <name>dfs.ha.fencing.ssh.connect-timeout</name>
        <value>30000</value>
    </property>

    <property>
        <name>ha.failover-controller.cli-check.rpc-timeout.ms</name>
        <value>60000</value>
    </property>
</configuration>
```

hadoop/etc/hadoop/yarn-site.xml

```xml
<configuration>

    <!-- 开启RM高可用 -->
    <property>
        <name>yarn.resourcemanager.ha.enabled</name>
        <value>true</value>
    </property>
    <!-- 指定RM的cluster id -->
    <property>
        <name>yarn.resourcemanager.cluster-id</name>
        <value>yarn-cluster</value>
    </property>
    <!-- 指定RM的名字 -->
    <property>
        <name>yarn.resourcemanager.ha.rm-ids</name>
        <value>rm1,rm2</value>
    </property>
    <!-- 分别指定RM的地址 -->
    <property>
        <!--  指定第一個節點的所在機器 -->
        <name>yarn.resourcemanager.hostname.rm1</name>
        <value>intellif-bigdata-master</value>
    </property>
    <property>
        <!--  指定第二個節點所在機器 -->
        <name>yarn.resourcemanager.hostname.rm2</name>
        <value>intellif-bigdata-node2</value>
    </property>
    <!-- 指定zk集群地址 -->
    <property>
        <name>yarn.resourcemanager.zk-address</name>
        <value>intellif-bigdata-node1:2181,intellif-bigdata-node2:2181,intellif-bigdata-node3:2181</value>
    </property>

    <property>
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
    </property>
    <property>
        <name>yarn.log-aggregation-enable</name>
        <value>true</value>
    </property>
    <property>
        <name>yarn.log-aggregation.retain-seconds</name>
        <value>106800</value>
    </property>
    <property>
        <name>yarn.log.server.url</name>
        <value>http://intellif-bigdata-master:19888/jobhistory/logs</value>
    </property>

    <!-- 启用自动恢复 -->
    <property>
        <name>yarn.resourcemanager.recovery.enabled</name>
        <value>true</value>
    </property>
    <!-- 指定resourcemanager的状态信息存储在zookeeper集群上 -->
    <property>
        <name>yarn.resourcemanager.store.class</name>
        <value>org.apache.hadoop.yarn.server.resourcemanager.recovery.ZKRMStateStore</value>
    </property>

    <property>
        <!-- 客戶端通過該地址向RM提交對應用程序操作 -->
        <name>yarn.resourcemanager.address.rm1</name>
        <value>intellif-bigdata-master:8032</value>
    </property>
    <property>
        <!--ResourceManager 對ApplicationMaster暴露的訪問地址。ApplicationMaster通過該地址向RM申請資源、釋放資源等。 -->
        <name>yarn.resourcemanager.scheduler.address.rm1</name>
        <value>intellif-bigdata-master:8030</value>
    </property>
    <property>
        <!-- RM HTTP訪問地址,查看集羣信息-->
        <name>yarn.resourcemanager.webapp.address.rm1</name>
        <value>intellif-bigdata-master:8088</value>
    </property>
    <property>
        <!-- NodeManager通過該地址交換信息 -->
        <name>yarn.resourcemanager.resource-tracker.address.rm1</name>
        <value>intellif-bigdata-master:8031</value>
    </property>
    <property>
        <!--管理員通過該地址向RM發送管理命令 -->
        <name>yarn.resourcemanager.admin.address.rm1</name>
        <value>intellif-bigdata-master:8033</value>
    </property>
    <property>
        <name>yarn.resourcemanager.ha.admin.address.rm1</name>
        <value>intellif-bigdata-master:23142</value>
    </property>

    <property>
        <name>yarn.resourcemanager.address.rm2</name>
        <value>intellif-bigdata-node2:8032</value>
    </property>
    <property>
        <name>yarn.resourcemanager.scheduler.address.rm2</name>
        <value>intellif-bigdata-node2:8030</value>
    </property>
    <property>
        <name>yarn.resourcemanager.webapp.address.rm2</name>
        <value>intellif-bigdata-node2:8088</value>
    </property>
    <property>
        <name>yarn.resourcemanager.resource-tracker.address.rm2</name>
        <value>intellif-bigdata-node2:8031</value>
    </property>
    <property>
        <name>yarn.resourcemanager.admin.address.rm2</name>
        <value>intellif-bigdata-node2:8033</value>
    </property>
    <property>
        <name>yarn.resourcemanager.ha.admin.address.rm2</name>
        <value>intellif-bigdata-node2:23142</value>
    </property>
    
    <property>
        <name>yarn.scheduler.maximum-allocation-mb</name>
        <value>30720</value>
    </property>
    <property>
        <name>yarn.nodemanager.resource.memory-mb</name>
        <value>30720</value>
    </property>

    <property>
        <name>yarn.nodemanager.pmem-check-enabled</name>
        <value>false</value>
    </property>
    <property>
        <name>yarn.nodemanager.vmem-check-enabled</name>
        <value>false</value>
    </property>
    <property>
        <!-- Maximum resources to allocate to application masters
       If this is too high application masters can crowd out actual work -->
        <name>yarn.scheduler.capacity.maximum-am-resource-percent</name>
       <value>0.6</value>
    </property>
</configuration>
```

hadoop/etc/hadoop/mapred-site.xml

```xml
<configuration>
    <!-- 指定mr框架为yarn方式 -->
    <property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
    </property>

    <!-- 指定mapreduce jobhistory地址 -->
    <property>
        <name>mapreduce.jobhistory.address</name>
        <value>intellif-bigdata-master:10020</value>
        <description>MapReduce JobHistory Server IPC host:port</description>
    </property>

    <!-- 任务历史服务器的web地址 -->
    <property>
        <name>mapreduce.jobhistory.webapp.address</name>
        <value>intellif-bigdata-master:19888</value>
        <description>MapReduce JobHistory Server Web UI host:port</description>

    </property>
    <property>
        <name>mapred.compress.map.output</name>
        <value>true</value>
    </property>
</configuration>
```

hadoop/etc/hadoop/slaves

```
intellif-bigdata-node1
intellif-bigdata-node2
intellif-bigdata-node3
```

环境变量 ~/.bashrc

```sh
HADOOP_HOME=/home/bigdata/hadoop
export HADOOP_INSTALL=$HADOOP_HOME
export HADOOP_MAPRED_HOME=$HADOOP_HOME
export HADOOP_COMMON_HOME=$HADOOP_HOME
export HADOOP_HDFS_HOME=$HADOOP_HOME
export YARN_HOME=$HADOOP_HOME
export HADOOP_COMMON_LIB_NATIVE_DIR=$HADOOP_HOME/lib/native
export HADOOP_OPTS="-Djava.library.path=$HADOOP_HOME/lib:$HADOOP_COMMON_LIB_NATIVE_DIR"
export PATH=$PATH:$HADOOP_HOME/sbin:$HADOOP_HOME/bin
```

source ~/.bashrc

## 三、分发

复制所有的 hadoop 文件夹到其他节点**

```
scp -r hadoop bigdata@intellif-bigdata-node1:/home/bigdata/
scp -r hadoop bigdata@intellif-bigdata-node2:/home/bigdata/
scp -r hadoop bigdata@intellif-bigdata-node3:/home/bigdata/
```

给所有节点 配置 Hadoop 环境变量， master 前面已经配置，这里只配其他的节点就好

```sh
HADOOP_HOME=/home/bigdata/hadoop
export HADOOP_INSTALL=$HADOOP_HOME
export HADOOP_MAPRED_HOME=$HADOOP_HOME
export HADOOP_COMMON_HOME=$HADOOP_HOME
export HADOOP_HDFS_HOME=$HADOOP_HOME
export YARN_HOME=$HADOOP_HOME
export HADOOP_COMMON_LIB_NATIVE_DIR=$HADOOP_HOME/lib/native
export HADOOP_OPTS="-Djava.library.path=$HADOOP_HOME/lib:$HADOOP_COMMON_LIB_NATIVE_DIR"
export PATH=$PATH:$HADOOP_HOME/sbin:$HADOOP_HOME/bin
```

source  ~/.bashrc

## 四、启动

启动zookeeper

启动journal(每个机器都执行)

```sh
cd /home/bigdata/data
rm -rf hadoopdata
hadoop-daemon.sh stop journalnode
hadoop-daemon.sh start journalnode
```

进入master的~/hadoop目录，执行以下操作格式化namenode

```shell
hdfs namenode -format
```

格式化namenode，第一次启动服务前执行的操作，以后不需要执行。

复制元数据到另外一个namenode

```sh
cd /home/bigdata/data/hadoopdata/dfs
scp -r name/ bigdata@intellif-bigdata-node1:$PWD
```

格式化ZK(在master上执行即可)

```sh
hdfs zkfc -formatZK
```

启动 hdfs

```sh
start-dfs.sh 
```

查看集群状态：

```sh
hadoop dfsadmin -report
```

查看namenode failover 

```sh
hdfs haadmin -failover nn1 nn2
```

启动 yarn

```sh
start-yarn.sh
```