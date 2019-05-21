## 一、简单配置服务

配置服务是分布式应用所需要的基本服务之一，它使集群中的机器可以共享配置信息中那些公共的部分。

ZooKeeper 可以作为一个具有高可用性的配置存储器，允许分布式应用的参与者检索和更新配置文件。
使用 ZooKeeper 中的观察机制，可以建立一个活跃的配置服务，使那些感兴趣的客户端能够获得配置信息修改的通知。

通过两个假设来简化所需实现的服务（稍加修改就可以取消这两个假设）：
1. 我们唯一需要存储的配置数据是字符串，关键字是 znode 的路径，因此我们在每个 znode 上存储了一个键／值对。
2. 在任何时候只有一个客户端会执行更新操作。

### ActiveKeyValueStore

write()方法的任务是将一个关键字及其值写到 ZooKeeper。
它隐藏了创建一个新的 znode 和用一个新值更新现有 znode 之间的区别，而是使用 exists 操作来检测 znode 是否存在，然后再执行相应的操作。

read() 方法的任务是读取一个节点的配置属性。ZooKeeper 的 getData() 方法有三个参数：
1. 路径
2. 一个观察对象
3. 一个 Stat 对象，Stat 对象由 getData() 方法返回的值填充，用来将信息回传给调用者。

通过这个方法，调用者可以获得一个 znode 的数据和元数据，但在这个例子中，由于我们对元数据不感兴趣，因此将 Stat 参数设为 null。

### ConfigUpdater

ConfigUpdater 中定义了一个 ActiveKeyValueStore，它在 ConfigUpdater 的构造函数中连接到 ZooKeeper，
run() 方法永远在循环，在随机时间以随机值更新 /config znode。

### ConfigWatcher

作为配置服务的用户，ConfigWatcher 创建了一个 ActiveKeyValueStore 对象 store，
并且在启动之后通过 displayConfig() 调用了 store 的 read() 方法，显示它所读到的配置信息的初始值，
并将自身作为观察传递给 store。当节点状态发生变化时，再次通过 displayConfig() 显示配置信息，并再次将自身作为观察传递给 store。

当 ConfigUpdater 更新 znode 时， ZooKeeper 产生一个类型为 EventType.NodeDataChanged 的事件，从而触发观察。
ConfigWatcher 在它的 process() 方法中对这个事件做出反应，读取并显示配置的最新版本。

由于观察仅发送单次信号，因此每次我们调用 ActiveKeyValueStore 的 read() 方法时，都将一个新的观察告知 ZooKeeper 来确保我们可以看到将来的更新。
但是，我们还是不能保证接收到每一个更新，因为在收到观察事件通知与下一次读之间， znode 可能已经被更新过，而且可能是很多次，由于客户端在这段时间没有注册任何观察，因此不会收到通知。

对于示例中的配置服务，这不是问题，因为客户端只关心属性的最新值，最新值优先于之前的值。但是，一般情况下，这个潜在的问题是不容忽视的。



