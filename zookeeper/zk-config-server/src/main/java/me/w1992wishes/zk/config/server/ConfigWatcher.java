package me.w1992wishes.zk.config.server;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;

/**
 * 观察ZooKeeper中属性的更新情况，并将其打印到控制台
 *
 * @author Administrator
 */
public class ConfigWatcher implements Watcher {
    private ActiveKeyValueStore store;

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == EventType.NodeDataChanged) {
            try {
                dispalyConfig();
            } catch (InterruptedException e) {
                System.err.println("Interrupted. exiting. ");
                Thread.currentThread().interrupt();
            } catch (KeeperException e) {
                System.out.printf("KeeperException锛?s. Exiting.\n", e);
            }
        }
    }

    public ConfigWatcher(String hosts) throws IOException, InterruptedException {
        store = new ActiveKeyValueStore();
        store.connect(hosts);
    }

    public void dispalyConfig() throws KeeperException, InterruptedException {
        String value = store.read(ConfigUpdater.PATH, this);
        System.out.printf("Read %s as %s\n", ConfigUpdater.PATH, value);
    }

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        ConfigWatcher configWatcher = new ConfigWatcher(args[0]);
        configWatcher.dispalyConfig();
        //stay alive until process is killed or Thread is interrupted
        Thread.sleep(Long.MAX_VALUE);
    }
}