## 可靠地服务配置

### RetryAbleActiveKeyValueStore

作为一个整体，write() 方法是一个“幂等”操作，可以对他进行无条件重试。

其中设置了重试的最大次数 MAX_RETRIES 和两次重试之间的间隔 RETRY_PERIOD_SECONDS.

### ResilientConfigUpdater

里面没有对 KeepException.SeeionExpiredException 异常进行重试，因为一个会话过期时，ZooKeeper对象会进入CLOSED状态，
此状态下它不能进行重试连接。只能将这个异常简单抛出并让拥有者创建一个新实例，以重试整个 write() 方法。
一个简单的创建新实例的方法是创建一个新的 ResilientConfigUpdater 用于恢复过期会话。

