# 【Hive】Hive 2.3.4 安装

[TOC]

## 一、前期工作

- 安装 JDK
- 安装 Hadoop
- 安装 MySQL（ https://blog.csdn.net/xiangwanpeng/article/details/54562362 ）

## 二、下载安装

### 2.1、下载

下载 **apache-hive-2.3.4-bin.tar.gz** ( https://archive.apache.org/dist/hive/hive-2.3.4/ )

### 2.2、安装

将下载的 tar 包解压：

```shell
tar -xzvf apache-hive-2.3.4-bin.tar.gz
```

### 2.3、设置环境变量

```shell
vim ~/.bashrc
```

添加如下内容：

```sh
# Hive
export HIVE_HOME=/home/hadoop/hive-2.3.4
export PATH=$PATH:$HIVE_HOME/bin
```

 使环境变量生效: 

```shell
source ~/.bashrc 
```

 为确保配置成功进行验证，如果显有版本号则以上步骤皆为正常：

```shell
hive --version
```

## 三、 创建 hive 所要的 hdfs 目录 

```shell
hdfs dfs -mkdir -p /user/hive/warehouse
hdfs dfs -mkdir -p /user/hive/tmp
hdfs dfs -mkdir -p /user/hive/log
hdfs dfs -chmod 777 /user/hive/warehouse
hdfs dfs -chmod 777 /usr/hive/tmp
hdfs dfs -chmod 777 /usr/hive/log
```

 创建完成后可以使用`hdfs dfs -ls /user/hive` 查看是否已经成功新建文件夹。 

## 四、修改 hive 所需配置

### 4.1、配置文件 hive.site.xml

进入`Hive/conf`文件夹 复制示例配置文件并改名为`hive.site.xml` ：

在开头添加如下配置：

```xml
<property>
   <name>system:java.io.tmpdir</name>
   <value>/tmp/hive/java</value>
</property>
<property>
   <name>system:user.name</name>
   <value>${user.name}</value>
</property>
```

然后把新建的 HDFS 文件夹的路径添加到配置文件中去（配置都有，只需替换值）：

```xml
  <!-- 资源临时文件存放位置 -->
  <property>
    <name>hive.exec.scratchdir</name>
    <value>/user/hive/tmp</value>
    <description>HDFS root scratch dir for Hive jobs which gets created with write all (733) permission. For each connecting user, an HDFS scratch dir: ${hive.exec.scratchdir}/&lt;username&gt; is created, with ${hive.scratch.dir.permission}.</description>
  </property>
  <!-- 设置 hive 仓库的 HDFS上的位置 -->
  <property>
    <name>hive.metastore.warehouse.dir</name>
    <value>/user/hive/warehouse</value>
    <description>location of default database for the warehouse</description>
  </property>
  <!-- 设置日志位置 -->
  <property>
    <name>hive.querylog.location</name>
    <value>/user/hive/log</value>
    <description>Location of Hive run time structured log file</description>
  </property>
```

### 4.2、创建及配置 Mysql

#### 4.2.1、创建 Hive 数据库

假定已经安装好 MySQL。需要创建一个 hive 数据库用来存储 Hive 元数据。

```sql
CREATE DATABASE hive; 
USE hive; 
CREATE USER 'hadoop'@'localhost' IDENTIFIED BY 'hadoop';
GRANT ALL ON hive.* TO 'hadoop'@'localhost' IDENTIFIED BY 'hadoop'; 
GRANT ALL ON hive.* TO 'hadoop'@'%' IDENTIFIED BY 'hadoop'; 
FLUSH PRIVILEGES; 
quit;
​``` 
```

#### 4.2.2、配置 Hive 数据库

将刚刚创建的数据库及用户名和密码写入配置文件 `hive-site.xml`（实际替换即可）：

```xml
<property>
    <name>javax.jdo.option.ConnectionURL</name>
    <value>jdbc:mysql://localhost:3306/hive?createDatabaseIfNotExist=true&amp;characterEncoding=UTF-8&amp;useSSL=false</value>
</property>
<property>
    <name>javax.jdo.option.ConnectionDriverName</name>
    <value>com.mysql.jdbc.Driver</value>
</property>
<property>
    <name>javax.jdo.option.ConnectionUserName</name>
    <value>hadoop</value>
</property>
<property>
    <name>javax.jdo.option.ConnectionPassword</name>
    <value>hadoop</value>
</property>
```

#### 4.2.3、拷贝 JDBC 驱动包

将 mysql-connector-java-5.1.47.jar 驱动放到  $HIVE_HOME/lib 目录下。

#### 4.2.4、初始化 Hive 数据库

```shell
schematool -dbType mysql -initSchema
```

运行后终端将会显示如下信息 :

```shell
Metastore connection URL:	 jdbc:mysql://192.168.xx.xx:3306/hive?createDatabaseIfNotExist=true&characterEncoding=UTF-8&useSSL=false
Metastore Connection Driver :	 com.mysql.jdbc.Driver
Metastore connection User:	 hadoop
Starting metastore schema initialization to 2.3.0
Initialization script hive-schema-2.3.0.mysql.sql
Initialization script completed
schemaTool completed
```

### 4.3、 配置文件 hive-env.sh

修改 hive conf 目录下 `hive-env.sh.template` 为 `hive-env.sh`： 

```shell
cp hive-env.sh.template hive-env.sh 
```

修改其中内容：

```sh
# hdoop 安装路径
HADOOP_HOME=/home/hadoop/hadoop-2.9.2
# Hive Configuration Directory can be controlled by:
# hive 配置文件存放路径
export HIVE_CONF_DIR=/home/hadoop/hive-2.3.4/conf 
# Folder containing extra libraries required for hive compilation/execution can be controlled by:
# hive 运行资源库路径
export HIVE_AUX_JARS_PATH=/home/hadoop/hive-2.3.4/lib
```

## 五、使用Hive CLI

在终端输入`Hive`，便可以进入。

创建一个table：

```sql
CREATE TABLE pokes (foo INT, bar STRING);
```

show tables 和 desc pokes 显示信息如下：

```shell
hive> show tables;
OK
pokes
Time taken: 0.462 seconds, Fetched: 1 row(s)
hive> desc pokes;
OK
foo                 	int                 	                    
bar                 	string              	                    
Time taken: 0.07 seconds, Fetched: 2 row(s)

```

## 六、slaves 节点的配置

之前的步骤均在 hadoop 的 master 节点上配置，现在需要配置 slaves 节点。 

1. 使用 scp 命令，拷贝 hive 至 salves 节点 

```shell
scp -r hive-2.3.4 ds073:/home/hadoop
scp -r hive-2.3.4 ds074:/home/hadoop
scp -r hive-2.3.4 ds075:/home/hadoop

```

2. 添加 hive 系统环境变量 

参考之前的步骤，在 slaves 节点上的 ~/.bashrc 文件中添加系统环境变量，并使用`source` 命令使其生效。 

3. 在 slaves 节点的中 hive-site.xml 添加以下配置

```xml
  <property>
    <name>hive.metastore.uris</name>
    <value>thrift://192.168.11.72:9083</value> 
  </property>
```

4.  启动启动`metastore`服务 

在使用 slaves 节点访问 hive 之前，在 master 节点中，执行 `hive --service metastore &` 来启动`metastore`服务。

 ```shell
hadoop@ds072:~$ hive --service metastore &
[1] 136874

hadoop@ds072:~$ jps
64832 QuorumPeerMain
18226 DataNode
17474 JournalNode
18037 NameNode
137060 Jps
21574 JobHistoryServer
136874 RunJar
79469 HMaster
79743 HRegionServer
18687 DFSZKFailoverController
 ```

5.  slaves 节点启动 hive 并执行简单的 hive 命令 

```shell
hadoop@ds073:~$ hive

hive> show tables;
OK
pokes
pokes_bak
```

