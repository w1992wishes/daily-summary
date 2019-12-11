# 【Hadoop】Hadoop 支持 lzo 完整过程

## 一、安装 lzop native library

- On Mac OS:

  ```
  sudo port install lzop lzo2
  ```

- On RH or CentOS:

  ```
  sudo yum install lzo liblzo-devel
  ```

- On Debian or ubuntu:

  ```
  sudo apt-get install liblzo2-dev
  ```

  接着如下步骤：

   

  ```shell
  # 下载
  wget http://www.oberhumer.com/opensource/lzo/download/lzo-2.06.tar.gz
  # 解压
  tar -zxvf lzo-2.06.tar.gz
  # 进入目录
  cd lzo-2.06
  # export
  export CFLAGS=-m64
  # 指定编译之后的位置
  ./configure -enable-shared -prefix=/usr/local/hadoop/lzo/
  # 开始编译安装
  make && sudo make install
  ```

编译完 lzo 包之后，会在 /usr/local/hadoop/lzo/ 生成一些文件。 

将 /usr/local/hadoop/lzo 目录下的所有文件打包，并同步到集群中的所有机器上。 

## 二、安装 hadoop-lzo

```shell
# 下载
wget https://github.com/twitter/hadoop-lzo/archive/master.zip
# 解压后的文件夹名为hadoop-lzo-master
unzip master.zip
# 然后进入hadoop-lzo-master目录，依次执行下面的命令
cd hadoop-lzo-master

export CFLAGS=-m64
export CXXFLAGS=-m64
export C_INCLUDE_PATH=/usr/local/hadoop/lzo/include
export LIBRARY_PATH=/usr/local/hadoop/lzo/lib

mvn clean package -Dmaven.test.skip=true

cd target/native/Linux-amd64-64

# 会在~目录下生成几个文件
tar -cBf - -C lib . | tar -xBvf - -C ~

cp ~/libgplcompression* $HADOOP_HOME/lib/native/

# 需要把hadoop-lzo-0.4.21-SNAPSHOT.jar 复制到hadoop中
cp target/hadoop-lzo-0.4.21-SNAPSHOT.jar $HADOOP_HOME/share/hadoop/common/
cp target/hadoop-lzo-0.4.21-SNAPSHOT.jar $HADOOP_HOME/share/hadoop/mapreduce/lib

# 最后别忘记将下面三个地址的文件复制到其他节点
 $HADOOP_HOME/lib/native/libgplcompression*
 $HADOOP_HOME/share/hadoop/mapreduce/lib/hadoop-lzo-0.4.21-SNAPSHOT.jar
 $HADOOP_HOME/share/hadoop/common/hadoop-lzo-0.4.21-SNAPSHOT.jar
```

## 三、配置 hadoop 环境变量

在 Hadoop 中的 $HADOOP_HOME/etc/hadoop/hadoop-env.sh 加上下面配置， vim $HADOOP_HOME/etc/hadoop/hadoop-env.sh ：

```shell
export LD_LIBRARY_PATH=/usr/local/hadoop/lzo/lib
export HADOOP_CLASSPATH="<extra_entries>:$HADOOP_CLASSPATH:${HADOOP_HOME}/share/hadoop/common"
export JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}:${HADOOP_HOME}/lib/native
```

在 $HADOOP_HOME/etc/hadoop/core-site.xml 加上如下配置， vim $HADOOP_HOME/etc/hadoop/core-site.xml ：

```xml
<property>
    <name>io.compression.codecs</name>
    <value>org.apache.hadoop.io.compress.GzipCodec,
           org.apache.hadoop.io.compress.DefaultCodec,
           com.hadoop.compression.lzo.LzoCodec,
           com.hadoop.compression.lzo.LzopCodec,
           org.apache.hadoop.io.compress.BZip2Codec
        </value>
</property>

<property>
    <name>io.compression.codec.lzo.class</name>
    <value>com.hadoop.compression.lzo.LzoCodec</value>
</property>
```

在 $HADOOP_HOME/etc/hadoop/mapred-site.xml 加上如下配置， vim $HADOOP_HOME/etc/hadoop/mapred-site.xml ：

```xml
<property>
    <name>mapred.compress.map.output</name>
    <value>true</value>
</property>

<property>
    <name>mapred.map.output.compression.codec</name>
    <value>com.hadoop.compression.lzo.LzoCodec</value>
</property>

<property>
    <name>mapred.child.env</name>
    <value>LD_LIBRARY_PATH=/usr/local/hadoop/lzo/lib</value>
</property>

<property>
    <name>mapreduce.map.env </name>
    <value>LD_LIBRARY_PATH=/usr/local/hadoop/lzo/lib</value>
</property>

<property>
    <name>mapreduce.reduce.env </name>
    <value>LD_LIBRARY_PATH=/usr/local/hadoop/lzo/lib</value>
</property>
```

将刚刚修改的配置文件全部同步到集群的所有机器上，并重启 Hadoop 集群，这样就可以在 Hadoop 中使用 lzo。 

## 四、配置 Spark

need merely to append two path values to `spark-env.sh`:

```sh
export SPARK_LIBRARY_PATH=$SPARK_LIBRARY_PATH:/usr/local/hadoop/lzo/lib
export SPARK_CLASSPATH=$SPARK_CLASSPATH:/path/to/your/hadoop-lzo/java/libs
```