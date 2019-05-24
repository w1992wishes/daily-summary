## ZooKeeper 数据结构

![](../../images/zk%20数据结构图.png)

1. 每个子目录项如 NameService 都被称作为 znode，这个 znode 是被它所在的路径唯一标识，如 Server1 这个 znode 的标识为 /NameService/Server1；
2. znode 可以有子节点目录，并且每个 znode 可以存储数据，注意 EPHEMERAL 类型的目录节点不能有子节点目录；
3. znode 是有版本的，每个 znode 中存储的数据可以有多个版本，也就是一个访问路径中可以存储多份数据；
4. znode 可以是临时节点，一旦创建这个 znode 的客户端与服务器失去联系，这个 znode 也将自动删除，Zookeeper 的客户端和服务器通信采用长连接方式，每个客户端和服务器通过心跳来保持连接，这个连接状态称为 session，如果 znode 是临时节点，这个 session 失效，znode 也就删除了；
5. znode 的目录名可以自动编号，如 App1 已经存在，再创建的话，将会自动命名为 App2；
6. znode 可以被监控，包括这个目录节点中存储的数据的修改，子节点目录的变化等，一旦变化可以通知设置监控的客户端，这个是 Zookeeper 的核心特性，Zookeeper 的很多功能都是基于这个特性实现的。

### Zookeeper 节点

和文件系统一样，我们能够自由的增加、删除 znode,在 znode 下增加、删除子 znode,唯一不同的在于 znode 是可以存储数据的。

有4种类型的 znode：

1. PERSISTENT--持久化目录节点：客户端与zookeeper断开连接后，该节点依旧存在
2. PERSISTENT_SEQUENTIAL-持久化顺序编号目录节点：客户端与zookeeper断开连接后，该节点依旧存在，只是Zookeeper给该节点名称进行顺序编号
3. EPHEMERAL-临时目录节点：客户端与zookeeper断开连接后，该节点被删除
4. EPHEMERAL_SEQUENTIAL-临时顺序编号目录节点：客户端与zookeeper断开连接后，该节点被删除，只是Zookeeper给该节点名称进行顺序编号