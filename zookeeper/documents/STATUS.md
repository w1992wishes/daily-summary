## ZooKeeper 实例状态

#### ZooKeeper 状态

ZooKeeper 对象在其生命周期中会经历几种不同的状态。可以在任何时刻通过 getState() 方法来查询对象的状态：

public States getState()

States 被定义成代表 ZooKeeper 对象不同状态的枚举类型值（不管是什么枚举值，一个 ZooKeeper 的实例在一个时刻只能处于一种状态）。
在试图与 ZooKeeper 服务建立连接的过程中，一个新建的 ZooKeeper 实例处于 CONNECTING 状态。一旦建立连接，它就会进入 CONNECTED 状态。 

![](../../images/zk/zk%20状态转换.png)

通过注册观察对象，使用了 ZooKeeper 对象的客户端可以收到状态转换通知。在进入 CONNECTED 状态时，观察对象会收到一个 WatchedEvent 通知，
其中 KeeperState 的值是 SyncConnected。

### Watch 与 ZooKeeper 状态

ZooKeeper的观察对象肩负着双重责任：
1. 可以用来获得 ZooKeeper 状态变化的相关通知；
2. 可以用来获得 Znode 变化的相关通知。

监视 ZooKeeper 状态变化：可以使用 ZooKeeper 对象默认构造函数的观察。

监视 Znode 变化：可以使用一个专用的观察对象，将其传递给适当的读操作。也可以通过读操作中的布尔标识来设定是否共享使用默认的观察。

ZooKeeper 实例可能失去或重新连接 ZooKeeper 服务，在 CONNECTED 和 CONNECTING 状态中切换。如果连接断开，watcher 得到一个 Disconnected 事件。
要注意的是，这些状态的迁移是由 ZooKeeper 实例自己发起的，如果连接断开他将自动尝试自动连接。

如果任何一个 close() 方法被调用，或是会话由 Expired 类型的 KeepState 提示过期时，ZooKeeper 可能会转变成第三种状态 CLOSED。一旦处于 CLOSED 状态，
ZooKeeper 对象将不再是活动的了(可以使用 states 的 isActive() 方法进行测试)，而且不能被重用。客户端必须建立一个新的 ZooKeeper 实例才能重新连接到 ZooKeeper 服务。

