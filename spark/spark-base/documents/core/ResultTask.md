# 【Spark】 Spark作业执行原理--获取执行结果

[TOC]

## 一、执行结果并序列化

任务执行完成后，是在 TaskRunner 的 run 方法的后半部分返回结果给 Driver 的：

```scala
override def run(): Unit = {
    ...
    // 执行任务
    val value = try {
      val res = task.run(
        taskAttemptId = taskId,
        attemptNumber = attemptNumber,
        metricsSystem = env.metricsSystem)
      threwException = false
      res
    } 
    ...
    val taskFinish = System.currentTimeMillis()
    val taskFinishCpu = if (threadMXBean.isCurrentThreadCpuTimeSupported) {
      threadMXBean.getCurrentThreadCpuTime
    } else 0L

    // If the task has been killed, let's fail it.
    if (task.killed) {
      throw new TaskKilledException
    }
    
	// 序列化结果
    val resultSer = env.serializer.newInstance()
    val beforeSerialization = System.currentTimeMillis()
    val valueBytes = resultSer.serialize(value)
    val afterSerialization = System.currentTimeMillis()

    // Deserialization happens in two parts: first, we deserialize a Task object, which
    // includes the Partition. Second, Task.run() deserializes the RDD and function to be run.
    task.metrics.setExecutorDeserializeTime(
      (taskStart - deserializeStartTime) + task.executorDeserializeTime)
    task.metrics.setExecutorDeserializeCpuTime(
      (taskStartCpu - deserializeStartCpuTime) + task.executorDeserializeCpuTime)
    // We need to subtract Task.run()'s deserialization time to avoid double-counting
    task.metrics.setExecutorRunTime((taskFinish - taskStart) - task.executorDeserializeTime)
    task.metrics.setExecutorCpuTime(
      (taskFinishCpu - taskStartCpu) - task.executorDeserializeCpuTime)
    task.metrics.setJvmGCTime(computeTotalGcTime() - startGCTime)
    task.metrics.setResultSerializationTime(afterSerialization - beforeSerialization)

    // 序列化后的结果封装成 DirectTaskResult
    // Note: accumulator updates must be collected after TaskMetrics is updated
    val accumUpdates = task.collectAccumulatorUpdates()
    // TODO: do not serialize value twice
    val directResult = new DirectTaskResult(valueBytes, accumUpdates)
    val serializedDirectResult = ser.serialize(directResult)
    val resultSize = serializedDirectResult.limit

    // directSend = sending directly back to the driver
    val serializedResult: ByteBuffer = {
      // 生成结果大于最大值（默认1GB）直接丢弃
      if (maxResultSize > 0 && resultSize > maxResultSize) {
        logWarning(s"Finished $taskName (TID $taskId). Result is larger than maxResultSize " +
          s"(${Utils.bytesToString(resultSize)} > ${Utils.bytesToString(maxResultSize)}), " +
          s"dropping it.")
        ser.serialize(new IndirectTaskResult[Any](TaskResultBlockId(taskId), resultSize))
      // 生成结果设置的 maxDirectResultSize 且小于 最大值，则存放到 BlockManager 中，然后返回 BlockManager 的编号
      } else if (resultSize > maxDirectResultSize) {
        val blockId = TaskResultBlockId(taskId)
        env.blockManager.putBytes(
          blockId,
          new ChunkedByteBuffer(serializedDirectResult.duplicate()),
          StorageLevel.MEMORY_AND_DISK_SER)
        logInfo(
          s"Finished $taskName (TID $taskId). $resultSize bytes result sent via BlockManager)")
        ser.serialize(new IndirectTaskResult[Any](blockId, resultSize))
      // 其他结果直接返回
      } else {
        logInfo(s"Finished $taskName (TID $taskId). $resultSize bytes result sent to driver")
        serializedDirectResult
      }
    }
	// 向 Driver 终端发送任务运行完毕的消息
    execBackend.statusUpdate(taskId, TaskState.FINISHED, serializedResult)
```

从上面可以看出，对于 Executor 的计算结果，会根据结果大小不同有不同策略。

（1）生成结果大于maxResultSize（ 默认 1GB），结果直接丢弃，可以通过 spark.driver.maxResultSize 进行设置；

（2）生成结果大小大于 maxDirectResultSize（默认128M），小于  maxResultSize（ 默认 1GB），将结果存入 BlockManager，并返回其编号，通过 Netty 发送给 Driver，maxDirectResultSize 由 spark.task.maxDirectResultSiz 和 spark.rpc.message.maxSize 控制，取两个中的最小值。

（3）生成结果小于 maxDirectResultSize（默认128M），则直接发送给 Driver。

## 二、发送执行结果

任务执行后，TaskRunner 将执行结果发送给 DriverEndpoint 终端：

```scala
override def statusUpdate(taskId: Long, state: TaskState, data: ByteBuffer) {
  val msg = StatusUpdate(executorId, taskId, state, data)
  driver match {
    case Some(driverRef) => driverRef.send(msg)
    case None => logWarning(s"Drop $msg because has not yet connected to driver")
  }
}
```

## 三、获取执行结果

在 statusUpdate 中，将转给 TaskScheduler 处理：

```scala
case StatusUpdate(executorId, taskId, state, data) =>
  scheduler.statusUpdate(taskId, state, data.value)
  if (TaskState.isFinished(state)) {
    executorDataMap.get(executorId) match {
      case Some(executorInfo) =>
        executorInfo.freeCores += scheduler.CPUS_PER_TASK
        makeOffers(executorId)
      case None =>
        // Ignoring the update since we don't know about the executor.
        logWarning(s"Ignored task status update ($taskId state $state) " +
          s"from unknown executor with ID $executorId")
    }
  }
```

TaskScheduler 中对任务的不同状态有不同处理：

```scala
case Some(taskSet) =>
  if (state == TaskState.LOST) {
    // TaskState.LOST is only used by the deprecated Mesos fine-grained scheduling mode,
    // where each executor corresponds to a single task, so mark the executor as failed.
    val execId = taskIdToExecutorId.getOrElse(tid, throw new IllegalStateException(
      "taskIdToTaskSetManager.contains(tid) <=> taskIdToExecutorId.contains(tid)"))
    if (executorIdToRunningTaskIds.contains(execId)) {
      reason = Some(
        SlaveLost(s"Task $tid was lost, so marking the executor as lost as well."))
      removeExecutor(execId, reason.get)
      failedExecutor = Some(execId)
    }
  }
  if (TaskState.isFinished(state)) {
    cleanupTaskState(tid)
    taskSet.removeRunningTask(tid)
    if (state == TaskState.FINISHED) {
      taskResultGetter.enqueueSuccessfulTask(taskSet, tid, serializedData)
    } else if (Set(TaskState.FAILED, TaskState.KILLED, TaskState.LOST).contains(state)) {
      taskResultGetter.enqueueFailedTask(taskSet, tid, state, serializedData)
    }
  }
```

### 3.1、TaskState.FINISHED

如果 TaskState.FINISHED，则进入 taskResultGetter.enqueueSuccessfulTask(taskSet, tid, serializedData)：

```scala
def enqueueSuccessfulTask(
    taskSetManager: TaskSetManager,
    tid: Long,
    serializedData: ByteBuffer): Unit = {
  getTaskResultExecutor.execute(new Runnable {
    override def run(): Unit = Utils.logUncaughtExceptions {
      try {
        val (result, size) = serializer.get().deserialize[TaskResult[_]](serializedData) match {
          case directResult: DirectTaskResult[_] =>
            if (!taskSetManager.canFetchMoreResults(serializedData.limit())) {
              return
            }
            // deserialize "value" without holding any lock so that it won't block other threads.
            // We should call it here, so that when it's called again in
            // "TaskSetManager.handleSuccessfulTask", it does not need to deserialize the value.
            directResult.value(taskResultSerializer.get())
            (directResult, serializedData.limit())
          case IndirectTaskResult(blockId, size) =>
            if (!taskSetManager.canFetchMoreResults(size)) {
              // dropped by executor if size is larger than maxResultSize
              sparkEnv.blockManager.master.removeBlock(blockId)
              return
            }
            logDebug("Fetching indirect task result for TID %s".format(tid))
            scheduler.handleTaskGettingResult(taskSetManager, tid)
            val serializedTaskResult = sparkEnv.blockManager.getRemoteBytes(blockId)
            if (!serializedTaskResult.isDefined) {
              /* We won't be able to get the task result if the machine that ran the task failed
               * between when the task ended and when we tried to fetch the result, or if the
               * block manager had to flush the result. */
              scheduler.handleFailedTask(
                taskSetManager, tid, TaskState.FINISHED, TaskResultLost)
              return
            }
            val deserializedResult = serializer.get().deserialize[DirectTaskResult[_]](
              serializedTaskResult.get.toByteBuffer)
            // force deserialization of referenced value
            deserializedResult.value(taskResultSerializer.get())
            sparkEnv.blockManager.master.removeBlock(blockId)
            (deserializedResult, size)
        }

        // Set the task result size in the accumulator updates received from the executors.
        // We need to do this here on the driver because if we did this on the executors then
        // we would have to serialize the result again after updating the size.
        result.accumUpdates = result.accumUpdates.map { a =>
          if (a.name == Some(InternalAccumulator.RESULT_SIZE)) {
            val acc = a.asInstanceOf[LongAccumulator]
            assert(acc.sum == 0L, "task result size should not have been set on the executors")
            acc.setValue(size.toLong)
            acc
          } else {
            a
          }
        }

        scheduler.handleSuccessfulTask(taskSetManager, tid, result)
      } catch {
        case cnf: ClassNotFoundException =>
          val loader = Thread.currentThread.getContextClassLoader
          taskSetManager.abort("ClassNotFound with classloader: " + loader)
        // Matching NonFatal so we don't catch the ControlThrowable from the "return" above.
        case NonFatal(ex) =>
          logError("Exception while getting task result", ex)
          taskSetManager.abort("Exception while getting task result: %s".format(ex))
      }
    }
  })
}
```

enqueueSuccessfulTask 方法中判断如果结果是 DirectTaskResult 类型，就直接获取，如果是 IndirectTaskResult 类型，则根据 blockId 远程调用 sparkEnv.blockManager.getRemoteBytes(blockId) 获取；

接着调用 scheduler.handleSuccessfulTask:

```scala
def handleSuccessfulTask(
    taskSetManager: TaskSetManager,
    tid: Long,
    taskResult: DirectTaskResult[_]): Unit = synchronized {
  taskSetManager.handleSuccessfulTask(tid, taskResult)
}
```

最终经过调用链会来到 DAGScheduler # handleTaskCompletion 中，在该方法中，如果 Task 是 ResultTask，判断作业 是否完成，如果完成，标记完成，并清理作业依赖的资源，发送消息给消息总线。

```scala
case Success =>
  stage.pendingPartitions -= task.partitionId
  task match {
    case rt: ResultTask[_, _] =>
      // Cast to ResultStage here because it's part of the ResultTask
      // TODO Refactor this out to a function that accepts a ResultStage
      val resultStage = stage.asInstanceOf[ResultStage]
      resultStage.activeJob match {
        case Some(job) =>
          if (!job.finished(rt.outputId)) {
            updateAccumulators(event)
            job.finished(rt.outputId) = true
            job.numFinished += 1
            // If the whole job has finished, remove it
            if (job.numFinished == job.numPartitions) {
              markStageAsFinished(resultStage)
              cleanupStateForJobAndIndependentStages(job)
              listenerBus.post(
                SparkListenerJobEnd(job.jobId, clock.getTimeMillis(), JobSucceeded))
            }

            // taskSucceeded runs some user code that might throw an exception. Make sure
            // we are resilient against that.
            try {
              job.listener.taskSucceeded(rt.outputId, event.result)
            } catch {
              case e: Exception =>
                // TODO: Perhaps we want to mark the resultStage as failed?
                job.listener.jobFailed(new SparkDriverExecutionException(e))
            }
          }
        case None =>
          logInfo("Ignoring result from " + rt + " because its job has finished")
      }
```

如果是 ShuffleMapTask，则将结果（MapStatus）序列化后存入 DirectTaskResult 或者 IndirectTaskResult 中，DAGScheduler 的 handleTaskCompletion 获取这个结果，并注册到  MapOutputTrackerMaster 中：

```scala
  case smt: ShuffleMapTask =>
    val shuffleStage = stage.asInstanceOf[ShuffleMapStage]
    updateAccumulators(event)
    val status = event.result.asInstanceOf[MapStatus]
    val execId = status.location.executorId
    logDebug("ShuffleMapTask finished on " + execId)
    if (failedEpoch.contains(execId) && smt.epoch <= failedEpoch(execId)) {
      logInfo(s"Ignoring possibly bogus $smt completion from executor $execId")
    } else {
      shuffleStage.addOutputLoc(smt.partitionId, status)
    }

    if (runningStages.contains(shuffleStage) && shuffleStage.pendingPartitions.isEmpty) {
      markStageAsFinished(shuffleStage)
      logInfo("looking for newly runnable stages")
      logInfo("running: " + runningStages)
      logInfo("waiting: " + waitingStages)
      logInfo("failed: " + failedStages)

      // We supply true to increment the epoch number here in case this is a
      // recomputation of the map outputs. In that case, some nodes may have cached
      // locations with holes (from when we detected the error) and will need the
      // epoch incremented to refetch them.
      // TODO: Only increment the epoch number if this is not the first time
      //       we registered these map outputs.
      mapOutputTracker.registerMapOutputs(
        shuffleStage.shuffleDep.shuffleId,
        shuffleStage.outputLocInMapOutputTrackerFormat(),
        changeEpoch = true)

      clearCacheLocs()

      if (!shuffleStage.isAvailable) {
        // Some tasks had failed; let's resubmit this shuffleStage
        // TODO: Lower-level scheduler should also deal with this
        logInfo("Resubmitting " + shuffleStage + " (" + shuffleStage.name +
          ") because some of its tasks had failed: " +
          shuffleStage.findMissingPartitions().mkString(", "))
        submitStage(shuffleStage)
      } else {
        // Mark any map-stage jobs waiting on this stage as finished
        if (shuffleStage.mapStageJobs.nonEmpty) {
          val stats = mapOutputTracker.getStatistics(shuffleStage.shuffleDep)
          for (job <- shuffleStage.mapStageJobs) {
            markMapStageJobAsFinished(job, stats)
          }
        }
        submitWaitingChildStages(shuffleStage)
      }
    }
}
```

### 3.2、TaskState.FAILED, TaskState.KILLED, TaskState.LOST

如果结果类型 TaskState.FAILED, TaskState.KILLED, TaskState.LOST，则进入 taskResultGetter.enqueueFailedTask(taskSet, tid, state, serializedData)：

```scala
def enqueueFailedTask(taskSetManager: TaskSetManager, tid: Long, taskState: TaskState,
  serializedData: ByteBuffer) {
  var reason : TaskFailedReason = UnknownReason
  try {
    getTaskResultExecutor.execute(new Runnable {
      override def run(): Unit = Utils.logUncaughtExceptions {
        val loader = Utils.getContextOrSparkClassLoader
        try {
          if (serializedData != null && serializedData.limit() > 0) {
            reason = serializer.get().deserialize[TaskFailedReason](
              serializedData, loader)
          }
        } catch {
          case cnd: ClassNotFoundException =>
            // Log an error but keep going here -- the task failed, so not catastrophic
            // if we can't deserialize the reason.
            logError(
              "Could not deserialize TaskEndReason: ClassNotFound with classloader " + loader)
          case ex: Exception => // No-op
        }
        scheduler.handleFailedTask(taskSetManager, tid, taskState, reason)
      }
    })
  } catch {
    case e: RejectedExecutionException if sparkEnv.isStopped =>
      // ignore it
  }
}
```

然后再调用 scheduler.handleFailedTask 重新分配资源重试：

```scala
def handleFailedTask(
    taskSetManager: TaskSetManager,
    tid: Long,
    taskState: TaskState,
    reason: TaskFailedReason): Unit = synchronized {
  taskSetManager.handleFailedTask(tid, taskState, reason)
  if (!taskSetManager.isZombie && taskState != TaskState.KILLED) {
    // Need to revive offers again now that the task set manager state has been updated to
    // reflect failed tasks that need to be re-run.
    backend.reviveOffers()
  }
}
```