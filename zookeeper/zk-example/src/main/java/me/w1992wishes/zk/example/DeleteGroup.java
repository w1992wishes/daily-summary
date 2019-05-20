package me.w1992wishes.zk.example;

import java.io.IOException;
import java.util.List;

import org.apache.zookeeper.KeeperException;

/**
 *
 * 删除一个组。ZooKeeper类提供了一个delete()方法，该方法有两个参数：
 *
 * 1. 路径
 *
 * 2. 版本号
 *
 * 如果所提供的版本号与znode的版本号一致，ZooKeeper会删除这个znode。这是一种乐观的加锁机制，使客户端能够检测出对znode的修改冲突。
 * 通过将版本号设置为-1，可以绕过这个版本检测机制，不管znode的版本号是什么而直接将其删除。
 *
 * ZooKeeper不支持递归的删除操作，因此在删除父节点之前必须先删除子节点。
 *
 * @author w
 */
public class DeleteGroup extends ConnectionWatcher {
    public void delete(String groupName) throws InterruptedException, KeeperException {
        String path = "/" + groupName;
        List<String> children;
        try {
            children = zk.getChildren(path, false);
            for (String child : children) {
                zk.delete(path + "/" + child, -1);
            }
            zk.delete(path, -1);
        } catch (KeeperException.NoNodeException e) {
            System.out.printf("Group %s does not exist\n", groupName);
            System.exit(1);
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException, KeeperException {
        DeleteGroup deleteGroup = new DeleteGroup();
        deleteGroup.connect(args[0]);
        deleteGroup.delete(args[1]);
        deleteGroup.close();
    }
}