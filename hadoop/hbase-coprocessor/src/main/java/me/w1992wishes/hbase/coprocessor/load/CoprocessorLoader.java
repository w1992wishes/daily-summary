package me.w1992wishes.hbase.coprocessor.load;

import me.w1992wishes.hbase.coprocessor.endpoint.RelationCountEndpoint;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 * @author w1992wishes 2019/8/3 11:14
 */
public class CoprocessorLoader {

    /**
     * 声明静态配置
     */
    private static Configuration conf;

    /**
     * 初始化连接
     */
    private static Connection con;

    static {
        conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "master");
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        try {
            // 获得连接对象
            con = ConnectionFactory.createConnection(conf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        allVersion();

    }

    private static void allVersion() throws IOException {
        TableName tableName = TableName.valueOf("follows");
        Admin admin = con.getAdmin();

        // 1.先 disable 表
        admin.disableTable(tableName);

        // 新建一个表描述
        String path = "hdfs://master:9000/hbase/lib/hbase-coprocessor.jar";
        TableDescriptorBuilder tableDescriptorBuilder =
                TableDescriptorBuilder.newBuilder(tableName)
                        /*.setRegionSplitPolicyClassName(IncreasingToUpperBoundRegionSplitPolicy.class.getName())
                        .setValue("hbase.increasing.policy.initial.size", "134217728")*/
                        .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes("f")).setMaxVersions(3).build())
                        .setValue("COPROCESSOR$1", path + "|"
                                + RelationCountEndpoint.class.getCanonicalName() + "|"
                                + Coprocessor.PRIORITY_USER);
        // 2. 修改表
        admin.modifyTable(tableDescriptorBuilder.build());

        // 3. enable 表
        admin.enableTable(tableName);

        con.close();
    }

    private static void newVersion() throws IOException {
        TableName tableName = TableName.valueOf("users");

        Admin admin = con.getAdmin();
        // 1.先 disable 表
        admin.disableTable(tableName);

        String path = "hdfs://master:9000/hbase/lib/hbase-coprocessor.jar";
        TableDescriptorBuilder tableDescriptorBuilder =
                TableDescriptorBuilder.newBuilder(tableName)
                .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes("f")).setMaxVersions(3).build())
                .setCoprocessor(RelationCountEndpoint.class.getCanonicalName())
                .setCoprocessor(CoprocessorDescriptorBuilder.newBuilder(RelationCountEndpoint.class.getCanonicalName())
                        .setJarPath(path)
                        .setPriority(Coprocessor.PRIORITY_USER)
                        .build());

        // 2. 修改表
        admin.modifyTable(tableDescriptorBuilder.build());

        // 3. enable 表
        admin.enableTable(tableName);

        con.close();
    }
}
