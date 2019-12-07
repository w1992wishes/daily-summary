# 【HBase】HBase 行健设计

[TOC]

## 一、RowKey 的作用

HBase 由于其存储和读写的高性能，在 OLAP 即时分析中越来越发挥重要的作用。作为 Nosql 数据库的一员，HBase 查询只能通过其 Rowkey 来查询(Rowkey用来表示唯一一行记录)，Rowkey 设计的优劣直接影响读写性能。

### 1.1、RowKey 在查询中的作用

HBase 是根据 RowKey 进行检索，系统通过找到某个 RowKey (或者某个 RowKey 范围)所在的 Region，然后将查询数据的请求路由到该 Region 获取数据。

HBase 的检索支持3种方式：

* 通过单个 RowKey 访问，即按照某个 RowKey 键值进行 get 操作，这样获取唯一一条记录；
* 通过 RowKey 的 range 进行 scan，即通过设置 startRowKey 和 endRowKey，在这个范围内进行扫描，这样可以按指定的条件获取一批记录；
*  全表扫描，即直接扫描整张表中所有行记录。

HBase 中的数据是按照 Rowkey 的 ASCII 字典顺序进行全局排序的，举例说明：假如有 5 个 Rowkey："012", "0", "123", "234", "3"，按 ASCII 字典排序后的结为："0", "012", "123", "234", "3"。

Rowkey 排序时会先比对两个 Rowkey 的第一个字节，如果相同，然后会比对第二个字节，依次类推......对比到第X个字节时，已经超出了其中一个 Rowkey 的长度，短的 Rowkey 排在前面。

HBase 按单个 RowKey 检索的效率是很高的，耗时在1毫秒以下，每秒钟可获取1000~2000条记录，不过非 key 列的查询很慢。因此在设计 HBase 表时，Rowkey  是最重要的，**应该基于预期的访问模式来为 Rowkey 建模，一般 Rowkey 上都会存一些比较关键的检索信息**，据查询方式进行数据存储格式的设计，避免做全表扫描，因为效率特别低。**并且在设计 RowKey 的时候选择好字段之后，还应该结合我们的实际的高频的查询场景来组合选择的字段，越高频的查询字段排列越靠左。**

### 1.2、RowKey 在 Region 中的作用

在 HBase 中，Region 相当于一个数据的分片，每个 Region 都有 StartRowKey  和 StopRowKey ，这是表示 Region 存储的 RowKey 的范围，HBase 表的数据时按照 RowKey 来分散到不同的 Region，要想将数据记录均衡的分散到不同的Region中去，因此需要 RowKey 满足这种散列的特点。此外，在数据读写过程中也是与RowKey 密切相关，RowKey 在读写过程中的作用：

* 读写数据时通过 RowKey 找到对应的 Region；
* MemStore 中的数据是按照 RowKey 的字典序排序；
* HFile 中的数据是按照 RowKey 的字典序排序。

## 二、RowKey 应该具备的特性

### 2.1、字符串类型

虽然行键在 HBase 中是以 byte[] 字节数组的形式存储的，但是建议在系统开发过程中将其数据类型设置为 String 类型，**保证通用性**。

如果在开发过程中将 RowKey 规定为其他类型，譬如 Long 型，那么数据的长度将可能受限于编译环境等所规定的数据长度。

### 2.2、有明确意义

RowKey 的主要作用是为了进行数据记录的唯一性标示，但是唯一性并不是其全部，具有明确意义的行键对于应用开发、数据检索等都具有特殊意义。譬如数字字符串 9559820140512，其实际意义是这样：95598（电网客服电话）+ 20140512（日期）。

行键往往由多个值组合而成，而各个值的位置顺序将影响到数据存储和检索效率，所以在设计行键时，需要对日后的业务应用开发有比较深入的了解和前瞻性预测，才能设计出可尽量高效率检索的行键。

### 2.3、具有有序性

RowKey 是按照字典序存储，因此，设计 RowKey 时，要充分利用这个排序特点，将经常一起读取的数据存储到一块，将最近可能会被访问的数据放在一块。

比如：如果最近写入 HBase 表中的数据是最可能被访问的，可以考虑将时间戳作为 RowKey 的一部分，由于是字典序排序，所以可以使用 Long.MAX_VALUE – timestamp 作为 RowKey，这样能保证新写入的数据在读取时可以被快速命中。

### 2.4、具有定长性

行键具有有序性的基础便是定长，譬如 20140512080500、20140512083000，这两个日期时间形式的字符串是递增的，不管后面的秒数是多少，都将其设置为14位数字形式，如果把后面的0去除了，那么 201405120805 将大于20140512083，其有序性发生了变更。

## 三、RowKey 设计原则

### 3.1、RowKey 的唯一原则

由于在 HBase 中数据存储是 Key-Value 形式，若 HBase 中同一表插入相同Rowkey，则原先的数据会被覆盖掉(如果表的 version 设置为1的话)，所以务必保证 Rowkey 的唯一性。

### 3.2、RowKey 的排序原则

HBase 的 RowKey 是按照 ASCII 有序设计的，在设计 RowKey 时要充分利用这点。比如视频网站上的弹幕信息，这个弹幕是按照时间倒排序展示视频里，这个时候设计的 Rowkey 要和时间顺序相关，可以使用 "Long.MAX_VALUE - 弹幕发表时间" 的 long  值作为 Rowkey  的前缀。

### 3.3、RowKey 的长度原则

RowKey 是一个二进制，RowKey 的长度建议设计在 10~100 个字节，越短越好。

原因有两点：

* HBase 的持久化文件 HFile 是按照 KeyValue 存储的，如果 RowKey 过长，比如 500 个字节，1000 万列数据光 RowKey 就要占用500*1000 万 = 50 亿个字节，将近 1G 数据，这会极大影响HFile的存储效率；
* MemStore 缓存部分数据到内存，如果 RowKey 字段过长内存的有效利用率会降低，系统无法缓存更多的数据，这会降低检索效率。

其实不仅 RowKey 的长度是越短越好，列族名、列名等也应该尽量使用短名字，因为 HBase 属于列式数据库，这些名字都是会写入到 HBase 的持久化文件 HFile 中去，过长的 RowKey、列族、列名都会导致整体的存储量成倍增加。

### 3.5、RowKey 散列原则

设计的 RowKey 应均匀的分布在各个 HBase节点上。

拿时间戳举例，假如 RowKey 是按系统时间戳的方式递增，时间戳信息作为RowKey 的第一部分，将造成所有新数据都在一个 RegionServer 上堆积，也就是通常说的 Region 热点问题。

热点发生在大量的 client 直接访问集中在个别 RegionServer 上（访问可能是读，写或者其他操作），导致单个 RegionServer 机器自身负载过高，引起性能下降甚至 Region 不可用，常见的是发生 jvm full gc 或者显示 region too busy 异常情况，当然这也会影响同一个 RegionServer 上的其他 Region。

设计良好的数据访问模式以使集群被充分，均衡的利用。

## 四、RowKey 避免热点的方法

首先明白热点产生原因：RowKey 前面的字符过于固定，有大量连续编号的 RowKey  集中在个别 region，client 检索记录时或者写入记录时，个别 region 负荷过大。

### 4.1、加盐 Salting

既然是因为前面字符过于集中，那么可以通过在 RowKey 前面添加随机的一个字符串，**Salting 是就在 RowKey 的前面增加随机数，使得它和之前的 RowKey 的开头不同。**这可以使得数据分散在多个不同的 Region，达到 Region 负载均衡的目标，避免热点。分配的前缀种类数量应该和想使数据分散到不同的 Region 的数量一致。

随机字符计算的一种方法：

**int saltNumber = new Long(new Long(timestamp).hashCode()) %<number of region servers>**

比如在一个有 4 个 Region (注：以 [ a)、[a,b)、[b,c)、[c, )为 Region 起至)的 HBase表中，加盐前的 RowKey：abc001、abc002、abc003，默认会在第 2 个Region中。

现在分别加上 a、b、c 前缀，加盐后 RowKey 为：a-abc001、b-abc002、c-abc003 ，数据会分布在 3 个 Region 中，理论上处理后的吞吐量应是之前的3倍。

由于前缀是随机的，读这些数据时需要耗费更多的时间，所以 **Salting 虽然增加了写操作的吞吐量，与此同时也增加了读操作的开销**。

### 4.2、Hash 散列或者 Mod 取模

**哈希会使同一行永远用一个前缀加盐，而不是随机前缀。**哈希同样可以使负载分散到整个集群，但是读却是可以预测的。使用确定的哈希(比如md5后取前4位做前缀)可以让客户端重构完整的 RowKey ，可以使用 get 操作准确获取某一个行数据。

将上述的原始 RowKey 经过 hash 处理，比如采用 md5 散列算法取前4位做前缀。结果如下：

9bf0-abc001 （abc001在md5后是9bf049097142c168c38a94c626eddf3d，取前4位是9bf0）、7006-abc002、95e6-abc003。

若以前4个字符作为不同分区的起止，上面几个 RowKey 数据会分布在3个 Region中。实际应用场景是当数据量越来越大的时候，这种设计会使得分区之间更加均衡。

如果 RowKey 是数字类型的，也可以考虑 Mod 方法。

### 4.3、反转 Reverse

第三种防止热点的方法是反转固定长度或者数字格式的 RowKey ，可以使得 RowKey 中经常改变的部分（最没有意义的部分）放在前面。这样可以有效的随机 RowKey ，但是牺牲了 RowKey 的有序性。

反转 RowKey 的例子：通常以手机举例，可以将手机号反转后的字符串作为RowKey，这样的就避免了以手机号那样比较固定开头(137x、15x等)导致热点问题。

### 4.4、时间戳反转

一个常见的数据处理问题是快速获取数据的最近版本，使用反转的时间戳作为 RowKey 的一部分对这个问题十分有用，可以用 Long.Max_Value - timestamp 追加到 key 的末尾，例如 key_reverse_timestamp ，key 的最新值可以通过 scan key 获得 key 的第一条记录，因为 HBase 中 RowKey 是有序的，第一条记录是最后录入的数据。

比如需要保存一个用户的操作记录，按照操作时间倒序排序，在设计 RowKey  的时候，可以这样设计 userId反转-(Long.Max_Value - timestamp)，在查询用户的所有操作记录数据的时候，直接指定反转后的 userId，startRow 是 userId反转-000000000000，stopRow 是 userId反转-(Long.Max_Value - timestamp)
如果需要查询某段时间的操作记录，startRow 是 user反转-(Long.Max_Value - 起始时间)，stopRow是 userId反转-(Long.Max_Value - 结束时间)。

## 五、RowKey 设计经验

**RowKey 的设计和数据的分布有很大关系，RowKey 设计的时候需要保证数据入库时的并发度，但又不能过于分散。**

### 5.1、可枚举值较少的属性放在 RowKey  前面 

在 RowKey  中，需要放入多个属性，这多个属性的先后次序和访问的效率有直接的关系。

**一个普遍的规则是：数量较少，可控的属性放在 RowKey  前面（如 ServiceType，CPID 等）；反之放在后面（如 url，mxid 等）。**这样做的原因是可控属性放在前面，对各种不同查询需求的平衡性强一些，反之平衡性较差。

#### 5.1.1、案例

```
201010-http-cp001-s-shanghai-xxx-1
201010-http-cp002-s-shenzhen-xxx-2
201010-rtsp-cp001-s-shanghai-xxx-1
201010-rtsp-cp002-s-shenzhen-xxx-21234
```

ServiceType 可枚举，并且数量较少，分为 http 和 rtsp 两种，放在前面； 而 cpid 可能会比较多（假设有5个 cp），因此放在后面。 

这样的设计能够适应如下两种需求，复杂度都比较小： 

1. 查询 2010 年 10 月所有 cp 的 http 数据。这种需求设置 scan 的 startRow= ‘201010-http-’，endRow=‘201010-http-z’，即可；
2. 查询 2010 年 10 月 cp001 的所有协议的数据。这种需求下，根据 scan RowKey  连续的原则，需要将查询划分成两个 scan，分别查询 http 类型 cp001 的数据和 rtsp 类型 cp001 的数据。

但是，如果将 cp 放在前面，如下所示，适应性就差一些，如下所示：

```
201010-cp001-http-s-shanghai-xxx-1
201010-cp002-http-s-shenzhen-xxx-2
201010-cp001-rtsp-s-shanghai-xxx-1
201010-cp002-rtsp-s-shenzhen-xxx-21234
```

1. 查询 2010 年 10 月 cp001 的所有协议的数据。这种需求下，设置 scan 的startRow=‘201010-cp001-’，endRow=‘201010-cp001-z’，即可；
2.  查询 2010 年10 月，所有 cp 的 http 数据。 这种需求下，根据 scan 的RowKey  连续原则，需要将查询分成 cp001-http、cp002-http、cp003-http、cp004-http、cp005-http 五个查询进行，相对模型一复杂了一些。

### 5.2、业务访问中权重高的 RowKey 放在前面

例如 URLRecords 表的主要用途是用来计算当天的 URL 访问排名。根据业务需求，需要访问某天的所有 URL，因此 date 是主键，权重更高，放在前面，而 URL 则放在后面。

### 5.3、构造冗余数据

例如，另一张表的数据包含了 URL Records 的数据，URL Records 的数据是冗余存储的，区别在于该表的 URL 放在 date 前面，而 URL Records 表的 URL 放在 date 后面。这就是由于 URL 在满足不同需求的时候，权重不同，由于 URL Records 需要的数据量不大，因此可以采用冗余的机制解决该矛盾。 

权衡需求的重要性和系统忍受度选择一种方案，当两种需求有矛盾，但其中一方属于次要需求，并且在系统忍受度范围之内的话，可以舍弃一种方案。优先满足需求更强的一方。

### 5.4、时间属性在 RowKey 中的使用

如果需要经常访问特定时间段的数据，将时间属性放在 RowKey 中是一个较好的选择。和利用时间戳来访问特定时间段的数据方法相比，将时间属性放在 RowKey 中具有可控性，容易将能够同时访问的数据相对集中存放的优点。 

时间属性放在 RowKey 中需要注意数据分布和并发度的问题：HBase 数据是按照 RowKey 排序的，时间属性放在 RowKey 中容易造成数据总是在末尾写入的情况，这种情况下并发度很差。可以通过在时间属性前面增加 prefix 和提前预分 Region 的方法解决。

### 5.5、循环 RowKey 使用

如果 RowKey 中有时间属性，并且随着时间的增加，RowKey 会不断的增大下去，造成 Region 数量不断地增加。如果使用 TTL 来控制数据的生命周期，一些老的数据就会过期，进而导致老的 Region 数据量会逐渐减少甚至成为空的 Region。这样一方面 Region 总数在不断增加，另外一方面老的 Region 在不断的成为空的 Region，而空的 Region 不会自动合并，进而造成过多空的 Region 占用负载和内存消耗的情况。 

这种情况下，可以使用循环 RowKey 的方法解决。思路是根据数据的生命周期设定 RowKey 的循环周期，当一个周期过去以后，通过时间映射的方法，继续使用老的过期数据的 RowKey 。 

例如 RowKey 的格式如下： YY-MM-DD-URL。如果数据的生命周期是一年，则可以使用 MM-DD-URL 的格式。这样当前一年过去以后，数据已经老化，后一年的数据可以继续写入前一年的位置，使用前一年数据的 RowKey 。这样可以避免空的 Region 占用资源的情况。

根据 HBase 的原理，RowKey 的周期需要至少比 TTL 大 2* hbase.hregion.majorcompaction（默认24小时）的时间，才能够保证过期的数据能够在 RowKey  循环回来之前得到完全清理。 

按照时间周期进行建表的方式也可以解决空 Region 的问题，和循环 RowKey 方法相比较，循环 RowKey  的优点如下：

- 操作简单，不需要重复建表，系统自动处理

同样，循环 RowKey 具有如下劣势：

- 需要使用 TTL 来老化数据，可能会增加 compact 负担
- 需要保证查询操作不会查询到过期数据，否则会影响系统性能。

如果在系统压力不是特别大，需要长期运行，能够控制查询不会查询到过期数据的场景下，建议使用 TTL+循环 RowKey 的方式，否则建议使用按照时间周期进行建表的方式。

## 六、RowKey 设计案例剖析

### 6.1、交易类表 RowKey 设计

- 查询某个卖家某段时间内的交易记录
  *sellerId + timestamp + orderId*

- 查询某个买家某段时间内的交易记录
  *buyerId + timestamp + orderId*

- 根据订单号查询
  *orderNo*

- 如果某个商家卖了很多商品，可以如下设计 RowKey 实现快速搜索
  *salt + sellerId + timestamp*
  其中，salt 是随机数。可以支持的场景：

  - 全表 Scan
  - 按照 sellerId 查询
  - 按照 sellerId + timestamp 查询

### 6.2、金融风控 RowKey 设计

查询某个用户的用户画像数据

- *prefix + uid*
- *prefix + idcard*
- *prefix + tele*

其中 prefix = substr(md5(uid),0 ,x)，x  取 5-6。uid、idcard以及 tele 分别表示用户唯一标识符、身份证、手机号码。

### 6.3、车联网 RowKey 设计

- 查询某辆车在某个时间范围的交易记录
  *carId + timestamp*
- 某批次的车太多，造成热点
  *prefix + carId + timestamp* 其中 prefix = substr(md5(uid),0 ,x)

### 6.4、查询最近的数据

查询用户最新的操作记录或者查询用户某段时间的操作记录，RowKey 设计如下：
*uid + Long.Max_Value - timestamp*
支持的场景：

- 查询用户最新的操作记录
  *Scan [uid] startRow [uid][000000000000] stopRow [uid][Long.Max_Value - timestamp]*
- 查询用户某段时间的操作记录
  *Scan [uid] startRow [uid][Long.Max_Value – startTime] stopRow [uid][Long.Max_Value - endTime]*

## 七、参考博文

1.[HBase设计之RowKey行键设计规范（2）](https://blog.csdn.net/xiangxizhishi/article/details/76997647)
2.[[HBase的rowkey的设计原则](https://www.cnblogs.com/yuguoshuo/p/6265649.html)
3.[HBase rowkey设计案例](https://blog.csdn.net/chengyuqiang/article/details/79134549)
4.[HBase Rowkey 设计指南](https://www.iteblog.com/archives/2486.html)