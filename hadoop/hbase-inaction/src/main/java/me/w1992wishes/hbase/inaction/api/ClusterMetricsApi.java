package me.w1992wishes.hbase.inaction.api;

import me.w1992wishes.hbase.common.util.HBaseUtils;
import org.apache.hadoop.hbase.ClusterMetrics;
import org.apache.hadoop.hbase.ServerMetrics;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;

import java.io.IOException;
import java.util.Map;

/**
 * @author w1992wishes 2019/11/6 16:37
 */
public class ClusterMetricsApi {

    public static void main(String[] args) throws IOException {
        Connection connection = HBaseUtils.getCon();
        Admin admin = connection.getAdmin();

        ClusterMetrics clusterMetrics = admin.getClusterMetrics();
        // 当前集群总共有几个Region
        int regionsCount = clusterMetrics.getRegionCount();
        System.out.println("regionsCount          = " + regionsCount);

        // 当前集群总共有几个RegionServer
        int regionServerSize = clusterMetrics.getLiveServerMetrics().size();
        System.out.println("regionServerSize      = " + regionServerSize);

        // 当前每个RegionServer上加载了多少Region
        double regionsPerRegionServer = clusterMetrics.getAverageLoad();
        System.out.println("regionsPerRegionServer= " + regionsPerRegionServer);

        // 查看各个服务器上的堆内存大小和总请求数
        for (Map.Entry<ServerName, ServerMetrics> serverMetricsEntry: clusterMetrics.getLiveServerMetrics().entrySet()) {
            ServerMetrics serverMetrics = serverMetricsEntry.getValue();
            ServerName serverName = serverMetricsEntry.getKey();
            System.out.println(serverName.getServerName() + ":");
            System.out.println("堆最大值(MB) : " + serverMetrics.getMaxHeapSize());
            System.out.println("堆使用量(MB) : " + serverMetrics.getUsedHeapSize());
            System.out.println("总请求数 : " + serverMetrics.getRequestCount());
        }
    }

}
