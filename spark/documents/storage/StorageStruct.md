# 【Spark】Spark 存储原理--通信层架构分析

本篇结构：

* 概念介绍
* 初始化过程
* 相互通信

Spark 存储管理模块采用的是主从结构（Master/Slave）来实现通信层，主节点和从节点之间传输控制信息、状态信息，通信的底层还是依赖 Spark Rpc 框架。 

Master 主要负责整个应用程序在运行期间 block 元数据的管理和维护，Slave 主要负责将本地数据块的状态的汇报给 Master，而且接收 Master 传过来的执行指令，比如获取数据块状态，删除 RDD 数据块等。

## 一、概念介绍

在阅读 Spark 存储原理相关源码前，先了解下 Spark 存储相关的类。

Block 是 Spark 数据处理的时候最小单位，是物理层面的储存单位，和逻辑层面 RDD 的分区是一一对应的。

BlockManager、BlockManagerMaster 及 BlockManagerMasterEndpoint 这几个类从名字上看很相似，理解较模糊，下面简单介绍。

### 1.1、BlockManager 

顾名思义，BlockManager 被 Spark 用来管理 BlockManager 所在节点内存和磁盘中的数据块。

Driver 和 Executor 节点都会创建 BlockManager 。Driver 上的 BlockManager 不具备实际存储的能力，它记录了各个 Executor 的 BlockManager 的状态。Executor 上的 BlockManager 负责数据的读写请求，删除等操作，并向 Driver 节点注册，汇报其所管理的数据块元数据信息。

### 1.2、BlockManagerMaster

和 BlockManager 一样，BlockManagerMaster 也在 SparkEnv 中创建。

BlockManager 负责管理其所在节点的 Block 数据块，而 BlockManagerMaster 主要负责整个应用程序在运行期间 block 元数据的管理和维护，以及向从节点发送指令执行命令。

### 1.3、BlockManagerMasterEndpoint、BlockManagerSlaveEndpoint

BlockManagerMaster 并不具备通信的能力，真正通信的是 BlockManagerMasterEndpoint，BlockManagerSlaveEndpoint，它们负责通过远程消息通信的方式去管理所有节点的 BlockManager。

BlockManagerMasterEndpoint 是 Driver 的通信端点，BlockManagerSlaveEndpoint 是 Executor 的通信端点，Driver 通过 BlockManagerMaster 管理所有的 Block 数据块，向 Executor 发送操作 Block 数据块的请求是通过 BlockManagerMasterEndpoint 和 BlockManagerSlaveEndpoint 的通信实现，BlockManagerMasterEndpoint 或者 BlockManagerSlaveEndpoint 收到请求后调用 BlockManager 去实际操控 Block 数据。

## 二、初始化过程

通过 Spark-submit 启动一个应用程序后，如果是 client 模式，应用程序就运行在提交程序端，如果是 cluster 模式，应用程序由 Master 分配到 Worker 中运行，应用程序运行的进程称为 driver 端。

应用程序启动时，都会构建 SparkContext，SparkContext 中会创建 Driver 端的通信底层框架 SparkEnv，在该 SparkEnv 初始化时又会实例化 BlockManager 和BlockManagerMaster，BlockManagerMaster 实例化过程中内部创建消息通信的终端点 BlockManagerMasterEndPoint 。

在 SparkEnv 初始化时还会创建 BlockTransferService 负责网络数据传输服务。

应用程序启动后会和 Master 通信，由 Master 根据应用程序的参数与 Worker 通信启动相应数量的 Executor，Executor 启动中也会创建 SparkEnv，同样会初始化 Block 数据块管理相关的类，只是根据 Driver 和 Executor 的不同，这些类的细节有部分不同，使用上也有不同。

SparkEnv：

```scala
// 远程数据传输服务，使用 Netty 实现
val blockTransferService =
  new NettyBlockTransferService(conf, securityManager, bindAddress, advertiseAddress,
    blockManagerPort, numUsableCores)

// 创建 BlockMangerMaster，如果是 Dirver端，在 BlockMangerMaster，内部则创建终端点 BlockManagerMasterEndpoint // 如果是 Executor，则持有 BlockManagerMasterEndpoint 的引用
val blockManagerMaster = new BlockManagerMaster(registerOrLookupEndpoint(
  BlockManagerMaster.DRIVER_ENDPOINT_NAME,
  new BlockManagerMasterEndpoint(rpcEnv, isLocal, conf, listenerBus)),
  conf, isDriver)

// NB: blockManager is not valid until initialize() is called later.
// 创建 BlockManager，运行在每个节点(Driver 和 Executor)上，该节点提供接口，用于管理本地节点 Block 数据块。
val blockManager = new BlockManager(executorId, rpcEnv, blockManagerMaster,
  serializerManager, conf, memoryManager, mapOutputTracker, shuffleManager,
  blockTransferService, securityManager, numUsableCores)
```

1. 先具体看 BlockManagerMaster 的创建，该过程中会调用  registerOrLookupEndpoint 方法。

SparkEnv # registerOrLookupEndpoint：

```scala
def registerOrLookupEndpoint(
    name: String, endpointCreator: => RpcEndpoint):
  RpcEndpointRef = {
  if (isDriver) {
    logInfo("Registering " + name)
    rpcEnv.setupEndpoint(name, endpointCreator)
  } else {
    RpcUtils.makeDriverRef(name, conf, rpcEnv)
  }
}
```

RpcUtils # makeDriverRef：

```scala
/**
 * Retrieve a `RpcEndpointRef` which is located in the driver via its name.
 */
def makeDriverRef(name: String, conf: SparkConf, rpcEnv: RpcEnv): RpcEndpointRef = {
  val driverHost: String = conf.get("spark.driver.host", "localhost")
  val driverPort: Int = conf.getInt("spark.driver.port", 7077)
  Utils.checkHost(driverHost)
  rpcEnv.setupEndpointRef(RpcAddress(driverHost, driverPort), name)
}
```

**可以看到对于 Driver 端，则创建 BlockManagerMasterEndpoint 通信终端，而 Executor 则创建 BlockManagerMasterEndpoint 的引用 RpcEndpointRef。这样 Executor 就能向发起通信**

2.再看 BlockManager 的创建，其初始化时会创建 BlockManagerSlaveEndpoint 通信终端。

BlockManager：

```scala
private val slaveEndpoint = rpcEnv.setupEndpoint(
  "BlockManagerEndpoint" + BlockManager.ID_GENERATOR.next,
  new BlockManagerSlaveEndpoint(rpcEnv, this, mapOutputTracker))
```

并且在 BlockManager 实例化后调用 initialize 方法中会将 

**BlockManagerSlaveEndpoint 注册到 Driver 中，这样 Driver 端就持有 BlockManagerSlaveEndpoint  通信端点的引用。显然 Driver 端即是 BlockManagerMasterEndpoint 也是 BlockManagerSlaveEndpoint ，而 Executor 端只是 BlockManagerSlaveEndpoint 。**

BlockManager # initialize：

```scala
val idFromMaster = master.registerBlockManager(
  id,
  maxOnHeapMemory,
  maxOffHeapMemory,
  slaveEndpoint)
```

BlockManagerMaster # registerBlockManager：

```scala
/**
 * Register the BlockManager's id with the driver. The input BlockManagerId does not contain
 * topology information. This information is obtained from the master and we respond with an
 * updated BlockManagerId fleshed out with this information.
 */
def registerBlockManager(
    blockManagerId: BlockManagerId,
    maxOnHeapMemSize: Long,
    maxOffHeapMemSize: Long,
    slaveEndpoint: RpcEndpointRef): BlockManagerId = {
  logInfo(s"Registering BlockManager $blockManagerId")
  val updatedId = driverEndpoint.askSync[BlockManagerId](
    RegisterBlockManager(blockManagerId, maxOnHeapMemSize, maxOffHeapMemSize, slaveEndpoint))
  logInfo(s"Registered BlockManager $updatedId")
  updatedId
}
```

**Driver 端持有 Executor 端 BlockManagerSlaveEndpoint 引用，Executor 又持有 Driver 端 BlockManagerMasterEndpoint 引用，Driver 和 Executor 相互持有 ，可以在应用程序执行中相互通信。**

## 三、相互通信

在 BlockManagerMasterEndpoint 中有三个 HashMap 存放数据块的元数据。

```scala
// Mapping from block manager id to the block manager's information.
// 存放了 BlockMangerId 与 BlockMangerInfo 的对应，其中BlockMangerInfo 包含了 Executor 内存使用情况、数据块的使用情况、已被缓存的数据块和 Executor 终端点引用，通过该引用可以向 Execuotr 发送消息
private val blockManagerInfo = new mutable.HashMap[BlockManagerId, BlockManagerInfo]

// Mapping from executor ID to block manager ID.
// 存放了 ExecutorID 和 BlockMangerID 对应列表
private val blockManagerIdByExecutor = new mutable.HashMap[String, BlockManagerId]

// Mapping from block id to the set of block managers that have the block.
// 存放了 BlockId 和 BlockManagerId 序列对应的列表，原因在于一个数据块可能存在多个副本，保持在多个 Executor 中
private val blockLocations = new JHashMap[BlockId, mutable.HashSet[BlockManagerId]]
```

### 3.1、更新数据块元信息

**当写入、更新或删除数据完毕后，发送数据块的最新状态消息 UpdateBlockInfo 给 BlockMangerMasterEndPoint 终端点，由其更新数据块的元数据，主要更新 BlockManagerInfo 和 BlockLocations 两个列表。**

BlockManagerMasterEndpoint # receiveAndReply：

```scala
override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
  ...
  case _updateBlockInfo @
      UpdateBlockInfo(blockManagerId, blockId, storageLevel, deserializedSize, size) =>
    context.reply(updateBlockInfo(blockManagerId, blockId, storageLevel, deserializedSize, size))
    listenerBus.post(SparkListenerBlockUpdated(BlockUpdatedInfo(_updateBlockInfo)))
    
  ...
```

BlockManagerMasterEndpoint  # updateBlockInfo：

```scala
private def updateBlockInfo(
    blockManagerId: BlockManagerId,
    blockId: BlockId,
    storageLevel: StorageLevel,
    memSize: Long,
    diskSize: Long): Boolean = {

  if (!blockManagerInfo.contains(blockManagerId)) {
    if (blockManagerId.isDriver && !isLocal) {
      // We intentionally do not register the master (except in local mode),
      // so we should not indicate failure.
      return true
    } else {
      return false
    }
  }

  if (blockId == null) {
    blockManagerInfo(blockManagerId).updateLastSeenMs()
    return true
  }

  blockManagerInfo(blockManagerId).updateBlockInfo(blockId, storageLevel, memSize, diskSize)

  var locations: mutable.HashSet[BlockManagerId] = null
  if (blockLocations.containsKey(blockId)) {
    locations = blockLocations.get(blockId)
  } else {
    locations = new mutable.HashSet[BlockManagerId]
    blockLocations.put(blockId, locations)
  }

  if (storageLevel.isValid) {
    locations.add(blockManagerId)
  } else {
    locations.remove(blockManagerId)
  }

  // Remove the block from master tracking if it has been removed on all slaves.
  if (locations.size == 0) {
    blockLocations.remove(blockId)
  }
  true
}
```

- 在处理 BokcMangerInfo 时，传入 BlockMangerId、blockId 和SotrageLevel 等参数，通过这些参数判断数据的操作是插入、更新还是删除操作。

BlockManagerMasterEndpoint # updateBlockInfo：

```scala
def updateBlockInfo(
    blockId: BlockId,
    storageLevel: StorageLevel,
    memSize: Long,
    diskSize: Long) {

  updateLastSeenMs()

  val blockExists = _blocks.containsKey(blockId)
  var originalMemSize: Long = 0
  var originalDiskSize: Long = 0
  var originalLevel: StorageLevel = StorageLevel.NONE

  if (blockExists) {
    // The block exists on the slave already.
    val blockStatus: BlockStatus = _blocks.get(blockId)
    originalLevel = blockStatus.storageLevel
    originalMemSize = blockStatus.memSize
    originalDiskSize = blockStatus.diskSize

    if (originalLevel.useMemory) {
      _remainingMem += originalMemSize
    }
  }

  if (storageLevel.isValid) {
    /* isValid means it is either stored in-memory or on-disk.
     * The memSize here indicates the data size in or dropped from memory,
     * externalBlockStoreSize here indicates the data size in or dropped from externalBlockStore,
     * and the diskSize here indicates the data size in or dropped to disk.
     * They can be both larger than 0, when a block is dropped from memory to disk.
     * Therefore, a safe way to set BlockStatus is to set its info in accurate modes. */
    var blockStatus: BlockStatus = null
    if (storageLevel.useMemory) {
      blockStatus = BlockStatus(storageLevel, memSize = memSize, diskSize = 0)
      _blocks.put(blockId, blockStatus)
      _remainingMem -= memSize
      if (blockExists) {
        logInfo(s"Updated $blockId in memory on ${blockManagerId.hostPort}" +
          s" (current size: ${Utils.bytesToString(memSize)}," +
          s" original size: ${Utils.bytesToString(originalMemSize)}," +
          s" free: ${Utils.bytesToString(_remainingMem)})")
      } else {
        logInfo(s"Added $blockId in memory on ${blockManagerId.hostPort}" +
          s" (size: ${Utils.bytesToString(memSize)}," +
          s" free: ${Utils.bytesToString(_remainingMem)})")
      }
    }
    if (storageLevel.useDisk) {
      blockStatus = BlockStatus(storageLevel, memSize = 0, diskSize = diskSize)
      _blocks.put(blockId, blockStatus)
      if (blockExists) {
        logInfo(s"Updated $blockId on disk on ${blockManagerId.hostPort}" +
          s" (current size: ${Utils.bytesToString(diskSize)}," +
          s" original size: ${Utils.bytesToString(originalDiskSize)})")
      } else {
        logInfo(s"Added $blockId on disk on ${blockManagerId.hostPort}" +
          s" (size: ${Utils.bytesToString(diskSize)})")
      }
    }
    if (!blockId.isBroadcast && blockStatus.isCached) {
      _cachedBlocks += blockId
    }
  } else if (blockExists) {
    // If isValid is not true, drop the block.
    _blocks.remove(blockId)
    _cachedBlocks -= blockId
    if (originalLevel.useMemory) {
      logInfo(s"Removed $blockId on ${blockManagerId.hostPort} in memory" +
        s" (size: ${Utils.bytesToString(originalMemSize)}," +
        s" free: ${Utils.bytesToString(_remainingMem)})")
    }
    if (originalLevel.useDisk) {
      logInfo(s"Removed $blockId on ${blockManagerId.hostPort} on disk" +
        s" (size: ${Utils.bytesToString(originalDiskSize)})")
    }
  }
}
```

* 在处理 blockLoacations，根据 blockId 判断 blockLocations 中是否包含该数据块。如果包含该数据块，则根据数据块的操作，当进行数据更新时，更新数据块所在的 BlockMangerId 信息，当进行数据删除时，则移除该 BlockMangerId 信息，在删除过程中判断数据块对应的 Executor 是否为空，如果为空表示在集群中删除了该数据块，则在 blockLoactions 删除该数据块信息。

### 3.2、获取远程节点数据块

应用程序数据存储后，在获取远程节点数据、获取 RDD 执行的首选位置等操作时需要根据数据块的编号查询数据块所处的位置，此时发送 GetLocations 或 GetLocationsMultipleBlockIds 给 BlockManagerMasterEndpoint ，通过对元数据的查询获取数据块的位置信息。

BlockManagerMasterEndpoint  # receiveAndReply：

```scala
case GetLocations(blockId) =>
  context.reply(getLocations(blockId))
```

BlockManagerMasterEndpoint  # getLocations：

```scala
private def getLocations(blockId: BlockId): Seq[BlockManagerId] = {
  if (blockLocations.containsKey(blockId)) blockLocations.get(blockId).toSeq else Seq.empty
}
```

### 3.3、删除数据块

Spark 提供删除 RDD、数据块、广播变量的方式。当数据需要删除的时候，提交删除信息给 BlockMangerSlaveEndPoint 终端点，在该终端点发起删除操作，删除操作一方面需要删除 Driver 端元数据信息，另一方面需要发送消息通知 Executor，删除对应的物理数据。

比如 SparkContext 中 unpersistRDD：

```scala
/**
 * Unpersist an RDD from memory and/or disk storage
 */
private[spark] def unpersistRDD(rddId: Int, blocking: Boolean = true) {
  env.blockManager.master.removeRdd(rddId, blocking)
  persistentRdds.remove(rddId)
  listenerBus.post(SparkListenerUnpersistRDD(rddId))
}
```

BlockManagerMaster # removeRdd：

```scala
/** Remove all blocks belonging to the given RDD. */
def removeRdd(rddId: Int, blocking: Boolean) {
  val future = driverEndpoint.askSync[Future[Seq[Int]]](RemoveRdd(rddId))
  future.failed.foreach(e =>
    logWarning(s"Failed to remove RDD $rddId - ${e.getMessage}", e)
  )(ThreadUtils.sameThread)
  if (blocking) {
    timeout.awaitResult(future)
  }
}
```

经过消息传递来到 BlockManagerMasterEndpoint  消息终端。

BlockManagerMasterEndpoint  # receiveAndReply：

```scala
case RemoveRdd(rddId) =>
  context.reply(removeRdd(rddId))
```

BlockManagerMasterEndpoint  # removeRdd：

```scala
private def removeRdd(rddId: Int): Future[Seq[Int]] = {
  // First remove the metadata for the given RDD, and then asynchronously remove the blocks
  // from the slaves.

  // Find all blocks for the given RDD, remove the block from both blockLocations and
  // the blockManagerInfo that is tracking the blocks.
  // 首先根据 RDDId 获取该 RDD 对应的数据块信息
  val blocks = blockLocations.asScala.keys.flatMap(_.asRDDId).filter(_.rddId == rddId)
  blocks.foreach { blockId =>
    // 然后根据该 blockId 找出这些数据块在 blockManagerId 中的列表，遍历这些列表并删除
    val bms: mutable.HashSet[BlockManagerId] = blockLocations.get(blockId)
    bms.foreach(bm => blockManagerInfo.get(bm).foreach(_.removeBlock(blockId)))
    // 同时删除 blockLocations 对应数据块的元数据
    blockLocations.remove(blockId)
  }

  // Ask the slaves to remove the RDD, and put the result in a sequence of Futures.
  // The dispatcher is used as an implicit argument into the Future sequence construction.
  // 最后发送 RemoveRDD 消息给 Executor，通知其删除 RDD
  val removeMsg = RemoveRdd(rddId)

  val futures = blockManagerInfo.values.map { bm =>
    bm.slaveEndpoint.ask[Int](removeMsg).recover {
      case e: IOException =>
        logWarning(s"Error trying to remove RDD $rddId from block manager ${bm.blockManagerId}",
          e)
        0 // zero blocks were removed
    }
  }.toSeq

  Future.sequence(futures)
}
```

### 3.4、总结

BlockManger 存在于 Dirver 端和每个 Executor 中，在 Driver 端的 BlockManger 保存了数据的元数据信息，而在 Executor 的 BlockManger 根据接受到消息进行操作：

* 当 Executor 的 BlockManger 接受到读取数据时，根据数据块所在节点是否为本地使用 BlockManger 不同的方法进行处理。如果在本地，则直接调用 MemoryStore 和 DiskStore 中的取方法 getValues/getBytes 进行读取；如果在远程，则调用 BlockTransferService 的服务进行获取远程数据节点上的数据。

* 当 Executor 的 BlockManger 接收到写入数据时，如果不需要创建副本，则调用 BlockStore 的接口方法进行处理，根据数据写入的存储模型，决定调用对应的写入方法。

BlockManager # get：

```scala
/**
 * Get a block from the block manager (either local or remote).
 *
 * This acquires a read lock on the block if the block was stored locally and does not acquire
 * any locks if the block was fetched from a remote block manager. The read lock will
 * automatically be freed once the result's `data` iterator is fully consumed.
 */
def get[T: ClassTag](blockId: BlockId): Option[BlockResult] = {
  val local = getLocalValues(blockId)
  if (local.isDefined) {
    logInfo(s"Found block $blockId locally")
    return local
  }
  val remote = getRemoteValues[T](blockId)
  if (remote.isDefined) {
    logInfo(s"Found block $blockId remotely")
    return remote
  }
  None
}
```