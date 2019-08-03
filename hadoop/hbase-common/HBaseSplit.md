# 【HBase】HBase 自动拆分和预分区

[TOC]

## 一、Region 自动拆分

HBase 中，表会被划分为1...n 个 Region，被托管在 RegionServer 中。

Region 二个重要的属性：StartKey 与 EndKey 表示这个 Region 维护的 RowKey 范围，当读/写数据时，如果 RowKey 落在某个 start-end key 范围内，那么就会定位到目标region并且读/写到相关的数据。

默认，HBase 在创建表的时候，会自动为表分配一个 Region，正处于混沌时期，start-end key 无边界，所有 RowKey 都往这个 Region里分配。

当数据越来越多，Region 的 size 越来越大时，达到默认的阈值时（根据不同的拆分策略有不同的阈值），HBase 中该 Region 将会进行 split，会找到一个 MidKey 将 Region 一分为二，成为 2 个 Region。而 MidKey 则为这二个 Region 的临界，左为 N 无下界，右为 M 无上界。< MidKey 被分配到 N 区，> MidKey 则会被分配到 M 区。

随着数据量进一步扩大，分裂的两个 Region 达到临界后将重复前面的过程，分裂出更多的 Region。

## 二、Region 自动拆分策略

Region 的分割操作是不可见的，Master 不会参与其中。RegionServer 拆分 Region的步骤是：先将该 Region 下线，然后拆分，将其子 Region 加入到 META 元信息中，再将他们加入到原本的 RegionServer 中，最后汇报 Master。

*执行 split 的线程是 CompactSplitThread。*

在 2.0.5 版本中，HBase 提供了 7 种自动拆分策略：

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g52nbulq2mj20kn074mxt.jpg)

他们之间的继承关系如下：

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g52naxnwe1j20pb08m3ym.jpg)

有三种配置方法：

- 在 hbase-site.xml 中配置，例如：

  ```xml
  <property> 
    <name>hbase.regionserver.region.split.policy</name> 
    <value>org.apache.hadoop.hbase.regionserver.IncreasingToUpperBoundRegionSplitPolicy</value> 
  </property>
  ```

- 在 HBase Configuration中配置：

  ```java
  private static Configuration conf = HBaseConfiguration.create();
  conf.set("hbase.regionserver.region.split.policy", "org.apache.hadoop.hbase.regionserver.SteppingSplitPolicy");
  ```

- 在创建表的时候配置：Region 的拆分策略需要根据表的属性来合理的配置， 所以在建表的时候不建议用前两种方式配置，而是针对不同的表设置不同的策略，每种策略在建表时具体使用在解释每种策略的时候说明。

### 2.1、ConstantSizeRegionSplitPolicy

0.94.0 之前的默认拆分策略，这种策略非常简单，只要 **Region 中的任何一个 StoreFile 的大小达到了`hbase.hregion.max.filesize` 所定义的大小**，就进行拆分。

**1）相关参数：**

**hbase.hregion.max.filesize**

* default: 10737418240 (10GB)
* description: 当一个 Region 的任何一个 StoreFile 容量达到这个配置定义的大小后,就会拆分 Region

**2）部分源码**：

拆分的阈值大小可在创建表的时候设置，如果没有设置，就取`hbase.hregion.max.filesize` 这个配置定义的值，如果这个配置也没有定义，取默认值 10G。

```java
  @Override
  protected void configureForRegion(HRegion region) {
    super.configureForRegion(region);
    Configuration conf = getConf();
    TableDescriptor desc = region.getTableDescriptor();
    if (desc != null) {
      // 如果用户在建表时指定了该表的单个Region的上限, 取用户定义的这个值
      this.desiredMaxFileSize = desc.getMaxFileSize();
    }
    if (this.desiredMaxFileSize <= 0) {
      // 如果用户没有定义, 取'hbase.hregion.max.filesize'这个配置定义的值, 如果这个配置没有定义, 取默认值 10G
      this.desiredMaxFileSize = conf.getLong(HConstants.HREGION_MAX_FILESIZE,
        HConstants.DEFAULT_MAX_FILE_SIZE);
    }
    ...
  }

  // 判断是否进行拆分
  @Override
  protected boolean shouldSplit() {
    boolean force = region.shouldForceSplit();
    boolean foundABigStore = false;

    for (HStore store : region.getStores()) {
      // If any of the stores are unable to split (eg they contain reference files)
      // then don't split
      if ((!store.canSplit())) {
        return false;
      }

      // Mark if any store is big enough
      if (store.getSize() > desiredMaxFileSize) {
        foundABigStore = true;
      }
    }

    return foundABigStore || force;
  }
```

**3）拆分效果：**

经过这种策略的拆分后，Region 的大小是均匀的，例如一个 10G 的Region，拆分为两个 Region 后，这两个新的 Region 的大小是相差不大的，理想状态是每个都是5G。

**ConstantSizeRegionSplitPolicy **切分策略对于大表和小表没有明显的区分，阈值（hbase.hregion.max.filesize）：

* 设置较大对大表比较友好，但是小表就有可能不会触发分裂，极端情况下可能就1个，这对业务来说并不是什么好事；
* 设置较小则对小表友好，但大表就会在整个集群产生大量的 Region，这对于集群的管理、资源使用、failover 来说都不是一件好事。

**4）创建表时配置：**

```java
Connection conn = ConnectionFactory.createConnection(conf);
// 创建一个数据库管理员
Admin admin = conn.getAdmin();
TableName tn = TableName.valueOf(tableName);
// 新建一个表描述，指定拆分策略和最大 StoreFile Size
TableDescriptorBuilder tableBuilder =
    TableDescriptorBuilder.newBuilder(tn)
    .setRegionSplitPolicyClassName(ConstantSizeRegionSplitPolicy.class.getName())
    .setMaxFileSize(1048576000);
// 在表描述里添加列族
for (String columnFamily : columnFamilys) {
    tableBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily)).build());
}
// 根据配置好的表描述建表
admin.createTable(tableBuilder.build());
```

### 2.2、IncreasingToUpperBoundRegionSplitPolicy

该策略继承自 ConstantSizeRegionSplitPolicy，是 0.94.0  到 2.0.0 版本的默认策略，其**优化了原来 ConstantSizeRegionSplitPolicy 只是单一按照 Region 文件大小的拆分策略，增加了对当前表的分片数作为判断因子**。当Region中某个 Store Size  达到 sizeToCheck 阀值时进行拆分，sizeToCheck 计算如下：

```java
protected long getSizeToCheck(final int tableRegionsCount) {
    // safety check for 100 to avoid numerical overflow in extreme cases
    return tableRegionsCount == 0 || tableRegionsCount > 100
               ? getDesiredMaxFileSize()
               : Math.min(getDesiredMaxFileSize(),
                          initialSize * tableRegionsCount * tableRegionsCount * tableRegionsCount);
  }
```

**如果表的分片数为 0 或者大于 100，则切分大小还是以设置的单一 Region 文件大小为标准。如果分片数在 1~99 之间，则由 min(单一 Region 大小， Region 增加策略的初始化大小 *  当前 Table Region 数的3次方) 决定**。

Region 增加策略的初始化大小计算如下：

```java
protected void configureForRegion(HRegion region) {
  super.configureForRegion(region);
  Configuration conf = getConf();
  initialSize = conf.getLong("hbase.increasing.policy.initial.size", -1);
  if (initialSize > 0) {
    return;
  }
  TableDescriptor desc = region.getTableDescriptor();
  if (desc != null) {
    initialSize = 2 * desc.getMemStoreFlushSize();
  }
  if (initialSize <= 0) {
    initialSize = 2 * conf.getLong(HConstants.HREGION_MEMSTORE_FLUSH_SIZE,
                                   TableDescriptorBuilder.DEFAULT_MEMSTORE_FLUSH_SIZE);
  }
}
```

**1）相关参数：**

**hbase.hregion.max.filesize**

- default: 10737418240 (10GB)
- description: 当一个 Region 的任何一个 StoreFile 容量达到这个配置定义的大小后，就会拆分 Region，该策略下并不会一开始就以该值作为拆分阈值

**hbase.increasing.policy.initial.size**

- default: none
- description: IncreasingToUpperBoundRegionSplitPolicy 拆分策略下用于计算 Region 阈值的一个初始值

**hbase.hregion.memstore.flush.size**

* default: 134217728 (128MB)
* description: 如果 Memstore 的大小超过这个字节数，它将被刷新到磁盘

**2）部分源码：**

```java
  @Override
  protected boolean shouldSplit() {
    boolean force = region.shouldForceSplit();
    boolean foundABigStore = false;
    // Get count of regions that have the same common table as this.region
    int tableRegionsCount = getCountOfCommonTableRegions();
    // Get size to check
    long sizeToCheck = getSizeToCheck(tableRegionsCount);

    for (HStore store : region.getStores()) {
      // If any of the stores is unable to split (eg they contain reference files)
      // then don't split
      if (!store.canSplit()) {
        return false;
      }

      // Mark if any store is big enough
      long size = store.getSize();
      if (size > sizeToCheck) {
        LOG.debug("ShouldSplit because " + store.getColumnFamilyName() +
          " size=" + StringUtils.humanSize(size) +
          ", sizeToCheck=" + StringUtils.humanSize(sizeToCheck) +
          ", regionsWithCommonTable=" + tableRegionsCount);
        foundABigStore = true;
      }
    }

    return foundABigStore || force;
  }
```

在默认情况，使用IncreasingToUpperBoundRegionSplitPolicy 策略拆分 Region 的过程是:

* 某张表刚开始只有一个Region， 当 Region 达到 256M 的时候开始拆分 ：

```java
tableRegionsCount = 1
initialSize = 2 * 128M = 256M
getDesiredMaxFileSize() = 10G
sizeToCheck = min(getDesiredMaxFileSize(), initialSize * tableRegionsCount * tableRegionsCount * tableRegionsCount) = min(10G, 256M) = 256M
```

* 拆分后这张表有两个 Region，当 Region 大小达到 2GB 时开始拆分：

```java
tableRegionsCount = 2
initialSize = 2 * 128M = 256M
getDesiredMaxFileSize() = 10G
sizeToCheck = min(getDesiredMaxFileSize(), initialSize * tableRegionsCount * tableRegionsCount * tableRegionsCount) 
= min(10G, 2 * 2 * 2 * 256M) 
= min(10G, 2G)
= 2G
```

- 以此类推，当表有3个 Region 的时候，Region 的最大容量为 6.75G
- 当表有4个Region的时候，计算出来的结果大于10GB，所以使用10GB 作为以后的拆分上限

**3）拆分效果：**

和 ConstantSizeRegionSplitPolicy  一样，也是均匀拆分。

不同的是，**IncreasingToUpperBoundRegionSplitPolicy** 切分策略弥补了ConstantSizeRegionSplitPolicy 的短板，能够自适应大表和小表，并且在大集群条件下对于很多大表来说表现很优秀。

但并不完美，这种策略下很多小表会在大集群中产生大量小 Region，分散在整个集群中。而且在发生 Region 迁移时也可能会触发 Region 分裂。

**4）创建表时配置：**

```java
Connection conn = ConnectionFactory.createConnection(conf);
// 创建一个数据库管理员
Admin admin = conn.getAdmin();
TableName tn = TableName.valueOf(tableName);
// 新建一个表描述，指定拆分策略和最大 StoreFile Size
TableDescriptorBuilder tableBuilder =
    TableDescriptorBuilder.newBuilder(tn)
   .setRegionSplitPolicyClassName(IncreasingToUpperBoundRegionSplitPolicy.class.getName())
    .setMaxFileSize(1048576000)
    .setValue("hbase.increasing.policy.initial.size", "134217728");
// 在表描述里添加列族
for (String columnFamily : columnFamilys) {
    tableBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily)).build());
}
// 根据配置好的表描述建表
admin.createTable(tableBuilder.build());
```

### 2.3、SteppingSplitPolicy

2.0 版本默认切分策略。SteppingSplitPolicy 是IncreasingToUpperBoundRegionSplitPolicy 的子类，其对 Region 拆分文件大小做了优化，如果只有1个 Region 的情况下，那第1次的拆分就是 256M，后续则按配置的拆分文件大小（10G）做为拆分标准。

**1）相关参数：**

同 IncreasingToUpperBoundRegionSplitPolicy 。

**2）全部源码：**

```java
/**
 * @return flushSize * 2 if there's exactly one region of the table in question
 * found on this regionserver. Otherwise max file size.
 * This allows a table to spread quickly across servers, while avoiding creating
 * too many regions.
*/
@Override
protected long getSizeToCheck(final int tableRegionsCount) {
  return tableRegionsCount == 1  ? this.initialSize : getDesiredMaxFileSize();
}
```

它的源码只有一个方法，优化了 getSizeToCheck 方法，其他都是继承 自IncreasingToUpperBoundRegionSplitPolicy 类。

**3）拆分效果：**

在 IncreasingToUpperBoundRegionSplitPolicy 策略中，针对大表的拆分表现很不错，但是针对小表会产生过多的 Region，SteppingSplitPolicy 则将小表的 Region 控制在一个合理的范围，对大表的拆分也不影响。

**4）创建表时配置：**

```java
Connection conn = ConnectionFactory.createConnection(conf);
// 创建一个数据库管理员
Admin admin = conn.getAdmin();
TableName tn = TableName.valueOf(tableName);
// 新建一个表描述，指定拆分策略和最大 StoreFile Size
TableDescriptorBuilder tableBuilder =
    TableDescriptorBuilder.newBuilder(tn)
   .setRegionSplitPolicyClassName(SteppingSplitPolicy.class.getName())
    .setMaxFileSize(1048576000)
    .setValue("hbase.increasing.policy.initial.size", "134217728");
// 在表描述里添加列族
for (String columnFamily : columnFamilys) {
    tableBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily)).build());
}
// 根据配置好的表描述建表
admin.createTable(tableBuilder.build());
```

### 2.4、KeyPrefixRegionSplitPolicy

KeyPrefixRegionSplitPolicy 是 IncreasingToUpperBoundRegionSplitPolicy 的子类，该策略除了具备其父类自动调整 Region 拆分阈值大小、适应大小表的特点外，增加了对拆分点(splitPoint，拆分点就是 Region 被拆分处的 RowKey)的定义，可以保证有相同前缀的 RowKey不会被拆分到两个不同的 Region 里面。

**1）相关参数：**

在 IncreasingToUpperBoundRegionSplitPolicy 的配置之上增加了一个参数。

**KeyPrefixRegionSplitPolicy.prefix_length**

* default: 0，会尝试去使用已经废弃的参数 prefix_split_key_policy.prefix_length 值
* description: 指定了在 RowKey 中，取前几个字符作为前缀，例如这个设置这个值为5，那么在 RowKey中，如果前5个字符是相同的，拆分后也一定会在一个Region中

**2）部分源码：**

```java
protected byte[] getSplitPoint() {
  byte[] splitPoint = super.getSplitPoint();
  if (prefixLength > 0 && splitPoint != null && splitPoint.length > 0) {
    // group split keys by a prefix
    return Arrays.copyOf(splitPoint,
        Math.min(prefixLength, splitPoint.length));
  } else {
    return splitPoint;
  }
}
```

先从父类获取拆分点，如果设置了 prefixLength > 0，就从父类拆分点中截取需要的前缀作为新的拆分点返回。

**3）拆分效果：**

* 普通拆分

  ![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g54031u9bsj20l409vmx7.jpg)

- 按照Rowkey前缀拆分

  ![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g54037vfe3j20l409vglm.jpg)

*KeyPrefixRegionSplitPolicy （SteppingSplitPolicy、DelimitedKeyPrefixRegionSplitPolicy、BusyRegionSplitPolicy (HBase-2.x Only)）按照 RowKey 的前缀去拆分 Region，但是什么时候拆分，原 Region 容量的最大值是多少还是需要使用 IncreasingToUpperBoundRegionSplitPolicy 的方法去计算*。

如果所有数据都只有一两个前缀，那么采用默认的策略较好。  如果前缀划分的比较细，查询就比较容易发生跨 Region 查询的情况，此时采用KeyPrefixRegionSplitPolicy 较好。 

所以这个策略适用的场景是：

- 数据有多种前缀
- 查询多是针对前缀，较少跨越多个前缀来查询数据

**4）创建表时配置：**

```java
Connection conn = ConnectionFactory.createConnection(conf);
// 创建一个数据库管理员
Admin admin = conn.getAdmin();
TableName tn = TableName.valueOf(tableName);
// 新建一个表描述，指定拆分策略和最大 StoreFile Size
TableDescriptorBuilder tableBuilder =
    TableDescriptorBuilder.newBuilder(tn)
   .setRegionSplitPolicyClassName(KeyPrefixRegionSplitPolicy.class.getName())
    .setMaxFileSize(1048576000)
    .setMemStoreFlushSize(134217728)
    .setValue("KeyPrefixRegionSplitPolicy.prefix_length", "5")
    .setValue("hbase.increasing.policy.initial.size", "134217728");
// 在表描述里添加列族
for (String columnFamily : columnFamilys) {
    tableBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily)).build());
}
// 根据配置好的表描述建表
admin.createTable(tableBuilder.build());
```

### 2.5、DelimitedKeyPrefixRegionSplitPolicy

继承自 IncreasingToUpperBoundRegionSplitPolicy，也是根据 RowKey 前缀来进行拆分的。不同就是：KeyPrefixRegionSplitPolicy 是根据 RowKey 的固定前几位字符来进行判断，而 DelimitedKeyPrefixRegionSplitPolicy 是根据分隔符来判断的。

**1）相关参数：**

在 IncreasingToUpperBoundRegionSplitPolicy 的配置之上增加了一个参数。

**DelimitedKeyPrefixRegionSplitPolicy.delimiter**

- default: none
- description: 使用该参数定义的分隔符分隔 RowKey ，分隔后的前部分相同的 RowKey 拆分后一定会在一个 Region 中

**2）部分源码：**

```java
protected byte[] getSplitPoint() {
  byte[] splitPoint = super.getSplitPoint();
  if (splitPoint != null && delimiter != null) {

    //find the first occurrence of delimiter in split point
    int index =
        org.apache.hbase.thirdparty.com.google.common.primitives.Bytes.indexOf(splitPoint, delimiter);
    if (index < 0) {
      LOG.warn("Delimiter " + Bytes.toString(delimiter) + "  not found for split key "
            + Bytes.toString(splitPoint));
      return splitPoint;
    }

    // group split keys by a prefix
    return Arrays.copyOf(splitPoint, Math.min(index, splitPoint.length));
  } else {
    return splitPoint;
  }
}
```

先找到分隔符下标位置，然后从父类的拆分点截取出来。

**3）拆分效果：**

DelimitedKeyPrefixRegionSplitPolicy  根据 RowKey 中指定分隔字符做为拆分，显得更加灵活，如 RowKey 的值为“userid_eventtype_eventid”，userId 不是定长的，则 DelimitedKeyPrefixRegionSplitPolicy 可以取 RowKey 值中从左往右且第一个分隔字符串之前的字符做为拆分串，在该示例中就是“userid”。

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g54105vtp6j20nq09vq2x.jpg)

**4）创建表时配置：**

```java
Connection conn = ConnectionFactory.createConnection(conf);
// 创建一个数据库管理员
Admin admin = conn.getAdmin();
TableName tn = TableName.valueOf(tableName);
// 新建一个表描述，指定拆分策略和最大 StoreFile Size
TableDescriptorBuilder tableBuilder =
    TableDescriptorBuilder.newBuilder(tn)
   .setRegionSplitPolicyClassName(DelimitedKeyPrefixRegionSplitPolicy.class.getName())
    .setMaxFileSize(1048576000)
    .setMemStoreFlushSize(134217728)
    .setValue("DelimitedKeyPrefixRegionSplitPolicy.delimiter", "_")
    .setValue("hbase.increasing.policy.initial.size", "134217728");
// 在表描述里添加列族
for (String columnFamily : columnFamilys) {
    tableBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily)).build());
}
// 根据配置好的表描述建表
admin.createTable(tableBuilder.build());
```

### 2.6、BusyRegionSplitPolicy

之前的策略都未考虑 Region 热点问题，考虑某些 Region 可能被频繁访问，负荷很大，BusyRegionSplitPolicy 策略同样继承自 IncreasingToUpperBoundRegionSplitPolicy，但主要针对 Region 问题，是在 2.x 中新增加的拆分策略。

**1）相关参数：**

在 IncreasingToUpperBoundRegionSplitPolicy 的配置之上增加了如下参数：

**hbase.busy.policy.blockedRequests**

* default: 0.2f
* description: 请求阻塞率，即请求被阻塞的严重程度。取值范围是[0.0, 1.0]，默认是0.2，即20%的请求被阻塞的意思。

**hbase.busy.policy.minAge**

* default: 600000 (10min)
* description: 拆分最小年龄。当 Region 的年龄比这个小的时候不拆分，这是为了防止在判断是否要拆分的时候出现了短时间的访问频率波峰，结果没必要拆分的 Region 被拆分了，因为短时间的波峰会很快地降回到正常水平。单位毫秒，默认值是 600000，即10分钟。

**hbase.busy.policy.aggWindow**

* default: 300000 (5min)
* description: 计算是否繁忙的时间窗口，单位毫秒，默认值是300000，即5分钟。用以控制计算的频率。

**2）部分源码：**

```java
  @Override
  protected boolean shouldSplit() {
    float blockedReqRate = updateRate();
    if (super.shouldSplit()) {
      return true;
    }

    if (EnvironmentEdgeManager.currentTime() <  startTime + minAge) {
      return false;
    }

    for (HStore store: region.getStores()) {
      if (!store.canSplit()) {
        return false;
      }
    }

    if (blockedReqRate >= maxBlockedRequests) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Going to split region " + region.getRegionInfo().getRegionNameAsString()
            + " because it's too busy. Blocked Request rate: " + blockedReqRate);
      }
      return true;
    }

    return false;
  }
```

在判断是否需要进行拆分的时候，先调用父类的 shouldSplit 方法检验，如果需要则直接返回 true，否则需要判断当前时间是否比开始时间大于 minAge 值，如果是的，则计算请求阻塞率 blockedReqRate，如果阻塞率大于设定的阈值，则进行拆分。

阻塞率的计算如下：

```java
  /**
   * Update the blocked request rate based on number of blocked and total write requests in the
   * last aggregation window, or since last call to this method, whichever is farthest in time.
   * Uses weighted rate calculation based on the previous rate and new data.
   *
   * @return Updated blocked request rate.
   */
  private synchronized float updateRate() {
    float aggBlockedRate;
    long curTime = EnvironmentEdgeManager.currentTime();

    long newBlockedReqs = region.getBlockedRequestsCount();
    long newWriteReqs = region.getWriteRequestsCount();

    aggBlockedRate =
        (newBlockedReqs - blockedRequestCount) / (newWriteReqs - writeRequestCount + 0.00001f);

    if (curTime - prevTime >= aggregationWindow) {
      blockedRate = aggBlockedRate;
      prevTime = curTime;
      blockedRequestCount = newBlockedReqs;
      writeRequestCount = newWriteReqs;
    } else if (curTime - startTime >= aggregationWindow) {
      // Calculate the aggregate blocked rate as the weighted sum of
      // previous window's average blocked rate and blocked rate in this window so far.
      float timeSlice = (curTime - prevTime) / (aggregationWindow + 0.0f);
      aggBlockedRate = (1 - timeSlice) * blockedRate + timeSlice * aggBlockedRate;
    } else {
      aggBlockedRate = 0.0f;
    }
    return aggBlockedRate;
  }
```

主要的计算逻辑是：请求的被阻塞率(aggBlockedRate) = curTime - prevTime 时间内新增的阻塞请求 / 这段时间的总请求。

**3）拆分效果：**

如果系统常常会出现热点 Region，又对性能有很高的追求，那么这种策略可能会比较适合。

它会通过拆分热点 Region 来缓解热点 Region 的压力，但是根据热点来拆分Region 也会带来很多不确定性因素，因为不能确定下一个被拆分的 Region 是哪个。

**4）创建表时配置：**

```java
Connection conn = ConnectionFactory.createConnection(conf);
// 创建一个数据库管理员
Admin admin = conn.getAdmin();
TableName tn = TableName.valueOf(tableName);
// 新建一个表描述，指定拆分策略和最大 StoreFile Size
TableDescriptorBuilder tableBuilder =
    TableDescriptorBuilder.newBuilder(tn)
   .setRegionSplitPolicyClassName(BusyRegionSplitPolicy.class.getName())
    .setMaxFileSize(1048576000)
    .setMemStoreFlushSize(134217728)
    .setValue("hbase.busy.policy.blockedRequests", "0.2")
    .setValue("hbase.busy.policy.aggWindow", "300000")
    .setValue("hbase.busy.policy.minAge", "600000");
// 在表描述里添加列族
for (String columnFamily : columnFamilys) {
    tableBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily)).build());
}
// 根据配置好的表描述建表
admin.createTable(tableBuilder.build());
```

### 2.7、DisabledRegionSplitPolicy

DisabledRegionSplitPolicy 就是不使用 Region 拆分策略，将所有的数据都写到同一个 Region 中。

**1）全部源码：**

```java
public class DisabledRegionSplitPolicy extends RegionSplitPolicy {
  @Override
  protected boolean shouldSplit() {
    return false;
  }
}
```

源码很简单，就是直接返回 false。

**2）拆分效果：**

这个策略极少使用。

即使在建表的时候合理的进行了预拆分，还没有写入的数据的时候就已经手动分好了 Region，但是随着数据的持续写入，我预先分好的 Region 的大小也会达到阈值，那时候还是要依靠 HBase 的自动拆分策略去拆分 Region。

但这种策略也有它的用途：

假如有一批静态数据，一次存入以后不会再加入新数据，且这批数据主要是用于查询，为了性能好一些，可以先进行预分区后，各个 Region 数据量相差不多，然后设置拆分策略为禁止拆分，最后导入数据即可。

**3）创建表时配置：**

```java
Connection conn = ConnectionFactory.createConnection(conf);
// 创建一个数据库管理员
Admin admin = conn.getAdmin();
TableName tn = TableName.valueOf(tableName);
// 新建一个表描述，指定拆分策略和最大 StoreFile Size
TableDescriptorBuilder tableBuilder =
    TableDescriptorBuilder.newBuilder(tn)
   .setRegionSplitPolicyClassName(DisabledRegionSplitPolicy.class.getName());
// 在表描述里添加列族
for (String columnFamily : columnFamilys) {
    tableBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily)).build());
}
// 根据配置好的表描述建表
admin.createTable(tableBuilder.build());
```

## 三、Region 预分区

### 3.1、预分区原因

已经有自动分区了，为什么还需要预分区？

HBase 在创建表的时候，会自动为表分配一个Region，当一个 Region 达到拆分条件时（shouldSplit 为 true），HBase 中该 Region 将会进行 split，分裂为2个 Region，以此类推。表在进行 split 的时候，会耗费很多的资源，有大量的 io 操作，频繁的分区对  HBase 的性能有很大的影响。

所以，HBase 提供了预分区功能，让用户可以在创建表的时候对表按照一定的规则分区。

假设初始 10 个 Region，那么导入大量数据的时候，就会均衡到 10 个 Region 里面，显然比初始 1 个 Region 要好很多，**合理的预分区可以减少 Region 热点问题，提升写数据的性能和速度，而且也能减少后续的 split 操作**。

### 3.2、预分区方法

首先要明白数据的 RowKey 是如何分布的，然后根据 RowKey 的特点规划要分成多少 Region，每个 Region 的 startKey 和 endKey 是多少，接着就可以预分区了。

比如，RowKey 的前几位字符串都是从 0001~0010 的数字，这样可以分成10个Region：

```javascript
0001|  
0002|  
0003|  
0004|  
0005|  
0006|  
0007|  
0008|  
0009|
```

第一行为第一个 Region 的 stopKey。为什么后面会跟着一个"|"，是因为在ASCII码中，"|"的值是124，大于所有的数字和字母等符号。

**shell中建分区表**

```shell
create 'test', {NAME => 'cf', VERSIONS => 3, BLOCKCACHE => false}, SPLITS => ['10','20','30']
```

也可以通过指定 SPLITS_FILE 的值指定分区文件，从文件中读取分区值，文件格式如上述例子所示：

```shell
create 'test', {NAME =>'cf', COMPRESSION => 'SNAPPY'}, {SPLITS_FILE =>'region_split_info.txt'}
```

预分区后，可以从 HBase ui 页面观察到：

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g544nti6ljj20xt09l0tf.jpg)

**HBase API 建预分区表**

```java
byte[][] splitKeys = {
    Bytes.toBytes("10"),
    Bytes.toBytes("20"),
    Bytes.toBytes("30")
};

// 创建数据库表
public static void createTable(String tableName, byte[][] splitKeys, String... columnFamilys) throws IOException {
    // 建立一个数据库的连接
    Connection conn = getCon();
    // 创建一个数据库管理员
    Admin admin = conn.getAdmin();
    TableName tn = TableName.valueOf(tableName);
    TableDescriptorBuilder tableBuilder =
                TableDescriptorBuilder.newBuilder(tn)
                        .setRegionSplitPolicyClassName(IncreasingToUpperBoundRegionSplitPolicy.class.getName())
                        .setValue("hbase.increasing.policy.initial.size", "134217728");
        // 在表描述里添加列族
        for (String columnFamily : columnFamilys) {
            tableBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily)).build());
        }
        // 根据配置好的表描述建表
        admin.createTable(tableBuilder.build(), splitKeys);
    }
    conn.close();
}
```

### 3.3、随机散列与预分区

为防止热点问题，同时避免 Region Split 后，部分 Region 不再写数据或者很少写数据。也为了得到更好的并行性，希望有好的 load blance，让每个节点提供的请求处理都是均等的，并且 Region 不要经常 split，因为 split 会使 server 有一段时间的停顿，随机散列加上预分区是比较好的解决方式。

预分区一开始就预建好了一部分 Region，这些 Region 都维护着自已的 start-end keys，再配合上随机散列，写数据能均等地命中这些预建的 Region，就能通过良好的负载，提升并行，大大地提高了性能。

**hash + 预分区**

在 RowKey 的前面拼接通过 hash 生成的随机字符串，可以生成范围比较随机的 RowKey，可以比较均衡分散到不同的 Region 中，那么就可以解决写热点问题。

假设 RowKey 原本是自增长的 long 型，可以将 RowKey 先进行 hash，加上本身 id  ，组成rowkey，这样就生成比较随机的 RowKey 。

```java
long currentId = 1L;
byte [] rowkey = Bytes.add(MD5Hash.getMD5AsHex(Bytes.toBytes(currentId)).substring(0, 8).getBytes(),
Bytes.toBytes(currentId));
```

那么对于这种方式的 RowKey 设计，如何去进行预分区？

1. 先取样，先随机生成一定数量的 RowKey ，将取样数据按升序排序放到一个集合里；
2. 根据预分区的 Region 个数，对整个集合平均分割，得到相关的 splitKeys；
3. 再创建表时将步骤2得到的 splitKeys 传入即可。

**partition + 预分区**

partition 顾名思义就是分区式，这种分区有点类似于 mapreduce 中的 partitioner，将区域用长整数作为分区号，每个 Region 管理着相应的区域数据，在 RowKey 生成时，将 id 取模后，然后拼上 id 整体作为 RowKey 。

```java
long currentId = 1L;
long partitionId = currentId % partition;
byte[] rowkey = Bytes.add(Bytes.toBytes(partitionId),
                    Bytes.toBytes(currentId));
```

## 四、参考博文

1.[HBase Region 自动拆分策略](https://cloud.tencent.com/developer/article/1374592)
2.[hbase预分区](https://my.oschina.net/u/2000675/blog/1083450)