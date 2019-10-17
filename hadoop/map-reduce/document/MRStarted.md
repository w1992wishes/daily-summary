## Hadoop MapReduce

### MR 简单使用

在 windows 中运行报错，可在 VM Options 添加： -DHADOOP_USER_NAME=hadoop

* MaxTemperature：简单计算最大温度例子
* MaxTemperatureDriver：本地简单计算最大温度例子
* MaxTemperatureTest：用 MRUnit 写单元测试

如果要放到集群中运行：
1. mvn clean package -DskipTests
2. 在集群中运行 hadoop jar map-reduce-1.0-SNAPSHOT.jar me.w1992wishes.hadoop.map.reduce.MaxTemperature
3. 如果有其他的依赖，还需要指定依赖

### MysqlMR：Hadoop 读写 mysql 例子

写一个 MySQL 与 MapReduce 之间交互的类，需要实现 Writable 和 DBWritable 接口， Writable 是为了与 MapReduce 进行对接，而 DBWritable 是为了与 MySQL 进行对接。

这两个功能要实现的函数是比较类似的，都要实现 write 和 readFields 函数。作用顾名思义，前者用于将一条数据写入数据流或者数据库中，而后者则从数据流/库中读取数据。

**在提交到集群运行时，出现没找到 mysql 驱动的异常。**

针对客户端未找到类，有两种方式解决：

* 将需要的 jar 包放到  $HADOOP_HOME/share/hadoop/common/lib 中
* 或者设置 HADOOP_CLASSPATH(只对客户端生效，是客户端的类路径)：export HADOOP_CLASSPATH=../..jar，再运行即可

针对 Map 或者 Reduce 任务未找到类，因为是在各自的 jvm 中运行，设置 HADOOP_CLASSPATH 不生效，可如下：

* 将依赖第三方包都 copy 到 lib 目录下，然后将 lib 目录和代码一起打成 jar 包，或者在打成的 jar 包中将 lib 目录移动进去
* 使用 -libjars (The -libjars option allows applications to add jars to the classpaths of the maps and reduces)，我这边试验
一直不成功，后续再看看