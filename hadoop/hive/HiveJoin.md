# 【Hive】Hive Join 介绍

[TOC]

## 一、Join 

**Hive 中的 Join 只支持等值 Join，也就是说 Join on 中的 on 里面表之间连接条件只能是 = ，不能是 <,> 等符号。此外，on中的等值连接之间只能是 and，不能是or。**

Hive 执行引擎会将 HQL “翻译” 成为map-reduce 任务，在执行表的 Join 操作时，如果多个表中每个表都使用同一个列进行连接（出现在 Join on 子句中），则只会生成一个 MR Job：

```sql
SELECT a.val, b.val, c.val FROM a JOIN b ON (a.key = b.key1) JOIN c ON (c.key = b.key1)
```

三个表 a、b、c 都分别使用了同一个字段进行连接，亦即同一个字段同时出现在两个 Join 子句中，从而只生成一个 MR Job。

如果多表中，其中存在一个表使用了至少 2 个字段进行连接（同一个表的至少2个列出现在 Join 子句中），则会至少生成 2 个MR Job：

```sql
SELECT a.val, b.val, c.val FROM a JOIN b ON (a.key = b.key1) JOIN c ON (c.key = b.key2)
```

三个表基于 2 个字段进行连接，这两个字段 b.key1 和 b.key2 同时出现在 b 表中。连接的过程是这样的：首先 a 和 b 表基于a.key 和 b.key1 进行连接，对应着第一个 MR Job；表 a 和 b 连接的结果，再和 c 进行连接，对应着第二个 MR Job。

这是因为 Map 输出时候以 Join on 条件中的列为 key，如果 Join 有多个关联键，则以这些关联键的组合作为 key，Map 根据 Key 分发数据给 Reduce 端，具体的 Join 是在 Reduce 操作中完成，因此，如果多表基于不同的列做 Join，则无法在一轮 MR 任务中将所有相关数据 shuffle 到同一个 Reduce 操作中。 

## 二、Join 类型

Hive 支持常用的 SQL Join 语句，例如内连接、左外连接、右外连接以及 Hive 独有的 map 端连接。其中 map 端连接是用于优化 Hive 连接查询的一个重要技巧。

先准备三张表。

employee员工表：

```sql
create table if not exists employee(
user_id int,
username string,
dept_id int)
row format delimited 
fields terminated by ' '  
lines terminated by '\n';
```

dept部门表：

```sql
create table if not exists dept(
dept_id int,
dept_name string)
row format delimited 
fields terminated by ' '  
lines terminated by '\n';
```

薪水表:

```sql
create table if not exists salary(
userid int,
dept_id int,
salarys double)
row format delimited 
fields terminated by ' '  
lines terminated by '\n';
```

### 2.1、INNER JOIN 内连接

多张表进行内连接操作时，只有所有表中与 on 条件中相匹配的数据才会显示，类似取交集。

```sql
select e.username,e.dept_id,d.dept_name,d.dept_id from employee e join dept d on e.dept_id = d.dept_id
```

### 2.2、LEFT OUTER JOIN 左外连接

JOIN 操作符**左边表中符合 where 条件的所有记录都会被保留**，JOIN 操作符右边表中如果没有符合 on 后面连接条件的记录，则从右边表中选出的列为NULL，如果没有 where 条件，则左边表中的记录都被保留。

标准查询关键字执行顺序为 **from->on->where->group by->having->order by**，on 是先对表进行筛选后再关联的，left 关联则 on 只对右表有效，左表都要选出来。

对于大量的数据，在编写 SQL 时尽量用 where 条件过滤掉不符合条件的数据是有益的。但是对于左外连接和右外连接，**where 条件是在 on 条件执行之后才会执行，on 条件会产生一个临时表，where 条件是对这个临时表进行过滤**。

因此为了优化 Hive SQL 执行的效率，**在需要使用外连接的场景，如果是要条件查询后才连接应该把查询件放置于 on 后，如果是想再连接完毕后才筛选就应把条件放置于 where 后面，对主表的筛选要用 where 条件**。

**特别要注意的是，如果是需要对主表过滤之后再和从表做左关联，最好将主表写成子查询的形式，可以减少主表的数据量**：

```sql
select e1.user_id,e1.username,s.salarys from (select e.* from employee e where e.user_id < 8) e1 left outer join salary s on e1.user_id = s.userid;
```

### 2.3、RIGHT OUTER JOIN 右外连接

RIGHT OUTER JOIN，与 LEFT OUTER JOIN 相对，**JOIN 操作符右边表中符合where 条件的所有记录都会被保留**，JOIN 操作符左边表中如果没有符合 on 后面连接条件的记录，则从左边表中选出的列为 NULL。

```sql
select e.user_id,e.username,s.salarys from employee e right outer join salary s on e.user_id = s.userid;
```

### 2.4、FULL OUTER JOIN 全外连接

保留满足 where 条件的两个表的数据，类似并集，没有符合连接条件的字段使用 NULL 填充。 

```sql
select e.user_id,e.username,s.salarys from employee e full outer join salary s on e.user_id = s.userid where e.user_id > 0;
```

### 2.5、LEFT SEMI JOIN 左半开连接

以 LEFT SEMI JOIN 关键字前面的表为主表，返回主表的 KEY 也在副表中的记录。在早期的 Hive 版本中，不支持标准 SQL 中的 IN 或 EXISTS 的功能，可以使用LEFT SEMI JOIN 实现类似的功能。

```sql
select e.* from employee e LEFT SEMI JOIN salary s on e.user_id=s.userid;
```

需要强调的是：

- 左半开连接是内连接的优化，当左边表的一条数据，在右边表中存在时，Hive就停止扫描。因此效率比 join 高。
- 左半开连接的 select 和 where 关键字后面只能出现左边表的字段，不能出现右边表的字段。
- Hive 不支持右半开连接。

### 2.6、JOIN笛卡尔积

笛卡尔积是一种连接，表示左边表的行数乘以右边表的行数。

```sql
select e.user_id,e.username,s.salarys from employee e join salary s;
```

## 三、Join 的实现原理

> PS：该段内容来自参考博文 https://blog.csdn.net/login_sonata/article/details/75000766。

Hive中的 Join 可分为 Common Join（Reduce 阶段完成 join）和 Map Join（Map 阶段完成 join）。

### 3.1、Common Join

如果不指定 Map Join 或者不符合 Map Join 的条件，那么 Hive 解析器会默认把执行 Common Join，即在 Reduce 阶段完成 join。整个过程包含 Map、Shuffle、Reduce 阶段。

1. **Map阶段**：读取源表的数据，Map 输出时候以 Join on 条件中的列为key，如果 Join 有多个关联键，则以这些关联键的组合作为 key；Map 输出的 value 为 join 之后所关心的(select 或者 where 中需要用到的)列，同时在 value 中还会包含表的 Tag 信息，用于标明此 value 对应哪个表。
2. **Shuffle阶段**：根据 key 的值进行 hash，并将 key/value 按照 hash 值推送至不同的 reduce 中，这样确保两个表中相同的 key 位于同一个 reduce 中。
3. **Reduce阶段**：根据 key 的值完成 join 操作，期间通过 Tag 来识别不同表中的数据。

以下面 HQL 为例，图解其过程：

```sql
SELECT a.id,a.dept,b.age 
FROM a join b 
ON (a.id = b.id);
```

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g5y32t1s8zj20mz062tcw.jpg)

### 3.2、Map Join

Map Join 通常用于一个很小的表和一个大表进行 join 的场景，具体小表有多小，由参数 hive.mapjoin.smalltable.filesize 来决定，该参数表示小表的总大小，默认值为 25000000 字节，即 25M。 

Hive 0.7 之前，需要使用 hint 提示 /*+ mapjoin(table)* / 才会执行Map Join，否则执行 Common Join，但在 0.7 版本之后，默认自动会转换 Map Join，由参数hive.auto.convert.join 来控制，默认为 true。

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g5y35xtd48j20i80fd0xj.jpg)

如上图中的流程，**首先Task A** 在客户端本地执行，负责扫描小表 b 的数据，将其转换成一个 HashTable 的数据结构，并写入本地的文件中，之后将该文件加载到DistributeCache 中。

接下来是 Task B，该任务是一个没有 Reduce 的 MR，启动 MapTasks 扫描大表 a,在 Map 阶段，根据 a 的每一条记录去和 DistributeCache 中 b 表对应的 HashTable 关联，并直接输出结果。 

由于 MapJoin 没有 Reduce，所以由 Map 直接输出结果文件，有多少个 Map Task，就有多少个结果文件。

## 四、Join 优化

> PS：该段来自博文：[http://datavalley.github.io/2015/10/25/Hive%E4%B9%8BJOIN%E5%8F%8AJOIN%E4%BC%98%E5%8C%96](http://datavalley.github.io/2015/10/25/Hive之JOIN及JOIN优化)

### 4.1、BUCKET MAP JOIN

Map Join 效率比 Common Join  效率好，但总会有“小表”条件不满足的时候。这就需要 bucket map join 了。

Bucket map join 需要待连接的两个表在连接字段上进行分桶（每个分桶对应hdfs上的一个文件），而且小表的桶数需要时大表桶数的倍数。

建立分桶表的例子：

```SQL
CREATE TABLE my_user
(uid INT,
 name STRING)
CLUSTERED BY (uid) into 32 buckets
STORED AS TEXTFILE;
```

这样，my_user 表就对应 32 个桶，数据根据 uid 的 hash value 与32 取余，然后被分发导不同的桶中。

如果两个表在连接字段上分桶，则可以执行 bucket map join 了，具体的：

1. 设置属性 `hive.optimize.bucketmapjoin= true` 控制 hive 执行 bucket map join；
2. 对小表的每个分桶文件建立一个 hashtable，并分发到所有做连接的 map端；
3. map 端接受了N（N为小表分桶的个数） 个小表的 hashtable，做连接操作的时候，只需要将小表的一个 hashtable 放入内存即可，然后将大表的对应的 split 拿出来进行连接，所以其内存限制为小表中最大的那个hashtable 的大小。

### 4.2、SORT MERGE BUCKET MAP JOIN

对于 bucket map join 中的两个表，如果每个桶内分区字段也是有序的，则还可以进行 sort merge bucket map join。

建表语句为：

```SQL
CREATE TABLE my_user
( uid INT,
  name STRING)
CLUSTERED BY (uid) SORTED BY (uid) into 32 buckets
STORED AS TEXTFILE;
```

这样一来当两边 bucket 要做局部 join 的时候，只需要用类似 merge sort 算法中的 merge 操作一样把两个 bucket 顺序遍历一遍即可完成，小表的数据可以每次只读取一部分，然后还是用大表一行一行的去匹配，这样的join 没有限制内存的大小. 并且也可以执行全外连接。

进行sort merge bucket map join时，需要设置的属性为：

```SQL
set hive.optimize.bucketmapjoin= true;
set hive.optimize.bucketmapjoin.sortedmerge = true;
set hive.input.format = org.apache.hadoop.hive.ql.io.BucketizedHiveInputFormat;
```

### 4.3、JOIN 对比

| JOIN类型                    | 优点                                             | 缺点                                                      |
| --------------------------- | ------------------------------------------------ | --------------------------------------------------------- |
| COMMON JOIN                 | 可以完成各种 JOIN 操作，不受表大小和表格式的限制 | 无法只在 map 端完成 JOIN 操作，耗时长，占用更多地网络资源 |
| MAP JOIN                    | 可以在 map 端完成 JOIN 操作，执行时间短          | 待连接的两个表必须有一个“小表”，“小表”必须加载内存中      |
| BUCKET MAP JOIN             | 可以完成 MAP JOIN，不受“小表”限制                | 表必须分桶，做连接时小表分桶对应 hashtable 需要加载到内存 |
| SORT MERGE  BUCKET MAP JOIN | 执行时间短，可以做全连接，几乎不受内存限制       | 表必须分桶，而且桶内数据有序                              |

## 五、Join 在倾斜表中的优化

Join 的过程中，Map 结束之后，会将相同的 Key 的数据 shuffle 到同一个 Reduce中，如果数据分布均匀的话，每个Reduce 处理的数据量大体上是比较均衡的，但是若明显存在数据倾斜的时候，会出现某些 Reducer 处理的数据量过大，从而使得该节点的处理时间过长，成为瓶颈。

### 5.1、大表和小表关联

1. 多表关联时，将小表(关联键重复记录少的表)依次放到前面，这样可以触发 reduce 端更少的操作次数，减少运行时间。
2. 同时可以使用 Map Join 让小的维度表缓存到内存。在map端完成join过程，从而省略掉redcue端的工作。

### 5.2、大表和大表的关联

大表与大表关联，如果其中一张表的多是空值或者 0 比较多，容易 shuffle 给一个reduce，造成运行慢。

这种情况可以对异常值赋一个随机值来分散 key，均匀分配给多个 reduce 去执行，比如：

```sql
select *
  from log a
  left outer join users b
  on case when a.user_id is null then concat('hive',rand() ) else a.user_id end = b.user_id;

-- 将A表垃圾数据（为null）赋一个随机的负数，然后将这些数据shuffle到不同reduce处理。
```

当 key 值都是有效值时，解决办法为：

设置以下参数：

```shell
# 每个节点的 reduce 默认是处理 1G 大小的数据
set hive.exec.reducers.bytes.per.reducer = 1000000000 
# 如果 join 操作也产生了数据倾斜，可以设定
set hive.optimize.skewjoin = true;
set hive.skewjoin.key = skew_key_threshold (default = 100000)

```

Hive 在运行的时候无法判断哪个 key 会产生倾斜，所以使用 hive.skewjoin.key 参数控制倾斜的阈值，如果超过这个值，新的值会发送给那些还没有达到的 reduce，一般可以设置成待处理的总记录数/reduce 个数的 2-4 倍。

## 六、参考博文

1.[Hive基础二（join原理和机制，join的几种类型，数据倾斜简单处理）](https://blog.csdn.net/login_sonata/article/details/75000766)
2.[Hive:JOIN及JOIN优化](http://datavalley.github.io/2015/10/25/Hive之JOIN及JOIN优化)
3.[hive中关于常见数据倾斜的处理](https://blog.csdn.net/qq_26442553/article/details/80866723)