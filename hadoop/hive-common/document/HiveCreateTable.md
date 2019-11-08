# 【Hive】Hive 建表语句详解

[TOC]

## 一、hql 建表语法格式

 *hql不区分大小写，[]里的属性是可选属性*。

 ```sql
  CREATE [EXTERNAL] TABLE [IF NOT EXISTS] table_name      
  [(col_name data_type [COMMENT col_comment], ...)]      
  [COMMENT table_comment]                                 
  [PARTITIONED BY(col_name data_type [COMMENT col_comment], ...)]
  [CLUSTERED BY (col_name, col_name, ...)
  [SORTED BY(col_name [ASC|DESC], ...)] INTO num_buckets BUCKETS]
  [ROW FORMAT row_format] 
  [STORED AS file_format]
  [LOCATION hdfs_path]  
 ```

## 二、参数说明

1. `CREATE TABLE` 创建一个指定名字的表。如果相同名字的表已经存在，则抛出异常；用户可以用 IF NOT EXISTS 选项来忽略这个异常。

2. `EXTERNAL` 关键字可以让用户创建一个外部表，默认是内部表。外部表在建表的必须同时指定一个指向实际数据的路径（LOCATION），Hive 创建内部表时，会将数据移动到数据仓库指向的路径；若创建外部表，仅记录数据所在的路径，不对数据的位置做任何改变。在删除表的时候，内部表的元数据和数据会被一起删除，而外部表只删除元数据，不删除数据。

3. `COMMENT` 是给表字段或者表内容添加注释说明的。

4. `PARTITIONED BY` 给表做分区，决定了表是否是分区表。

5.  `CLUSTERED BY` 对于每一个表（table）或者分区， Hive 可以进一步组织成桶，也就是说桶是更为细粒度的数据范围划分，**Hive采用对列值哈希，然后除以桶的个数求余的方式决定该条记录存放在哪个桶当中**。

6.  `ROW FORMAT DELIMITED FIELDS TERMINATED BY ','`， 这里指定表存储中列的分隔符，默认是 \001，这里指定的是逗号分隔符，还可以指定其他列的分隔符。

7.  `STORED AS  SEQUENCEFILE|TEXTFILE|RCFILE`，如果文件数据是纯文本，可以使用 STORED AS TEXTFILE，如果数据需要压缩，使用 STORED AS SEQUENCEFILE。

8.  `LOCATION` 定义 hive 表的数据在 hdfs 上的存储路径，一般管理表（内部表不不要自定义），但是如果定义的是外部表，则需要直接指定一个路径。

   

## 三、创建表的三种方式

### 3.1、 使用 create 命令

```sql
CREATE  TABLE `employee`(  
      `dept_no` int,   
      `addr` string,   
      `tel` string)
    partitioned by(statis_date string ) 
    ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
```

详细参考上述建表说明。

可以使用`describe formatted employee`查看建表相关的各种配置属性以及默认属性。 

### 3.2、 create table ...as select..(CTAS)

 [AS select_statement] 这个语句是用来通过查询已有的表来创建一张新表，这样可以根据已有的表来创建子表，对于数据分析和优化都是有很大的好处的。 

```sql
create table employee1 
        as   
       select *  from employee where statis_date='20180229';
```

1. 使用查询创建**并填充表**，select 中选取的列名会作为新表的列名（所以通常是要取别名）；
2. 会改变表的属性、结构，比如只能是内部表、分区分桶也没了：
   *  目标表不允许使用分区分桶的，`FAILED: SemanticException [Error 10068]: CREATE-TABLE-AS-SELECT does not support partitioning in the target table`，对于旧表中的分区字段，如果通过 select * 的方式，新表会把它看作一个新的字段，这里要注意 ；
   * 目标表不允许使用外部表，如 create external table … as select… 报错 `FAILED: SemanticException [Error 10070]: CREATE-TABLE-AS-SELECT cannot create external table`；
   * 创建的表存储格式会变成默认的格式 TEXTFILE 。
3.   可以指定表的存储格式，行和列的分隔符等。

### 3.3、 使用 like 创建相同结构的表

```sql
CREATE TABLE LIKE
```

- 用来复制表的结构
- 需要外部表的话，通过 create external table like … 指定
- 不填充数据