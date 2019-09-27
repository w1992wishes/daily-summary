# 【ES】ElasticSearch 6.4.2 安装

[TOC]

## 一、安装 es

### 1.1、新增 es 用户

因为 es 不能用 root 启动。

```shell
sudo useradd -m es -s /bin/bash  //添加用户
sudo passwd es //创建密码
sudo adduser es sudo   //sudo 授权
```

### 1.2、下载 es 版本、解压

```shell
tar -zxvf elastic*.tar.gz 

chown -R es:es  elasticsearch-6.4.2

mkdir -p /var/log/elasticsearch
mkdir -p /data/elasticsearch
chown -R es:es /var/log/elasticsearch
chown -R es:es /data/elasticsearch
```

### 1.3、配置

```shell
vi elasticsearch.yml
```

```yml
cluster.name: my-es-cluster
#
# ------------------------------------ Node ------------------------------------
#
# Use a descriptive name for the node:
#
node.name: es-node-1
node.master: true
node.data: true
#
# Add custom attributes to the node:
#
#node.attr.rack: r1
#
# ----------------------------------- Paths ------------------------------------
#
# Path to directory where to store the data (separate multiple locations by comma):
path.data: /data/elasticsearch
#
#
# Path to log files:
path.logs: /var/log/elasticsearch
#
#
# ----------------------------------- Memory -----------------------------------
#
# Lock the memory on startup:
#
#bootstrap.memory_lock: true
#
# Make sure that the heap size is set to about half the memory available
# on the system and that the owner of the process is allowed to use this
# limit.
#
# Elasticsearch performs poorly when the system is swapping the memory.
#
# ---------------------------------- Network -----------------------------------
#
# Set the bind address to a specific IP (IPv4 or IPv6):
#
#network.host: 192.168.0.1
network.host: 0.0.0.0
# Set a custom port for HTTP:
#
http.port: 9200
transport.tcp.port: 9300
#
# For more information, consult the network module documentation.
#
# --------------------------------- Discovery ----------------------------------
#
# Pass an initial list of hosts to perform discovery when new node is started:
# The default list of hosts is ["127.0.0.1", "[::1]"]
#
discovery.zen.ping.unicast.hosts: ["192.168.11.72"]
#
# Prevent the "split brain" by configuring the majority of nodes (total number of master-eligible nodes / 2 + 1):
#
discovery.zen.minimum_master_nodes: 1
```

### 1.4、启动

```shell
# (后台运行)
bin/elasticsearch -d
```

如果遇到 如果遇到 max virtual memory areas vm.max_map_count [65530] likely too low, increase to at least [262144] 错误。

切换到root用户修改配置sysctl.conf

```shell
vi /etc/sysctl.conf
```

vm.max_map_count=655360

并执行命令：

```shell
sysctl -p
```

切换到 es 用户，重新启动 elasticsearch。

## 二、安装 head 插件

### 2.1、安装 grunt

grunt 是一个很方便的构建工具，可以进行打包压缩、测试、执行等等的工作，6.x里的 head 插件需要通过 grunt 启动。因此需要安装grunt：

```shell
npm install -g grunt-cli
```

### 2.2、下载 elasticsearch-head 插件的源码

```shell
git clone git://github.com/mobz/elasticsearch-head.git
```

### 2.3、配置 elasticsearch-head

#### 2.3.1、修改 elasticsearch-head 配置-Gruntfile.js

elasticsearch-head/Gruntfile.js，增加 hostname 属性，设置为*：

```js
connect: {
    server: {
        options: {
            port: 9100,
            hostname: '*',
            base: '.',
            keepalive: true
        }
    }
}
```

#### 2.3.2、修改 elasticsearch-head 源码配置-app.js

修改 elasticsearch-head 的连接地址:

```js
this.base_uri = this.config.base_uri || this.prefs.get("app-base_uri") || "http://localhost:9200";
```

把 localhost 修改成 es 的服务器地址:

```js
this.base_uri = this.config.base_uri || this.prefs.get("app-base_uri") || "http://127.0.0.1:9200";
```

### 2.4、添加 Elasticsearch 的配置参数

elasticsearch.yml 配置文件，在文件末尾追加如下配置：

```yml
*# 设置集群的名字，对应为http://127.0.0.1:9200/中的"cluster_name" : "acanx-es-demo",*
cluster.name: acanx-es-demo
*# 节点名字，对应为http://127.0.0.1:9200/中的"name" : "node-1001",*
node.name: node-1001
node.master: true
node.data: true
*# 修改ES的监听地址，以便其他机器也可以访问*
network.host: 0.0.0.0
*# 默认端口*
http.port: 9200
*# 增加新的CORS参数，这样head插件可以访问ES*
http.cors.enabled: true
http.cors.allow-origin: "*"
```

### 2.5、运行 elasticsearch-head

在 elasticsearch-head 源码目录下，执行 npm install 下载的包：

```shell
npm install
```

在head源代码目录下启动 Nodejs：

```shell
grunt server
```

