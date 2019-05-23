package me.w1992wishes.zk.example;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 通过 digest(用户名：密码的方式)为创建的节点设置 ACL 的例子
 *
 * @author Administrator
 */
public class NewDigest {
    public static void main(String[] args) throws Exception {//new一个acl
        List<ACL> acls = new ArrayList<>();
        //添加第一个id，采用用户名密码形式
        Id id1 = new Id("digest", DigestAuthenticationProvider.generateDigest("admin:admin"));
        ACL acl1 = new ACL(ZooDefs.Perms.ALL, id1);
        acls.add(acl1);
        //添加第二个id，所有用户可读权限
        Id id2 = new Id("world", "anyone");
        ACL acl2 = new ACL(ZooDefs.Perms.READ, id2);
        acls.add(acl2);
        // Zk 用 admin 认证，创建 /test ZNode。
        ZooKeeper zk = new ZooKeeper(args[0], 2000, null);

        zk.create("/test", "data".getBytes(), acls, CreateMode.PERSISTENT);

        zk.addAuthInfo("digest", "admin:admin".getBytes());
        System.out.println(zk.getData("/test", false, null));
    }
}