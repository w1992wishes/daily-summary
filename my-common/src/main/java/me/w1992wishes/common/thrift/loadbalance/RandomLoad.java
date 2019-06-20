package me.w1992wishes.common.thrift.loadbalance;

import me.w1992wishes.common.thrift.pool.ThriftServer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author w1992wishes 2019/6/19 16:30
 */
public class RandomLoad implements LoadBalancer {

    private int randomNextInt() {
        return ThreadLocalRandom.current().nextInt();
    }

    @Override
    public ThriftServer getServerInstance(List<ThriftServer> servers) {
        if (servers.isEmpty()) {
            throw new RuntimeException("****** No server available. ******");
        }
        return servers.get(randomNextInt() % servers.size());
    }
}
