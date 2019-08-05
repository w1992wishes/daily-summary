package me.w1992wishes.hbase.inaction.coprocessor;

import me.w1992wishes.hbase.coprocessor.endpoint.RelationCountEndpoint;
import me.w1992wishes.hbase.coprocessor.endpoint.RelationCountEndpointClient;
import me.w1992wishes.hbase.coprocessor.setting.FollowedByCoprocessorSetting;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import java.io.IOException;

/**
 * @author w1992wishes 2019/8/3 17:27
 */
public class RelationEndpointTool {

    private static final String PATH = "hdfs://master:9000/hbase/lib/hbase-coprocessor.jar";

    private static Configuration conf;
    private static Connection connection;

    static {
        conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "master");
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        try {
            // 获得连接对象
            connection = ConnectionFactory.createConnection(conf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Throwable {
        //followedByLoadAndUnload();
        followedByCount();
    }

    private static void followedByLoadAndUnload() throws IOException {
        FollowedByCoprocessorSetting setting = new FollowedByCoprocessorSetting();
        // 卸载
        setting.unload(connection);
        // 加载
        setting.allVersionLoad(connection, PATH, RelationCountEndpoint.class.getCanonicalName());

        //setting.unload(connection);
        // 有点问题
        // setting.newVersionLoad(connection, PATH, RelationCountEndpoint.class.getName());

        connection.close();
    }

    private static void followedByCount() throws Throwable {
        RelationCountEndpointClient countEndpointClient = new RelationCountEndpointClient();
        long sum = countEndpointClient.followedByCount(connection, "star");
        System.out.println("****" + sum);
        connection.close();
    }

}
