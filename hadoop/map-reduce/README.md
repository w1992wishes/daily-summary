## Hadoop MapReduce

在 windows 中运行报错，可在 VM Options 添加： -DHADOOP_USER_NAME=hadoop

* MaxTemperature：简单计算最大温度例子
* MaxTemperatureDriver：本地简单计算最大温度例子
* MaxTemperatureTest：用 MRUnit 写单元测试

如果要放到集群中运行：
1. mvn clean package -DskipTests
2. 在集群中运行 hadoop jar map-reduce-1.0-SNAPSHOT.jar me.w1992wishes.hadoop.map.reduce.MaxTemperature
3. 如果有其他的依赖，还需要指定依赖

