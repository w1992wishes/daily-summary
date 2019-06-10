# Hbase 简单使用

在进行 Hbase 开发前，需要安装 JDK、Hadoop 和 HBase。

## HBaseUtils 

主要是介绍 HBase 的基本使用，包括建表、插入表数据、删除表数据、获取一行数据、表扫描、删除列族、删除表。

首先需要设置 HBase 的配置，如 ZooKeeper 的地址、端口号等等。

可以通过 org.apache.hadoop.conf.Configuration.set 方法手工设置 HBase 的配置信息，也可以直接将 HBase 的 hbase-site.xml 配置文件引入项目。

```java
// 声明静态配置
private static Configuration conf = null;
static {
    conf = HBaseConfiguration.create();
    conf.set("hbase.zookeeper.quorum", "localhost");
    conf.set("hbase.zookeeper.property.clientPort", "2181");
}
```

具体操作看代码。

### 重要类

**org.apache.hadoop.hbase.HBaseConfiguration**

通过此类可以对HBase进行配置

**org.apache.hadoop.hbase.client.HBaseAdmin**

提供一个接口来管理HBase数据库中的表信息。它提供创建表、删除表等方法

**org.apache.hadoop.hbase.client.HTableDescriptor**

包含了表的名字及其对应列族。 提供的方法有

    void                addFamily(HColumnDescriptor)              添加一个列族
    HColumnDescriptor   removeFamily(byte[] column)               移除一个列族
    byte[]              getName()                                 获取表的名字
    byte[]              getValue(byte[] key)                      获取属性的值
    void                setValue(String key,String value)         设置属性的值
    
**org.apache.hadoop.hbase.client.HColumnDescriptor**

维护关于列的信息。提供的方法有

    byte[]              getName()                                 获取列族的名字
    byte[]              getValue()                                获取对应的属性的值
    void                setValue(String key,String value)         设置对应属性的值
    
**org.apache.hadoop.hbase.client.HTable**

用户与HBase表进行通信。此方法对于更新操作来说是非线程安全的，如果启动多个线程尝试与单个HTable实例进行通信，那么写缓冲器可能会崩溃

**org.apache.hadoop.hbase.client.Put**

用于对单个行执行添加操作

**org.apache.hadoop.hbase.client.Get**

用于获取单个行的相关信息

**org.apache.hadoop.hbase.client.Result**

存储Get或Scan操作后获取的单行值

**org.apache.hadoop.hbase.client.ResultScanner**

客户端获取值的接口

