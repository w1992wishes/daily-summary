## Zookeeper 权限管理机制

### 权限管理 ACL(Access Control List)　

ZooKeeper 的权限管理亦即 ACL 控制功能，使用 ACL 来对 Znode 进行访问控制。ACL 的实现和 Unix 文件访问许可非常相似：
它使用许可位来对一个节点的不同操作进行允许或禁止的权限控制。

ZooKeeper 的权限管理通过 Server、Client 两端协调完成。

#### Server端

一个 ZooKeeper 的节点存储两部分内容：数据和状态，状态中包含ACL 信息。创建一个 znode 会产生一个 ACL 列表，列表中每个 ACL 包括：
1. 权限 perms
2. 验证模式 scheme
3. 具体内容 expression：Ids

**ZooKeeper 提供了如下几种验证模式：**
1. Digest： Client 端由用户名和密码验证，譬如 user:pwd
3. Ip：Client 端由 IP 地址验证，譬如 172.2.0.0/24
4. World ：固定用户为 anyone，为所有 Client 端开放权限

当会话建立的时候，客户端将会进行自我验证。例如，当 scheme="digest" 时， Ids 为用户名密码， 即 "root ：J0sTy9BCUKubtK1y8pkbL7qoxSw"。

**权限许可集合如下：**
1. Create 允许对子节点 Create 操作
2. Read 允许对本节点 GetChildren 和 GetData 操作
3. Write 允许对本节点 SetData 操作
4. Delete 允许对子节点 Delete 操作
5. Admin 允许对本节点 setAcl 操作

另外，ZooKeeper Java API 支持三种标准的用户权限，它们分别为：
1. ZOO_PEN_ACL_UNSAFE：对于所有的ACL来说都是完全开放的，任何应用程序可以在节点上执行任何操作，比如创建、列出并删除子节点。
2. ZOO_READ_ACL_UNSAFE：对于任意的应用程序来说，仅仅具有读权限。
3. ZOO_CREATOR_ALL_ACL：授予节点创建者所有权限。需要注意的是，设置此权限之前，创建者必须已经通了服务器的认证。

Znode ACL 权限用一个 int 型数字p erms 表示， perms 的 5 个二进制位分别表示 setacl、delete、create、write、read。

比如adcwr=0x1f(11111)，----r=0x1(00001)，a-c-r=0x15(10101)。

**注意的是，exists 操作和 getAcl 操作并不受 ACL许可控制，因此任何客户端可以查询节点的状态和节点的 ACL。**

#### 客户端

Client 通过调用 addAuthInfo() 函数设置当前会话的 Author 信息（针对 Digest 验证模式）。

Server 收到 Client 发送的操作请求（除 exists、getAcl 之外），需要进行 ACL 验证：
对该请求携带的 Author 明文信息加密，并与目标节点的 ACL 信息进行比较，如果匹配则具有相应的权限，否则请求被 Server 拒绝。

一次 Client 对 Znode 进行操作的验证 ACL 的方式为，遍历 znode 的所有 ACL：
1. 对于每一个 ACL，首先操作类型与权限（perms）匹配
2. 只有匹配权限成功才进行 session 的 auth 信息与 ACL 的用户名、密码匹配
3. 如果两次匹配都成功，则允许操作；否则，返回权限不够 error（rc=-102）