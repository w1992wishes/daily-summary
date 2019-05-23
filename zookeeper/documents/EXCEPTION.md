## ZooKeeper 异常

在 Java API 中的每一个 ZooKeeper 操作都在其 throws 子句中声明了两种类型的异常，分别是 InterruptedException 和 KeeperException。

（一）InterruptedException 异常

如果操作被中断，则会有一个 InterruptedException 异常被抛出。在 Java 语言中有一个取消阻塞方法的标准机制，即针对存在阻塞方法的线程调用 interrupt()。
一个成功的取消操作将产生一个 InterruptedException 异常。

ZooKeeper 也遵循这一机制，因此你可以使用这种方法来取消一个 ZooKeeper 操作。使用了 ZooKeeper 的类或库通常会传播 InterruptedException 异常，使客户端能够取消它们的操作。
InterruptedException 异常并不意味着有故障，而是表明相应的操作已经被取消，所以在配置服务的示例中，可以通过传播异常来中止应用程序的运行。

（二）KeeperException异常

(1) 如果 ZooKeeper 服务器发出一个错误信号或与服务器存在通信问题，抛出的则是 KeeperException 异常。

**针对不同的错误情况，KeeperException 异常存在不同的子类。**

例如:　KeeperException.NoNodeException 是 KeeperException 的一个子类，如果你试图针对一个不存在的 znode 执行操作，抛出的则是该异常。

**每一个 KeeperException 异常的子类都对应一个关于错误类型信息的代码。**

例如:　KeeperException.NoNodeException 异常的代码是 KeeperException.Code.NONODE

**有两种方法被用来处理 KeeperException 异常。**

1. 捕捉 KeeperException 异常，并且通过检测它的代码来决定采取何种补救措施；
2. 另一种是捕捉等价的 KeeperException 子类，并且在每段捕捉代码中执行相应的操作。

(3) KeeperException 异常分为三大类

* 状态异常 

当一个操作因不能被应用于 znode 树而导致失败时，就会出现状态异常。状态异常产生的原因通常是在同一时间有另外一个进程正在修改znode。
例如，如果一个 znode 先被另外一个进程更新了，根据版本号执行 setData 操作的进程就会失败，并收到一个 KeeperException.BadVersionException 异常，
这是因为版本号不匹配。程序员通常都知道这种冲突总是存在的，也都会编写代码来进行处理。

一些状态异常会指出程序中的错误，例如 KeeperException.NoChildrenForEphemeralsException 异常，试图在短暂 znode 下创建子节点时就会抛出该异常。

* 可恢复异常

可恢复的异常是指那些应用程序能够在同一个 ZooKeeper 会话中恢复的异常。一个可恢复的异常是通过 KeeperException.ConnectionLossException 来表示的，
它意味着已经丢失了与 ZooKeeper 的连接。ZooKeeper 会尝试重新连接，并且在大多数情况下重新连接会成功，并确保会话是完整的。

但是 ZooKeeper 不能判断与 KeeperException.ConnectionLossException 异常相关的操作是否成功执行。这种情况就是部分失败的一个例子。
这时程序员有责任来解决这种不确定性，并且根据应用的情况来采取适当的操作。在这一点上，就需要对“幂等”(idempotent)操作和“非幂等”(Nonidempotent)操作进行区分。
幂等操作是指那些一次或多次执行都会产生相同结果的操作，例如读请求或无条件执行的 setData 操作。对于幂等操作，只需要简单地进行重试即可。对于非幂等操作，就不能盲目地进行重试，
因为它们多次执行的结果与一次执行是完全不同的。程序可以通过在 znode 的路径和它的数据中编码信息来检测是否非幂等操怍的更新已经完成。

* 不可恢复的异常 

在某些情况下，ZooKeeper 会话会失效——也许因为超时或因为会话被关闭，两种情况下都会收到 KeeperException.SessionExpiredException 异常，
或因为身份验证失败，KeeperException.AuthFailedException 异常。无论上述哪种情况，所有与会话相关联的短暂 znode 都将丢失，
因此应用程序需要在重新连接到 ZooKeeper 之前重建它的状态。
