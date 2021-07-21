## CompletableFuture

JDK 8 中 CompletableFuture 是对 Future 的增强 大大简化了异步编程步骤,在Spring 框架中配合@EnableAsync @Async 更加事办功倍。

1:在JDK 8 之前实现多线必需实现两个接口 Runnable 不带返回值，另一个Callable带返回值的接口,配合ThreadPoolTaskExecutor.submit(Callable callable) 返回一个Future对象。

使用Future获得异步执行结果时，要么调用阻塞方法get()，要么轮询看isDone()是否为true，这两种方法都不是很好，因为主线程也会被迫等待，而CompletableFuture出现改变了这个问题，而且提供更多并且强大的其它功能。

### 1:按功能分类的话：
xxx()：表示该方法将继续在已有的线程中执行；

xxxAsync()：表示将异步在线程池中执行。

异步执行方法默认一个参数的话任务是在 ForkJoinPool.commonPool() 线程池中执行的，带executor 参数的使用 executor线程池异步执行。

### 2:按逻辑和组织方式来分话(completableFuture 中大约有50个来方法)
一种是 then 的逻辑，即前一个计算完成的时候调度后一个计算

一种是 both 的逻辑，即等待两个计算都完成之后执行下一个计算，只要能组合一个和另一个，我们就可以无限复用这个 +1 的逻辑组合任意多的计算

另一种是 either 的逻辑，即等待两个计算的其中一个完成之后执行下一个计算。注意这样的计算可以说是非确定性的。因为被组合的两个计算中先触发下一个计算执行的那个会被作为前一个计算，而这两个前置的计算到底哪一个先完成是不可预知的
### 3:从依赖关系和出入参数类型区别，基本分为三类：
apply 字样的方式意味着组合方式是 Function，即接受前一个计算的结果，应用函数之后返回一个新的结果

accept 字样的方式意味着组合方式是 Consumer，即接受前一个计算的结果，执行消费后不返回有意义的值

run 字样的方式意味着组合方式是 Runnable，即忽略前一个计算的结果，仅等待它完成后执行动作