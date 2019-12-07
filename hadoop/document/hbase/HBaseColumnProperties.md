# 【HBase】HBase 列族属性配置

[TOC]

## 一、创建表及属性查看

创建一个测试表 test，列族为 cf：

```shell
create 'test', {NAME => 'cf'}
```

默认属性如下：

```shell
hbase > describe 'test'

{
NAME => 'cf', 
BLOOMFILTER => 'ROW', 
VERSIONS => '1', 
IN_MEMORY => 'false', 
KEEP_DELETED_CELLS => 'FALSE', 
DATA_BLOCK_ENCODING => 'NONE', 
TTL => 'FOREVER', 
COMPRESSION => 'GZ', 
MIN_VERSIONS => '0', 
BLOCKCACHE => 'true',
BLOCKSIZE => '65536', 
REPLICATION_SCOPE => '0'
}   
```

## 二、列族属性配置

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g50r5da3kaj20nm0jtmyq.jpg)

### 2.1、版本数量（VERSIONS）

HBase 一切操作均为更新，Hbase Put 操作不会去覆盖一个值，只会在后面追加写，用时间戳（版本号）来区分，HBase 版本维度**按递减顺序存储**，以便在从存储文件读取时，首先找到最近的值；Hbase Delete 操作也不是真正删除了记录，而是放置了一个墓碑标记，过早的版本会在执行  Major Compaction 时真正删除。

0.96版本默认是3个， 0.98版本之后是1， 要根据业务来划分，版本是历史记录，版本增多意味空间消耗。

插入数据的时候，版本默认是当前时间；查询的时候可以指定要获取的版本个数 `get 'test', { COLUMN => 'cf', VERSIONS => 2}`；

获取多个版本的时候，**多个数据是按照时间戳倒序排序**，也可以通过这个特性，来保存类似于事件发生的数据，查询时间历史的时候，拿出来的数据是按照时间排好序，如果要拿最新的事件，不指定版本即可。

版本的时间戳，也可以自定义，不使用默认生成的时间戳，可以自己指定业务相关的ID。

**使用方法:** 

```shell
create 'test1', {NAME => 'cf', VERSIONS => 3}
```

**更改版本号：**

```shell
alter 'test1', NAME => 'cf', VERSIONS => 5
```

### 2.2、存活时间（TTL）

TTL 全称是 Time To Live，ColumnFamilies 可以设置 TTL(单位是s)，HBase 会自动检查 TTL 值是否达到上限，如果 TTL 达到上限后自动删除行。当然真正删除是在**Major Compaction过程**中执行的。

试验一下：

```shell
alter 'test1', NAME => 'cf', VERSIONS => 5, TTL => 10

put 'test1', '0001', 'cf:name', 'james'

get 'test1', '0001'
```

发现过 10s 后数据已经删除。

### 2.3、最小版本数（MIN_VERSIONS ）

如果 **HBase 中的表设置了 TTL 的时候**，MIN_VERSIONS 才会起作用。

每个列族可以设置最小版本数，最小版本数缺省值是0，表示禁用该特性。最小版本数参数和存活时间是一起使用的，允许配置“**如保存最后T秒有价值的数据，最多N个版本，但最少M个版本**”（M是最小版本，M<N）。

* MIN_VERSION > 0时：Cell 至少有 MIN_VERSION 个最新版本会保留下来；
* MIN_VERSION = 0时：Cell 中的数据超过TTL时间时，全部清空，不保留最低版本。

### 2.4、保留删除的单元格（KEEP_DELETED_CELLS）

HBase 的`delete` 命令，并不是真的删除数据，而是设置一个标记（`delete marker`）。用户在检索数据的时候，会过滤掉这些标示的数据。
该属性可以设置为 `FALSE`（默认）、`TRUE`、`TTL`。

可以从 java API 源码中看到：

```java
public enum KeepDeletedCells {
  /** Deleted Cells are not retained. */
  FALSE,
  /**
   * Deleted Cells are retained until they are removed by other means
   * such TTL or VERSIONS.
   * If no TTL is specified or no new versions of delete cells are
   * written, they are retained forever.
   */
  TRUE,
  /**
   * Deleted Cells are retained until the delete marker expires due to TTL.
   * This is useful when TTL is combined with MIN_VERSIONS and one
   * wants to keep a minimum number of versions around but at the same
   * time remove deleted cells after the TTL.
   */
  TTL;
  public static KeepDeletedCells getValue(String val) {
    return valueOf(val.toUpperCase());
  }
}
```

* FALSE：不保留删除的单元格。
* TRUE：删除的单元格会保留，超期（TTL）或者数据版本数超过 VERSIONS 设置的值才会被删除；如果没有指定 TTL 或没有超出VERSIONS 值，则会永久保留它们。
* TTL：超期（TTL）才会删除，当 TTL 与 MIN_VERSIONS 结合使用时，会删除过期后的数据，但是同时会保留最少数量的版本。

### 2.5、数据块大小（BLOCKSIZE ）

HBase 默认的块大小是 64kb，不同于 HDFS 默认 64MB 的块大小。

数据块索引存储每个 HFile 数据块的起始键，数据块大小配置会影响数据块索引的大小。数据块越小，数据块索引越大，因而占用的内存空间越大。

同时，加载进内存的数据块更小，随机查找性能更好，因为一旦找到了行键所在的块，接下来就会定位对应的单元格，使用更小的数据块效率更优。

但是如果需要更好的顺序扫描性能，那么一次能够加载更多 HFile 数据进入内存则更为合理，这意味数据块大小应该设置更大的值。相应索引将变小，将在随机读性能上付出代价。

**对于不同的业务数据，块大小的合理设置对读写性能有很大的影响。如果业务请求以 Get 请求为主，可以考虑将块大小设置较小；如果以 Scan 请求为主，可以将块大小调大；默认的 64K 块大小是在 Scan 和 Get 之间取得的一个平衡。**

默认块大小适用于多种数据使用模式，调整块大小是比较高级的操作。配置错误将对性能产生负面影响。因此建议在调整之后进行测试，根据测试结果决定是否可以线上使用。

### 2.6、块缓存（BLOCKCACHE）

默认是 true。缓存是内存存储，HBase 使用块缓存将最近使用的块加载到内存中。块缓存会根据**最近最久未使用（LRU）**的规则删除数据块。

如果使用场景是经常顺序访问 Scan 或者很少被访问，可以关闭列族的缓存。列族缓存默认是打开的。

### 2.7、激进缓存（IN_MEMORY）

HBase 可以选择一个列族赋予更高的优先级缓存，激进缓存（表示优先级更高），`IN_MEMORY` 默认是false。

如果设置为true，HBase 会尝试将整个列族保存在内存中，只有在需要保存是才会持久化写入磁盘。但是在运行时 HBase 会尝试将整张表加载到内存里。

这个参数通常适合较小的列族。

### 2.8、压缩（COMPRESSION）

数据压缩是 HBase 提供的一个特性，HBase 在写入数据块到 HDFS 之前会首**先对数据块进行压缩，再落盘**，从而可以减少磁盘空间使用量。

而在读数据的时候首先从 HDFS 中加载出 block 块之后**进行解压缩，然后再缓存到BlockCache**，最后返回给用户。

写路径和读路径分别如下：

* **写路径**： *Finish DataBlock* –> *Encoding KVs* –> **Compress DataBlock** –> *Flush*
* **读路径**： *Read Block From Disk* –> **DeCompress DataBlock** –> *Cache DataBlock* –> *Decoding Scan KVs*

压缩可以**节省空间**，但读写数据会**增加CPU负载**，默认为 **NONE**，不使用用压缩，HBase 目前提供了三种常用的压缩方式： **GZip, LZO, Snappy**：

1. GZIP 的压缩率最高，但是 CPU 密集型的，对 CPU 的消耗比其他算法要多，压缩和解压速度也慢；
2. LZO 的压缩率居中，比 GZIP 要低一些，但是压缩和解压速度明显要比GZIP 快很多，其中解压速度快的更多；
3. Snappy 的压缩率最低，而压缩和解压速度要稍微比 LZO 要快一些。

综合来看，**Snappy** 的压缩率最低，但是编解码速率最高，对 CPU 的消耗也最小，目前一般建议使用 **Snappy**。

### 2.9、布隆过滤器（BLOOMFILTER）

布隆过滤器用自己的算法，实现了**快速的检索一个元素是否在一个较大的元素列表之中**。

它的基本思想是：当一个元素被加入集合时，通过 K 个散列函数将这个元素映射成一个位数组中的 K 个点，把它们置为1；检索时，只要看看这些点是不是都是1 就（大约）知道集合中有没有它了——**如果这些点有任何一个0，则被检元素一定不在，如果都是1，则被检元素很可能在**。

*它的优点是空间效率和查询时间都远远超过一般的算法，缺点是有一定的误识别率和删除困难使用了Hash算法，必然会存在极限巧合下的 hash 碰撞，会将不存在的数据认为是存在的。但是存在的数据一定是可以正确判断的。*

HBase 中的 BloomFilter 主要用来过滤不存在待检索 RowKey 或者 Row-Col 的 HFile 文件，避免无用的 IO 操作。它可以判断 HFile 文件中是否可能存在待检索的KV，如果不存在，就可以不用消耗 IO 打开文件进行 seek。**通过设置 BloomFilter 可以提升随机读写的性能。**

BloomFilter 是一个列族级别的配置属性，如果在表中设置了BloomFilter，那么HBase 会在生成 StoreFile 时包含一份 BloomFilter 结构的数据，称其为`MetaBlock` ，和 `DataBlock` (真实KeyValue数据)一起由 LRUBlockCache 维护。所以开启 BloomFilter 会有一定的存储即内存 Cache 的开销。

BloomFilter 取值有两个，`row` 和`rowcol`，需要根据业务来确定具体使用哪种。

- 如果业务大多数随机查询时仅仅使用 row 作为查询条件，BloomFilter 设置为row；
- 如果大多数随机查询使用 row+cf 作为查询条件，BloomFilter 需要设置为rowcol；
- 如果不确定查询类型，建议设置为 row。

### 2.10、数据块编码（DATA_BLOCK_ENCODING）

除了数据压缩之外，HBase 还提供了数据编码功能。

和压缩一样，数据在落盘之前首先会对 KV 数据进行编码；但又**和压缩不同，数据块在缓存前并没有执行解码**。因此即使后续**命中缓存的查询是编码的数据块，需要解码后才能获取到具体的 KV 数据**。

和不编码情况相比，编码后相同数据 block 块占用内存更少，即**内存利用率更高**，但是读取的时候需要解码，又不利于读性能，在内存不足的情况下，可以压榨  CPU 字段，换区更多的缓存数据。

HBase目前提供了四种常用的编码方式： **Prefix_Tree**、 Diff 、 Fast_Diff 、Prefix。

写路径和读路径分别如下：

* **写路径**： *Finish DataBlock* –> **Encoding KVs** –> *Compress DataBlock* –> *Flush*
* **读路径**： *Read Block From Disk* –> *DeCompress DataBlock* –> *Cache DataBlock* –> **Decoding Scan KVs**

### 2.11、复制范围（REPLICATION_SCOPE ）

HBase 提供了跨级群同步的功能，本地集群的数据更新可以及时同步到其他集群。复制范围（replication scope）的参数默认为0，表示复制功能处于关闭状态。

## 三、列族设置

### 3.1、列族数量

**不要在一张表中定义太多的列族。**

目前 HBase 并不能很好的处理 2~3 以上的列族，`flush`和`compaction` 操作是针对一个 Region 的。

当一个列族操作大量数据的时候会引发一个 flush，它邻近的列族也会因关联效应被触发 flush，尽管它没有操作多少数据。compaction 操作是根据一个列族下的全部文件的数量触发的，而不是根据文件大小触发的。

当很多的列族在 flush 和 compaction 时，会造成很多没用的 IO 负载。

尽量在模式中只针对一个列族进行操作。将使用率相近的列归为一个列族，这样每次访问就只用访问一个列族，既能提升查询效率，也能保持尽可能少的访问不同的磁盘文件。

### 3.2、列族的基数

如果一个表存在多个列族，要注意列族之间基数（如行数）相差不要太大。例如列族 A 有100 万行，列族 B 有 10 亿行，按照 RowKey 切分后，列族A可能被分散到很多很多 Region（及RegionServer），这导致扫描列族A十分低效。

### 3.3、列族名、列名长度

列族名和列名越短越好，冗长的名字虽然可读性好，但是更短的名字在 HBase 中更好。

一个具体的值由存储该值的行键、对应的列（`列族:列`）以及该值的时间戳决定。HBase 中索引是为了加速随机访问的速度，索引的创建是基于“`行键+列族:列+时间戳+值`”的，如果行键和列族的大小过大，甚至超过值本身的大小，那么将会增加索引的大小。并且在HBase中数据记录往往非常之多，重复的行键、列将不但使索引的大小过大，也将加重系统的负担。

## 四、总结

根据 HBase 列族属性配置，结合使用场景，HBase 列族可以进行如下优化：

1. 列族不宜过多，将相关性很强的 key-value 都放在同一个列族下；
2. 尽量最小化行键和列族的大小；
3. 提前预估数据量，再根据 RowKey 规则，提前规划好 Region 分区，在创建表的时候进行预分区；
4. 在业务上没有特别要求的情况下，只使用一个版本，即最大版本和最小版本一样，均为1；
5. 根据业务需求合理设置好失效时间（存储的时间越短越好）；
6. 根据查询条件，设置合理的BloomFilter配置。