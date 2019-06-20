package me.w1992wishes.common.thrift.loadbalance;


import me.w1992wishes.common.thrift.pool.ThriftServer;
import java.util.List;

/**
 * @author w1992wishes 2019/6/18 15:43
 */
public interface LoadBalancer {

    /**
     * choose one server instance
     *
     * @param all all available server instances
     * @return ServerInstance
     */
    ThriftServer getServerInstance(List<ThriftServer> all);

    /**
     * 负载均衡
     */
    class LoadBalance {
        /**
         * 随机
         **/
        public static final String RANDOM = "random";
        /**
         * 轮询
         **/
        public static final String ROUND_ROBIN = "round_robin";
        /**
         * 权重
         **/
        public static final String WEIGHT = "weight";
        /**
         * 哈希
         **/
        public static final String HASH = "hash";
    }
}
