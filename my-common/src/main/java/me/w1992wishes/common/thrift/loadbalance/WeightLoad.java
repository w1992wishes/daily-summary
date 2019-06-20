package me.w1992wishes.common.thrift.loadbalance;

import me.w1992wishes.common.thrift.pool.ThriftServer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author w1992wishes 2019/6/20 11:02
 */
public class WeightLoad implements LoadBalancer {
    @Override
    public ThriftServer getServerInstance(List<ThriftServer> servers) {
        if (servers.isEmpty()) {
            throw new RuntimeException("****** No server available. ******");
        }
        int[] weights = new int[servers.size()];
        for (int i = 0; i < servers.size(); i++) {
            weights[i] = servers.get(i).getWeight();
        }
        return servers.get(chooseWithWeight(weights));
    }

    private int chooseWithWeight(int[] weights) {
        int count = weights.length;
        int sum = 0;

        for (int i = 0; i < count; i++) {
            sum += weights[i];
        }

        int random = ThreadLocalRandom.current().nextInt(sum);

        while (random + weights[count - 1] < sum) {
            sum -= weights[--count];
        }
        return count - 1;
    }
}
