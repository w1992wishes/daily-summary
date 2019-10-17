# 【Spark】Spark 容错及 HA--Master 异常

## 一、Master 配置 recoveryMode

Master 作为 Spark Standalone 模式中的核心，如果 Master 出现异常，则整个集群的运行情况和资源将无法进行管理，整个集群将处于“群龙无首”的状况。

Spark 在设计时考虑了这种情况，在集群运行的时候，可以启动一个或多个 Standby Master，当 Master 出现异常的时候，Standby Master 将根据一定规则确定其中一个接管 Master。

Standalone  模式中，当 Master 出现异常，Spark 有四种恢复策略，可以在配置文件 Spark-env.sh 配置项 spark.deploy.recoveryMode 进行设置，默认为 NONE。

在 Master 中，也存在如下代码：

```scala
private val RECOVERY_MODE = conf.get("spark.deploy.recoveryMode", "NONE")
```

根据 spark.deploy.recoveryMode 的配置，Master 在启动后会初始化不同的 PersistenceEngine 和 LeaderElectionAgent 用于选举新的 Master 并恢复集群状态。

Master # onStart：

```scala
val (persistenceEngine_, leaderElectionAgent_) = RECOVERY_MODE match {
  case "ZOOKEEPER" =>
    logInfo("Persisting recovery state to ZooKeeper")
    val zkFactory =
      new ZooKeeperRecoveryModeFactory(conf, serializer)
    (zkFactory.createPersistenceEngine(), zkFactory.createLeaderElectionAgent(this))
  case "FILESYSTEM" =>
    val fsFactory =
      new FileSystemRecoveryModeFactory(conf, serializer)
    (fsFactory.createPersistenceEngine(), fsFactory.createLeaderElectionAgent(this))
  case "CUSTOM" =>
    val clazz = Utils.classForName(conf.get("spark.deploy.recoveryMode.factory"))
    val factory = clazz.getConstructor(classOf[SparkConf], classOf[Serializer])
      .newInstance(conf, serializer)
      .asInstanceOf[StandaloneRecoveryModeFactory]
    (factory.createPersistenceEngine(), factory.createLeaderElectionAgent(this))
  case _ =>
    (new BlackHolePersistenceEngine(), new MonarchyLeaderAgent(this))
}
```

## 二、具体 recoveryMode

* ZOOKEEPER：集群的元数据持久化到 Zookeeper 中，当 Master 出现异常时，Zookeeper 会通过选举机制选举出新的 Master，新的 Master 接管时需要从 Zookeeper 获取持久化信息并根据这些信息恢复集群状态。
* FILESYSTEM：集群的元数据持久化到本地文件系统中，当 Master 出现异常时，只要在该机器上重新启动 Master，启动后新的 Master 获取持久化信息并根据这些信息恢复集群状态。
* CUSTOM：自定义恢复方式，对 StandaloneRecoveryModeFactory 抽象类进行实现并把该类配置到系统中，当 Master 出现异常时，会根据用户自定义的方式进行恢复集群状态。
* NONE：不持久化集群的元数据，当 Master 出现异常时，新启动的Master 不进行恢复集群状态，而是直接接管集群。

## 三、FILESYSTEM 为例

### 3.1、运行时不断更新集群状态

Master # onStart：

```scala
case "FILESYSTEM" =>
  val fsFactory =
    new FileSystemRecoveryModeFactory(conf, serializer)
  (fsFactory.createPersistenceEngine(), fsFactory.createLeaderElectionAgent(this))
```

当 Master 收到 RegisterWorker 消息后，会持久化 worker 信息：

```scala
// Master # receive # case RegisterWorker
persistenceEngine.addWorker(worker)

// PersistenceEngine # addWorker
final def addWorker(worker: WorkerInfo): Unit = {
  persist("worker_" + worker.id, worker)
}
```

Master 收到 RegisterApplication 消息后，会持久化 app 信息：

```scala
// Master # receive # case RegisterApplication 
persistenceEngine.addApplication(app)

// PersistenceEngine # addApplication
final def addApplication(app: ApplicationInfo): Unit = {
  persist("app_" + app.id, app)
}
```

还有 Driver 也是同理：

```scala
// Master # receiveAndReply # case RequestSubmitDriver 
persistenceEngine.addDriver(driver)

// PersistenceEngine # addDriver
final def addDriver(driver: DriverInfo): Unit = {
  persist("driver_" + driver.id, driver)
}
```

相反，当 remWorker、 removeApplication、removeDriver 时，会将相应的持久化信息删除。

```scala
// PersistenceEngine # removeDriver
final def removeDriver(driver: DriverInfo): Unit = {
  unpersist("driver_" + driver.id)
}

// PersistenceEngine # removeWorker
final def removeWorker(worker: WorkerInfo): Unit = {
  unpersist("worker_" + worker.id)
}

// PersistenceEngine # removeApplication
final def removeApplication(app: ApplicationInfo): Unit = {
  unpersist("app_" + app.id)
}
```

### 3.2、重新启动读取持久化文件恢复集群状态

当 Master 异常 down 掉，重新启动该 Master，在 onStart 中创建 ElectionAgent。

Master # onStart：

```scala
case "FILESYSTEM" =>
  val fsFactory =
    new FileSystemRecoveryModeFactory(conf, serializer)
  (fsFactory.createPersistenceEngine(), fsFactory.createLeaderElectionAgent(this))
```

FileSystemRecoveryModeFactory # createLeaderElectionAgent：

```scala
def createLeaderElectionAgent(master: LeaderElectable): LeaderElectionAgent = {
    new MonarchyLeaderAgent(master)
  }
}

/** Single-node implementation of LeaderElectionAgent -- we're initially and always the leader. */
private[spark] class MonarchyLeaderAgent(val masterInstance: LeaderElectable)
  extends LeaderElectionAgent {
  masterInstance.electedLeader()
}
```

接着回到 Master。

Master # electedLeader：

```scala
override def electedLeader() {
  self.send(ElectedLeader)
}
```

Master 收到 ElectedLeader 消息后：

```scala
case ElectedLeader =>
  val (storedApps, storedDrivers, storedWorkers) = persistenceEngine.readPersistedData(rpcEnv)
  state = if (storedApps.isEmpty && storedDrivers.isEmpty && storedWorkers.isEmpty) {
    RecoveryState.ALIVE
  } else {
    RecoveryState.RECOVERING
  }
  logInfo("I have been elected leader! New state: " + state)
  if (state == RecoveryState.RECOVERING) {
    beginRecovery(storedApps, storedDrivers, storedWorkers)
    recoveryCompletionTask = forwardMessageThread.schedule(new Runnable {
      override def run(): Unit = Utils.tryLogNonFatalError {
        self.send(CompleteRecovery)
      }
    }, WORKER_TIMEOUT_MS, TimeUnit.MILLISECONDS)
  }
```

集群恢复：

Master # beginRecovery：

```scala
private def beginRecovery(storedApps: Seq[ApplicationInfo], storedDrivers: Seq[DriverInfo],
    storedWorkers: Seq[WorkerInfo]) {
  for (app <- storedApps) {
    logInfo("Trying to recover app: " + app.id)
    try {
      registerApplication(app)
      app.state = ApplicationState.UNKNOWN
      app.driver.send(MasterChanged(self, masterWebUiUrl))
    } catch {
      case e: Exception => logInfo("App " + app.id + " had exception on reconnect")
    }
  }

  for (driver <- storedDrivers) {
    // Here we just read in the list of drivers. Any drivers associated with now-lost workers
    // will be re-launched when we detect that the worker is missing.
    drivers += driver
  }

  for (worker <- storedWorkers) {
    logInfo("Trying to recover worker: " + worker.id)
    try {
      registerWorker(worker)
      worker.state = WorkerState.UNKNOWN
      worker.endpoint.send(MasterChanged(self, masterWebUiUrl))
    } catch {
      case e: Exception => logInfo("Worker " + worker.id + " had exception on reconnect")
    }
  }
}
```

完成恢复后重新调度：

Master # completeRecovery：

```scala
private def completeRecovery() {
  // Ensure "only-once" recovery semantics using a short synchronization period.
  if (state != RecoveryState.RECOVERING) { return }
  state = RecoveryState.COMPLETING_RECOVERY

  // Kill off any workers and apps that didn't respond to us.
  workers.filter(_.state == WorkerState.UNKNOWN).foreach(
    removeWorker(_, "Not responding for recovery"))
  apps.filter(_.state == ApplicationState.UNKNOWN).foreach(finishApplication)

  // Update the state of recovered apps to RUNNING
  apps.filter(_.state == ApplicationState.WAITING).foreach(_.state = ApplicationState.RUNNING)

  // Reschedule drivers which were not claimed by any workers
  drivers.filter(_.worker.isEmpty).foreach { d =>
    logWarning(s"Driver ${d.id} was not found after master recovery")
    if (d.desc.supervise) {
      logWarning(s"Re-launching ${d.id}")
      relaunchDriver(d)
    } else {
      removeDriver(d.id, DriverState.ERROR, None)
      logWarning(s"Did not re-launch ${d.id} because it was not supervised")
    }
  }

  state = RecoveryState.ALIVE
  schedule()
  logInfo("Recovery complete - resuming operations!")
}
```