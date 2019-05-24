# Zookeeper

## ZooKeeper 的产生

分布式架构是中心化的设计，就是一个主控机连接多个处理节点。当主控机失效时，整个系统则就无法访问了，所以保证系统的高可用性是非常关键之处，
也就是要保证主控机的高可用性。分布式锁就是一个解决该问题的较好方案，多主控机抢一把锁。

Zookeeper 是雅虎模仿强大的 Google chubby 实现的一套分布式锁管理系统。同时，Zookeeper 分布式服务框架是 Apache Hadoop 的一个子项目，
它是一个针对大型分布式系统的可靠协调系统，它主要是用来**解决分布式应用中经常遇到的一些数据管理问题，可以高可靠的维护元数据**。

提供的功能包括：配置维护、名字服务、分布式同步、组服务等。

ZooKeeper 的设计目标就是封装好复杂易出错的关键服务，将简单易用的接口和性能高效、功能稳定的系统提供给用户。

## ZooKeeper 的使用

Zookeeper 作为一个分布式的服务框架，主要用来解决分布式集群中应用系统的一致性问题，它能提供基于类似于文件系统的目录节点树方式的数据存储，
但是 Zookeeper 并不是用来专门存储数据的，它的作用主要是用来维护和监控存储的数据的状态变化。通过监控这些数据状态的变化，从而可以达到基于数据的集群管理。

Zookeeper "数据" 是有限制的：

* 从数据大小来看：ZooKeeper 的数据存储在一个叫 ReplicatedDataBase 的数据库中，该数据是一个内存数据库，数据量不会太大，这一点上与 hadoop 的
 HDFS 有了很大的区别，HDFS 的数据主要存储在磁盘上，支持海量数据存储。
* 从数据类型来看：ZooKeeper 的数据在内存中，由于内存空间的限制，所以 ZooKeeper 存储的数据都是我们所关心的数据而且数据量还不能太大，需要根据我们要
实现的功能来选择相应的数据。简单来说，干什么事存什么数据，ZooKeeper 所实现的一切功能，都是由 ZK 节点的性质和该节点所关联的数据实现的。

例如：

1. 集群管理：利用临时节点特性，节点关联的是机器的主机名、IP地址等相关信息，集群单点故障也属于该范畴。
2. 统一命名：主要利用节点的唯一性和目录节点树结构。
3. 配置管理：节点关联的是配置信息。
4. 分布式锁：节点关联的是要竞争的资源。

ZooKeeper 是一个高可用的分布式数据管理与系统协调框架。基于对 Paxos 算法的实现，使该框架保证了分布式环境中数据的强一致性，也正是基于这样的特性，
使得 zookeeper 能够应用于很多场景。

## ZooKeeper 数据结构

[ZooKeeper 数据结构](documents/NODE.md)

## Zookeeper Watch 机制

[Zookeeper Watch 机制](documents/WATCHER.md)

## Zookeeper 权限管理机制

[Zookeeper 权限管理机制](documents/ACL.md)

## ZooKeeper 异常

[ZooKeeper 异常](documents/EXCEPTION.md)

## ZooKeeper Session 机制

[ZooKeeper Session 机制](documents/SESSION.md)

## ZooKeeper 实例状态

[ZooKeeper 实例状态](documents/STATUS.md)

## ZooKeeper 一致性原理

[ZooKeeper一致性原理](documents/PRINCIPLE.md)

## ZooKeeper 应用场景

[ZooKeeper 应用场景](documents/USEFULL.md)