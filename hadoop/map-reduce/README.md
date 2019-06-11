## Hadoop MapReduce

在 windows 中运行报错，可在 VM Options 添加： -DHADOOP_USER_NAME=hadoop

* MaxTemperature：简单计算最大温度例子
* MaxTemperatureDriver：本地简单计算最大温度例子
* MaxTemperatureTest：用 MRUnit 写单元测试

如果要放到集群中运行：
1. mvn clean package -DskipTests
2. 在集群中运行 hadoop jar map-reduce-1.0-SNAPSHOT.jar me.w1992wishes.hadoop.map.reduce.MaxTemperature
3. 如果有其他的依赖，还需要指定依赖

* MysqlMR：Hadoop 读写 mysql 例子

写一个 MySQL 与 MapReduce 之间交互的类，需要实现 Writable 和 DBWritable 接口， Writable 是为了与 MapReduce 进行对接，而 DBWritable 是为了与 MySQL 进行对接。

这两个功能要实现的函数是比较类似的，都要实现 write 和 readFields 函数。作用顾名思义，前者用于将一条数据写入数据流或者数据库中，而后者则从数据流/库中读取数据。
