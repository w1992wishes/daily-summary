# 【Spark】 Spark作业执行原理--Spark执行任务

[TOC]

## 一、CoarseGrainedExecutorBackend 接收 LaunchTask 消息

CoarseGrainedSchedulerBackend 中向 Executor 发送 LaunchTask 消息，CoarseGrainedExecutorBackend 接收该消息：

```scala
case LaunchTask(data) =>
  if (executor == null) {
    exitExecutor(1, "Received LaunchTask command but executor was null")
  } else {
    val taskDesc = ser.deserialize[TaskDescription](data.value)
    logInfo("Got assigned task " + taskDesc.taskId)
    executor.launchTask(this, taskId = taskDesc.taskId, attemptNumber = taskDesc.attemptNumber,
      taskDesc.name, taskDesc.serializedTask)
  }
```

## 二、Executor 执行 launchTask

CoarseGrainedExecutorBackend 接收到消息后，先对 TaskDescription 进行反序列化，然后 调用 executor.launchTask：

```scala
def launchTask(
    context: ExecutorBackend,
    taskId: Long,
    attemptNumber: Int,
    taskName: String,
    serializedTask: ByteBuffer): Unit = {
  val tr = new TaskRunner(context, taskId = taskId, attemptNumber = attemptNumber, taskName,
    serializedTask)
  runningTasks.put(taskId, tr)
  threadPool.execute(tr)
}
```

在 Executor 的 launchTask 方法中，会构建一个  TaskRunner 来封装任务，再把 TaskRunner 放到线程池去执行。

在 TaskRunner 的 run 方法中，要对发送过来的 task 及其依赖的 jar 等文件进行反序列化，然后对反序列化后的 Task 调用 run 方法：

```scala
override def run(): Unit = {
  val threadMXBean = ManagementFactory.getThreadMXBean
  // 生成内存管理 TaskMemoryManager 实例，用于管理运行期间内存
  val taskMemoryManager = new TaskMemoryManager(env.memoryManager, taskId)
  val deserializeStartTime = System.currentTimeMillis()
  val deserializeStartCpuTime = if (threadMXBean.isCurrentThreadCpuTimeSupported) {
    threadMXBean.getCurrentThreadCpuTime
  } else 0L
  Thread.currentThread.setContextClassLoader(replClassLoader)
  val ser = env.closureSerializer.newInstance()
  logInfo(s"Running $taskName (TID $taskId)")
  // 向 Driver 终端发送任务开始的消息
  execBackend.statusUpdate(taskId, TaskState.RUNNING, EMPTY_BYTE_BUFFER)
  var taskStart: Long = 0
  var taskStartCpu: Long = 0
  startGCTime = computeTotalGcTime()

  try {
    // 对任务开始需要的 文件、jar包、代码进行反序列化
    val (taskFiles, taskJars, taskProps, taskBytes) =
      Task.deserializeWithDependencies(serializedTask)

    // Must be set before updateDependencies() is called, in case fetching dependencies
    // requires access to properties contained within (e.g. for access control).
    Executor.taskDeserializationProps.set(taskProps)

    updateDependencies(taskFiles, taskJars)
    task = ser.deserialize[Task[Any]](taskBytes, Thread.currentThread.getContextClassLoader)
    task.localProperties = taskProps
    task.setTaskMemoryManager(taskMemoryManager)

    // If this task has been killed before we deserialized it, let's quit now. Otherwise,
    // continue executing the task.
    if (killed) {
      // Throw an exception rather than returning, because returning within a try{} block
      // causes a NonLocalReturnControl exception to be thrown. The NonLocalReturnControl
      // exception will be caught by the catch block, leading to an incorrect ExceptionFailure
      // for the task.
      throw new TaskKilledException
    }

    logDebug("Task " + taskId + "'s epoch is " + task.epoch)
    env.mapOutputTracker.updateEpoch(task.epoch)

    // Run the actual task and measure its runtime.
    taskStart = System.currentTimeMillis()
    taskStartCpu = if (threadMXBean.isCurrentThreadCpuTimeSupported) {
      threadMXBean.getCurrentThreadCpuTime
    } else 0L
    var threwException = true
    // 调用 task 的 run 方法，task 是个抽象类，在其 run 模板方法中会调用子类的 runTask 方法
    val value = try {
      val res = task.run(
        taskAttemptId = taskId,
        attemptNumber = attemptNumber,
        metricsSystem = env.metricsSystem)
      threwException = false
      res
    } finally {
      ...
    }
  }
```

## 三、执行 Task 的 run 方法 

Task 的模板方法 run：

```scala
/**
 * Called by [[org.apache.spark.executor.Executor]] to run this task.
 *
 * @param taskAttemptId an identifier for this task attempt that is unique within a SparkContext.
 * @param attemptNumber how many times this task has been attempted (0 for the first attempt)
 * @return the result of the task along with updates of Accumulators.
 */
final def run(
    taskAttemptId: Long,
    attemptNumber: Int,
    metricsSystem: MetricsSystem): T = {
  SparkEnv.get.blockManager.registerTask(taskAttemptId)
  context = new TaskContextImpl(
    stageId,
    partitionId,
    taskAttemptId,
    attemptNumber,
    taskMemoryManager,
    localProperties,
    metricsSystem,
    metrics)
  TaskContext.setTaskContext(context)
  taskThread = Thread.currentThread()

  if (_killed) {
    kill(interruptThread = false)
  }

  new CallerContext("TASK", appId, appAttemptId, jobId, Option(stageId), Option(stageAttemptId),
    Option(taskAttemptId), Option(attemptNumber)).setCurrentContext()

  try {
    runTask(context)
  } catch {
    case e: Throwable =>
      // Catch all errors; run task failure callbacks, and rethrow the exception.
      try {
        context.markTaskFailed(e)
      } catch {
        case t: Throwable =>
          e.addSuppressed(t)
      }
      throw e
  } finally {
    // Call the task completion callbacks.
    context.markTaskCompleted()
    try {
      Utils.tryLogNonFatalError {
        // Release memory used by this thread for unrolling blocks
        SparkEnv.get.blockManager.memoryStore.releaseUnrollMemoryForThisTask(MemoryMode.ON_HEAP)
        SparkEnv.get.blockManager.memoryStore.releaseUnrollMemoryForThisTask(MemoryMode.OFF_HEAP)
        // Notify any tasks waiting for execution memory to be freed to wake up and try to
        // acquire memory again. This makes impossible the scenario where a task sleeps forever
        // because there are no other tasks left to notify it. Since this is safe to do but may
        // not be strictly necessary, we should revisit whether we can remove this in the future.
        val memoryManager = SparkEnv.get.memoryManager
        memoryManager.synchronized { memoryManager.notifyAll() }
      }
    } finally {
      TaskContext.unset()
    }
  }
}
```

runTask 的实现有两种，一种是 ShuffleMapTask  的实现，一种是 ResultTask。

对于 ShuffleMapTask ，它的计算结果会写到 BlockManage，最终返回给 DAGScheduler 的是一个 MapStatus 对象，该对象中管理了 ShuffleMapTask 的运算结果存储到 BlockManager 里的相关存储信息，而不是计算结果本身，这些信息会成为下一阶段的任务需要获得的输入数据时的依据:

```scala
override def runTask(context: TaskContext): MapStatus = {
  // Deserialize the RDD using the broadcast variable.
  val threadMXBean = ManagementFactory.getThreadMXBean
  val deserializeStartTime = System.currentTimeMillis()
  val deserializeStartCpuTime = if (threadMXBean.isCurrentThreadCpuTimeSupported) {
    threadMXBean.getCurrentThreadCpuTime
  } else 0L
  val ser = SparkEnv.get.closureSerializer.newInstance()
  // 反序列化获取 RDD 和 RDD 的依赖
  val (rdd, dep) = ser.deserialize[(RDD[_], ShuffleDependency[_, _, _])](
    ByteBuffer.wrap(taskBinary.value), Thread.currentThread.getContextClassLoader)
  _executorDeserializeTime = System.currentTimeMillis() - deserializeStartTime
  _executorDeserializeCpuTime = if (threadMXBean.isCurrentThreadCpuTimeSupported) {
    threadMXBean.getCurrentThreadCpuTime - deserializeStartCpuTime
  } else 0L

  var writer: ShuffleWriter[Any, Any] = null
  try {
    val manager = SparkEnv.get.shuffleManager
    writer = manager.getWriter[Any, Any](dep.shuffleHandle, partitionId, context)
    // 首先调用 rdd.iterator，如果该 RDD 已经 Cache 或者 Checkpoint，那么直接读取结果，
    // 否则计算，计算结果会保存在本地系统的 BlockManager 中
    writer.write(rdd.iterator(partition, context).asInstanceOf[Iterator[_ <: Product2[Any, Any]]])
    // 关闭 writer, 返回计算结果，返回包含了数据的 location 和 size 等元数据信息的 MapStatus  
    writer.stop(success = true).get
  } catch {
    case e: Exception =>
      try {
        if (writer != null) {
          writer.stop(success = false)
        }
      } catch {
        case e: Exception =>
          log.debug("Could not stop writer", e)
      }
      throw e
  }
}
```

看看计算的过程：

```scala
/**
 * Internal method to this RDD; will read from cache if applicable, or otherwise compute it.
 * This should ''not'' be called by users directly, but is available for implementors of custom
 * subclasses of RDD.
 */
final def iterator(split: Partition, context: TaskContext): Iterator[T] = {
  if (storageLevel != StorageLevel.NONE) {
    getOrCompute(split, context)
  } else {
    computeOrReadCheckpoint(split, context)
  }
}
```

->

```scala
/**
 * Compute an RDD partition or read it from a checkpoint if the RDD is checkpointing.
 */
private[spark] def computeOrReadCheckpoint(split: Partition, context: TaskContext): Iterator[T] =
{
  if (isCheckpointedAndMaterialized) {
    firstParent[T].iterator(split, context)
  } else {
    compute(split, context)
  }
}
```

compute 的实现有很多，这里以 MapPartitionsRDD 为例：

```scala
override def compute(split: Partition, context: TaskContext): Iterator[U] =
  f(context, split.index, firstParent[T].iterator(split, context))
```

再看 ResultTask 的 runTask：

```scala
override def runTask(context: TaskContext): U = {
  // Deserialize the RDD and the func using the broadcast variables.
  val threadMXBean = ManagementFactory.getThreadMXBean
  val deserializeStartTime = System.currentTimeMillis()
  val deserializeStartCpuTime = if (threadMXBean.isCurrentThreadCpuTimeSupported) {
    threadMXBean.getCurrentThreadCpuTime
  } else 0L
  val ser = SparkEnv.get.closureSerializer.newInstance()
  val (rdd, func) = ser.deserialize[(RDD[T], (TaskContext, Iterator[T]) => U)](
    ByteBuffer.wrap(taskBinary.value), Thread.currentThread.getContextClassLoader)
  _executorDeserializeTime = System.currentTimeMillis() - deserializeStartTime
  _executorDeserializeCpuTime = if (threadMXBean.isCurrentThreadCpuTimeSupported) {
    threadMXBean.getCurrentThreadCpuTime - deserializeStartCpuTime
  } else 0L

  func(context, rdd.iterator(partition, context))
}

```

也是先反序列化 RDD，然后直接返回 func 函数的计算结果。

