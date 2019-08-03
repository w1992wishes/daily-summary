# 【HBase】HBase 协处理器

[TOC]

## 一、协处理器的产生

HBase 和 MapReduce 有很高的集成，可以使用 MR 对存储在 HBase 中的数据进行分布式计算，但是：

* 有些情况，例如简单的加法计算或者聚合操作（求和、计数等），如果能够将这些计算推送到 RegionServer，这将大大减少服务器和客户的的数据通信开销，从而提高 HBase 的计算性能。
* 另外，HBase 作为列式数据库，无法轻易建立“二级索引”，对于查询条件不在行健中的数据访问，效率十分低下。

在这种情况下，HBase 在 0.92 之后引入了协处理器(coprocessors)，它允许用户将部分逻辑在数据存放端即 HBase RegionServer 服务端进行计算，也即允许用户在  RegionServer  运行用户自定义的代码。

协处理器的引入，执行求和、计数、排序等操作将变得更加高效，因为 RegionServer 将处理好的数据再返回给客户端，这可以极大地降低需要传输的数据量，从而获得性能上的提升。同时协处理器也允许用户扩展实现 HBase 目前所不具备的功能，如权限校验、二级索引、完整性约束等。

## 二、协处理器的类型

协处理器可以为全局 Region Server 上所有的表所使用，也可以为某一张表单独所使用，从这个方向划分，协处理器可分为：**系统协处理器和表协处理器**。

从功能上看，也是为了更好保持其扩展的灵活性，协处理器又可分为**观察者(Observer) 和 终端 (Endpoint)** 两类。

* Observer 提供了一些设计好的回调函数（钩子），类似于关系数据库的触发器，也可以类比面向切面编程中的 Advice；
* Endpoint 自定义操作添加到服务器端，有点像存储过程。

### 2.1、Observer

Observer 是一些散布在 HBase Server 端代码中的 hook 钩子，在一些特定的事件发生时被执行，这些事件包括用户产生的事件，也包括服务器内部产生的事件。

比如： put 操作之前有钩子函数 prePut，该函数在 put 操作执行前会被 Region Server 调用；在 put 操作之后则有 postPut 钩子函数。

#### 2.1.1、适用场景

- **权限校验**：在执行`Get`或`Put`操作之前，可以使用`preGet`或`prePut`方法检查权限；
- **完整性约束**： HBase 不支持关系型数据库中的外键功能，可以通过触发器在插入或者删除数据的时候，对关联的数据进行检查；
- **二级索引**：使用钩子关联行修改操作来维护二级索引。

#### 2.1.2、Observer 类型

目前 HBase 内置实现的 Observer 主要有以下几个：

- **WALObserver**：运行于 RegionServer  中，进行 WAL 写和刷新之前或之后会触发这个钩子函数，一个 RegionServer 只有一个 WAL 的上下文；
- **MasterObserver**：运行于 Master 进程中，进行诸如 DDL 的操作，如 create, delete, modify table 等之前或之后会触发这个钩子函数；
- **RegionObserver**：基于表的 Region 上的 Get, Put, Delete, Scan 等操作之前或之后触发，比如可以在客户端进行 Get 操作的时候定义 RegionObserver 来校验是否具有 Get 权限等；
- **BulkLoadObserver**：进行 BulkLoad 的操作之前或之后会触发这个钩子函数；
- **RegionServerObserver** ：RegionServer 上发生的一些操作可以触发这个钩子函数，这个是 RegionServer 级别的事件；
- **EndpointObserver**：每当用户调用 Endpoint 之前或之后会触发这个钩子。

#### 2.1.3、执行流程

以 RegionObserver 为例，其执行流程大致如下图：

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g5jbgz443nj20jc0ba74k.jpg)

- 客户端发出 put 请求
- 该请求被分派给合适的 RegionServer 和 Region
- CoprocessorHost 拦截该请求，然后在该表的每个 RegionObserver 上调用 prePut()
- prePut()  处理后，在 Region 执行 Put 操作
- Region 产生的结果再次被 CoprocessorHost 拦截，调用 postPut()
- 终结果被返回给客户端

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g5ja1ozrehj20mt0dzq3b.jpg)



#### 2.2.4、Observer Example

HBase 本身是不支持二级索引（ Secondary Index）的，基于索引检索数据只能单纯地依靠 RowKey，为了能支持多条件查询，开发者需要将所有可能作为查询条件的字段尽可能拼接到 RowKey 中，这是 HBase 开发中极为常见的做法，也是推荐的做法。

在《 HBase 实战》一书中，有个例子：当 a 关注 b 时，在 follower 表添加一行数据，RowKey 为 hash(a)+hash(b)，同时在 followedBy 表添加一行数据，RowKey  为 hash(b)+hash(a)，这样就能实现**我关注了谁**和**谁关注了我**这样的需求。

但是 HBase 不提供跨行事务去保证这两张表数据的一致性，这时候就可以利用 Observer 的 hook 函数提供一致性的保证。当 postPut 函数失败的时候，HBase 会自动重试 postPut 函数，直到 postPut 函数执行成功，通过同步重试来保证多条数据是同时插入成功的。

这其实就是二级索引的实现：在往一张表 A 中插入数据的时候，利用协处理器将 A表中的值作为行健、A 表的行健作为值插入 B 表，然后查询的时候，就可以通过 B 查询出 A 中的行健，继而快速从 A 中检索出需要的数据。

```java
/**
 * 2.0 版本之前使用extends BaseRegionObserver 实现
 *
 * @author w1992wishes 2019/7/30 20:52
 */
public class FollowsObserver implements RegionObserver, RegionCoprocessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FollowsObserver.class);

    private Connection conn;

    @Override
    public void start(CoprocessorEnvironment env) throws IOException {
        LOGGER.info("****** start ******");
        conn = ConnectionFactory.createConnection(env.getConfiguration());
    }

    @Override
    public void stop(CoprocessorEnvironment env) throws IOException {
        LOGGER.info("****** stop ******");
        conn.close();
    }

    @Override
    public void postPut(ObserverContext<RegionCoprocessorEnvironment> c, Put put, WALEdit edit, Durability durability)
            throws IOException {

        byte[] table = c.getEnvironment().getRegion().getRegionInfo().getTable().getName();
        if (!Bytes.equals(table, FOLLOWS_TABLE_NAME)) {
            return;
        }

        Cell fCell = put.get(RELATION_FAM, FROM).get(0);
        String from = Bytes.toString(fCell.getValueArray());
        Cell tCell = put.get(RELATION_FAM, TO).get(0);
        String to = Bytes.toString(tCell.getValueArray());

        RelationsDAO relationsDAO = new RelationsDAO(conn);
        relationsDAO.addFollowedBy(to, from);
        LOGGER.info("****** Create followedBy relation successfully! ****** ");
    }

}
```

### 2.2、Endpoint 

#### 2.2.1、适用场景

Endpoint 和 RDMBS 的存储过程很类似，用户提供一些自定义代码，并在 HBase 服务器端执行，结果通过 RPC 返回给客户，Endpoint 可以实现 min、 max、 avg、 sum、 distinct、 group by 等功能。

以聚合为例，如果没有协处理器，当用户需要找出一张表中的最大数据，即 max 聚合操作，需要进行全表扫描，返回所以数据给客户端，然后在客户端遍历扫描结果，查找最大值。

这是一种典型的“移动数据”的计算方案，将所有数据都移动到计算一端 Client，由 Client 端统一执 行，这样无法利用底层集群的并发能力，效率低下，而利用 Coprocessor，则可以实现“移动计算”，用户可以将求最大值的代码放到 HBase Server 端，利用 HBase 集群的多个节点并发执行求最大值的操作。即在每个 Region 范围内 执行求最大值的代码，将每个 Region 的最大值在 Region Server 端计算出，仅仅将该 max 值返回给客户端。在客户端进一步将多个 Region 的最大值进一步处理而找到其中的最大值。这样整体的执行效率就会提高很多。

#### 2.2.2、执行流程

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g5k9a8h7oij20oe0c93z8.jpg)

#### 2.2.3、Endpoint Example

**第一步：首先要安装 protobuf**

Protobuf Buffers 是一种轻便高效的结构化数据存储格式，可以用于数据序列化。适合做数据存储或 RPC 数据交换格式。用于通讯协议、数据存储等领域的语言无关、平台无关、可扩展的序列化结构数据格式。

在 HBase 中使用的 Protobuf 版本为 2.5.0，所以选择安装相同版本的 Protobuf。可以从官方的下载页面找到所以版本：https://repo1.maven.org/maven2/com/google/protobuf/protoc/。

下载后，配置好环境变量。

**第二步：使用Protobuf生成序列化类**

与 Observer 类型不同的是，Endpoint 协处理器需要与服务区直接通信，服务端是对于 Protobuf Service 的实现，所以两者之间会有一个基于 protocol 的 RPC 接口，客户端和服务端都需要进行基于接口的代码逻辑实现。

1）先准备一个 proto 文件 Count.proto，使用 ProtoBuf  的 message 做为消息传递的格式，使用 Rpc 做为传输协议，一般会定义三个 ProtoBuf  域，用于请求、响应、和业务实现：

```protobuf
syntax = "proto2";

option java_package = "me.w1992wishes.hbase.inaction.coprocessors";
option java_outer_classname = "CountCoprocessor";
option java_generic_services = true;
option java_generate_equals_and_hash = true;
option optimize_for = SPEED; 

/*具体的消息
 *每个message域对应一个内部类，该内部类还包含一个Builder内部类
 *域内字段会生成对应的 setter和getter方法
 *使用 Builder 内部类来对字段赋值
 **/
message CountRequest {
    required string startKey = 1;
    required string endKey = 2;
}

message CountResponse {
    required int64 count = 1 [default = 0]; 
}
/*提供服务的类
 *该类没有Builder内部类
 */
service CountService { 
    rpc count(CountRequest) 
      returns (CountResponse); 
}
```

2）在命令行执行命令生成 Java 类：

```shell
protoc --java_out=E:\project\my_project\test Count.proto
```

运行后会生成 CountCoprocessor.java 类，将其拷贝到对应的包下面，并添加相应的依赖。

```xml
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>2.5.0</version>
</dependency>
```

3）Endpoint Coprocessor服务端实现：

```java
/**
 * 说明：hbase 协处理器 Endpooint 的服务端代码
 * 功能：继承通过 protocol buffer 生成的 rpc 接口，在服务端获取指定列的数据后进行求和操作，最后将结果返回客户端
 *
 * @author w1992wishes 2019/8/1 16:58
 */
public class RelationCountEndpoint extends CountCoprocessor.CountService implements Coprocessor, CoprocessorService {

    private RegionCoprocessorEnvironment env;

    @Override
    public Service getService() {
        return this;
    }

    @Override
    public void start(CoprocessorEnvironment env) throws IOException {
        if (env instanceof RegionCoprocessorEnvironment) {
            this.env = (RegionCoprocessorEnvironment) env;
        } else {
            throw new CoprocessorException("Must be loaded on a table region!");
        }
    }

    @Override
    public void stop(CoprocessorEnvironment env) throws IOException {
        // do nothing
    }

    @Override
    public void followedByCount(RpcController controller, CountCoprocessor.CountRequest request, RpcCallback<CountCoprocessor.CountResponse> done) {
        Scan scan = new Scan();
        byte[] startKey = Bytes.toBytes(request.getStartKey());
        scan.withStartRow(startKey);
        scan.setFilter(new PrefixFilter(startKey));
        scan.addColumn(RELATION_FAM, FROM);
        scan.readVersions(1);

        CountCoprocessor.CountResponse response = null;

        try (InternalScanner scanner = env.getRegion().getScanner(scan)) {
            List<Cell> results = new ArrayList<>();
            boolean hasMore;
            long sum = 0L;

            do {
                // count 个数
                hasMore = scanner.next(results);
                sum += results.size();
                results.clear();

                // 累加
                /*hasMore = scanner.next(results);
                for (Cell cell : results) {
                    sum = sum + Bytes.toLong(CellUtil.cloneValue(cell));
                }
                results.clear();*/
            } while (hasMore);

            response = CountCoprocessor.CountResponse.newBuilder().setCount(sum).build();
        } catch (IOException ioe) {
            ResponseConverter.setControllerException(controller, ioe);
        }

        done.run(response);
    }
}
```

4）Endpoint Coprocessor 客户端实现：

```java
public long followedByCount(final String userId) throws Throwable {

    Table followed = conn.getTable(TableName.valueOf(FOLLOWED_TABLE_NAME));

    final byte[] startKey = Md5Utils.md5sum(userId);
    final byte[] endKey = Arrays.copyOf(startKey, startKey.length);
    endKey[endKey.length - 1]++;

    final CountCoprocessor.CountRequest request = CountCoprocessor.CountRequest.newBuilder()
            .setStartKey(Bytes.toString(startKey))
            .setEndKey(Bytes.toString(endKey))
            .build();

    Batch.Call<CountCoprocessor.CountService, Long> callable = countService -> {
        ServerRpcController controller = new ServerRpcController();
        BlockingRpcCallback<CountCoprocessor.CountResponse> rpcCallback = new BlockingRpcCallback<>();

        countService.followedByCount(controller, request, rpcCallback);

        CountCoprocessor.CountResponse response = rpcCallback.get();
        if (controller.failedOnException()) {
            throw controller.getFailedOn();
        }
        return (response != null && response.getCount() != 0) ?
                response.getCount() : 0;
    };

    Map<byte[], Long> results =
            followed.coprocessorService(
                    CountCoprocessor.CountService.class,
                    startKey,
                    endKey,
                    callable);

    long sum = 0;
    for (Map.Entry<byte[], Long> e : results.entrySet()) {
        sum += e.getValue();
    }
    
    return sum;
}
```

## 三、协处理的加载方式

使用协处理器，有两种方式：通过静态（使用 HBase 配置）或动态（使用 HBase Shell 或 Java API）加载它。

- 静态加载的协处理器称之为 **System Coprocessor**（系统级协处理器），作用范围是整个 HBase 上的所有表，需要重启HBase服务；
- 动态加载的协处理器称之为  **Table Coprocessor**（表处理器），作用于指定的表，不需要重启 HBase 服务。

### 3.1、静态加载与卸载

#### 3.1.1、静态加载

1. 首先在 hbase-site.xml 文件里面进行配置需要加载的协处理器：

   ```xml
   <property>
     <name>hbase.coprocessor.region.classes</name>
     <value>me.w1992wishes.hbase.coprocessor.endpoint.RelationCountEndpoint</value>
   </property>
   ```

   `<name>`标签的值必须是下面其中之一：

   - `hbase.coprocessor.region.classes` for RegionObservers and Endpoints.

   - `hbase.coprocessor.wal.classes` for WALObservers.

   - `hbase.coprocessor.master.classes` for MasterObservers.

   `<value>` must contain the fully-qualified class name of your coprocessor’s implementation class.

2. Put your code on HBase’s classpath. One easy way to do this is to drop the jar (containing you code and all the dependencies) into the `lib/` directory in the HBase installation.

3. Restart HBase.

#### 3.1.2、静态卸载

1. Delete the coprocessor’s ` <property>` element, including sub-elements, from `hbase-site.xml`.
2. Restart HBase.
3. Optionally, remove the coprocessor’s JAR file from the classpath or HBase’s `lib/` directory.

### 3.2、动态加载与卸载

使用动态加载协处理器，不需要重新启动 HBase，但动态加载的协处理器是基于每个表加载的，只能用于所指定的表，因此动态加载协处理器有时候称为 **Table Coprocessor**。

此外，在使用动态加载必须使表脱机（disable）以加载协处理器。动态加载通常有两种方式：Shell 和 Java API 。

#### 3.2.1、使用 HBash Shell 动态加载

1. Disable the table using HBase Shell:

   ```shell
   hbase> disable 'follows'
   ```

2. Load the Coprocessor, using a command like the following:

   ```shell
   hbase> alter 'follows', METHOD => 'table_att', 'Coprocessor'=>'hdfs:///hbase/lib/hbase-coprocessor.jar|me.w1992wishes.hbase.coprocessor.observer.FollowsObserver|1001'
   ```

   `Coprocessor` 包含由管道（|）字符分隔的四个参数，按顺序解释如下：

   - File path：协处理器实现类所在 Jar 包的路径，这个路径要求所有的 RegionServer 能够读取得到。比如放在所有 RegionServer 的本地磁盘；比较推荐的做法是将文件放到 HDFS 上。如果没有设置这个值，那么将直接从 HBase 服务的 classpath 中读取。
   - Class name：The full class name of the Coprocessor.
   - Priority：协处理器的优先级，是一个整数，遵循数字的自然序，即值越小优先级越高。如果同一个钩子函数有多个协处理器实现，那么将按照优先级执行。如果没有指定，将按照默认优先级执行。
   - **Arguments**：传递给协处理器实现类的参数列表，可以不指定。

3. Enable the table.

   ```shell
   hbase> enable 'follows'
   ```

4. Verify that the coprocessor loaded:

   ```shell
   hbase > describe 'follows'
                                 
   follows, {TABLE_ATTRIBUTES => {coprocessor$1 => 'hdfs://master:9000/hbase/lib/hbase-coprocessor.jar|me.w1992wishes.hbase.coprocessor.observer.FollowsObserver|1001'}                           
   ```

5. 部署后使用验证一下：

   ```java
   public class RelationsTool {

    private static final Logger log = LoggerFactory.getLogger(RelationsTool.class);

      public static final String usage =
            "relationstool action ...\n" +
                  "  help - print this message and exit.\n" +
                  "  follows fromId toId - add a new relationship where from follows to.\n" +
                  "  list follows userId - list everyone userId follows.\n" +
                  "  list followedBy userId - list everyone who follows userId.\n" +
                  "  followedByScan userId - count users' followers using a client-side scanner" +
                  "  followedByCoproc userId - count users' followers using the Endpoint coprocessor";

      public static void main(String[] args) throws Throwable {
         if (args.length == 0 || "help".equals(args[0])) {
            System.out.println(usage);
            System.exit(0);
         }

         Configuration conf = HBaseConfiguration.create();
         conf.set("hbase.zookeeper.quorum", "192.168.199.128");
         conf.set("hbase.zookeeper.property.clientPort", "2181");
         Connection conn = ConnectionFactory.createConnection(conf);
         RelationsDAO dao = new RelationsDAO(conn);

         if ("follows".equals(args[0])) {
            log.debug(String.format("Adding follower %s -> %s", args[1], args[2]));
            dao.addFollows(args[1], args[2]);
            System.out.println("Successfully added relationship");
         }

         if ("list".equals(args[0])) {
            List<Relation> results = new ArrayList<>();
            if (args[1].equals("follows")) {
               results.addAll(dao.listFollows(args[2]));
            } else if (args[1].equals("followedBy")) {
               results.addAll(dao.listFollowedBy(args[2]));
            }

            if (results.isEmpty()) {
               System.out.println("No relations found.");
            }
            for (Relation r : results) {
               System.out.println(r);
            }
         }

         if ("followedByScan".equals(args[0])) {
            long count = dao.followedByCountScan(args[1]);
            System.out.println(String.format("%s has %s followers.", args[1], count));
         }

         conn.close();
      }
   }
   ```

   使用该类往 follows 表插入数据，可以观察到 followedBy 表数据也已经添加。
   ```shell
   hbase> scan 'followedBy'                                                                             
   \xA5\xDF7]|\x97"H\x17~\x8BD\x07\xC8\x80\x8C\x1E\xD1d^\xDDpm\xC3y\xEF\xFE\x13\xF3\xED\xCA\xCF  column=f:from, timestamp=1564797728620, value=stars                                      
   \xA5\xDF7]|\x97"H\x17~\x8BD\x07\xC8\x80\x8C\x1E\xD1d^\xDDpm\xC3y\xEF\xFE\x13\xF3\xED\xCA\xCF  column=f:to, timestamp=1564797728620, value=fans  
   ```

#### 3.2.2、使用 HBash Shell 动态卸载

1. 卸载协处理器前需要先禁用表

   ```shell
   hbase >  disable 'follows'
   ```

2. 卸载协处理器

   ```shell
   hbase > alter 'follows', METHOD => 'table_att_unset', NAME => 'coprocessor$1'
   ```

3. 启用表

   ```shell
   hbase> enable 'follows'
   ```

4. 查看协处理器是否卸载成功

   ```shell
   hbase >  desc 'follows'
   ```


#### 3.2.3、通过 HBase API 动态加载

除了可以通过 HBase Shell 和 hbase-site.xml 配置文件来加载协处理器，还可以通过 Client API 来加载协处理器。

根据新老版本，有两种形式：

**1）all HBase versions**

The following Java code shows how to use the `setValue()` method of `HTableDescriptor` to load a coprocessor on the `users` table.

```java
private static void allVersion() throws IOException {
    TableName tableName = TableName.valueOf("users");
    Admin admin = con.getAdmin();

    // 1.先 disable 表
    admin.disableTable(tableName);

    // 新建一个表描述
    String path = "hdfs://master:9000/hbase/lib/hbase-coprocessor.jar";
    TableDescriptorBuilder tableDescriptorBuilder =
        TableDescriptorBuilder.newBuilder(tableName)
      .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes("f")).setMaxVersions(3).build())
        .setValue("COPROCESSOR$1", path + "|"
                  + RelationCountEndpoint.class.getCanonicalName() + "|"
                  + Coprocessor.PRIORITY_USER);
    // 2. 修改表
    admin.modifyTable(tableDescriptorBuilder.build());

    // 3. enable 表
    admin.enableTable(tableName);

    con.close();
}
```

**2）HBase 0.96+ only**

In HBase 0.96 and newer, the `addCoprocessor()` method of `HTableDescriptor` provides an easier way to load a coprocessor dynamically.

```java
private static void newVersion() throws IOException {
    TableName tableName = TableName.valueOf("users");

    Admin admin = con.getAdmin();
    // 1.先 disable 表
    admin.disableTable(tableName);

    String path = "hdfs://master:9000/hbase/lib/hbase-coprocessor.jar";
    TableDescriptorBuilder tableDescriptorBuilder =
        TableDescriptorBuilder.newBuilder(tableName)
        .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes("f")).setMaxVersions(3).build())
        .setCoprocessor(RelationCountEndpoint.class.getCanonicalName())
        .setCoprocessor(CoprocessorDescriptorBuilder.newBuilder(RelationCountEndpoint.class.getCanonicalName())
                        .setJarPath(path)
                        .setPriority(Coprocessor.PRIORITY_USER)
                        .build());

    // 2. 修改表
    admin.modifyTable(tableDescriptorBuilder.build());

    // 3. enable 表
    admin.enableTable(tableName);

    con.close();
}
```

#### 3.2.4、通过 HBase API 动态卸载

卸载其实就是重新定义表但不设置协处理器。这会删除所有表上的协处理器。

```java
TableName tableName = TableName.valueOf("users");
String path = "hdfs://<namenode>:<port>/user/<hadoop-user>/coprocessor.jar";
Configuration conf = HBaseConfiguration.create();
Connection connection = ConnectionFactory.createConnection(conf);
Admin admin = connection.getAdmin();
admin.disableTable(tableName);
HTableDescriptor hTableDescriptor = new HTableDescriptor(tableName);
HColumnDescriptor columnFamily1 = new HColumnDescriptor("personalDet");
columnFamily1.setMaxVersions(3);
hTableDescriptor.addFamily(columnFamily1);
HColumnDescriptor columnFamily2 = new HColumnDescriptor("salaryDet");
columnFamily2.setMaxVersions(3);
hTableDescriptor.addFamily(columnFamily2);
admin.modifyTable(tableName, hTableDescriptor);
admin.enableTable(tableName);
```

In HBase 0.96 and newer, you can instead use the `removeCoprocessor()` method of the `HTableDescriptor` class.

## 四、参考资料

[Apache HBase ™ Reference Guide](https://hbase.apache.org/book.html#cp_loading)

