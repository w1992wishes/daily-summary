package me.w1992wishes.hbase.coprocessor.setting;

import me.w1992wishes.hbase.coprocessor.endpoint.RelationCountEndpoint;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 * @author w1992wishes 2019/8/3 11:14
 */
public class FollowedByCoprocessorSetting {

    public void allVersionLoad(Connection con, String coprocessorPath, String coprocessorClassName) throws IOException {
        TableName tableName = TableName.valueOf("followedBy");
        Admin admin = con.getAdmin();

        // 1.先 disable 表
        admin.disableTable(tableName);

        // 新建一个表描述
        TableDescriptorBuilder tableDescriptorBuilder =
                TableDescriptorBuilder.newBuilder(tableName)
                        .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes("f")).setMaxVersions(3).build())
                        .setValue("coprocessor$1", coprocessorPath + "|"
                                + coprocessorClassName + "|"
                                + Coprocessor.PRIORITY_USER);
        // 2. 修改表
        admin.modifyTable(tableDescriptorBuilder.build());

        // 3. enable 表
        admin.enableTable(tableName);
    }

    public void newVersionLoad(Connection con, String coprocessorPath, String coprocessorClassName) throws IOException {
        TableName tableName = TableName.valueOf("followedBy");

        Admin admin = con.getAdmin();
        // 1.先 disable 表
        admin.disableTable(tableName);

        TableDescriptorBuilder tableDescriptorBuilder =
                TableDescriptorBuilder.newBuilder(tableName)
                        .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes("f")).setMaxVersions(3).build())
                        .setCoprocessor(RelationCountEndpoint.class.getCanonicalName())
                        .setCoprocessor(CoprocessorDescriptorBuilder.newBuilder(coprocessorClassName)
                                .setJarPath(coprocessorPath)
                                .setPriority(Coprocessor.PRIORITY_USER)
                                .build());

        // 2. 修改表
        admin.modifyTable(tableDescriptorBuilder.build());

        // 3. enable 表
        admin.enableTable(tableName);
    }

    public void unload(Connection con) throws IOException {
        TableName tableName = TableName.valueOf("followedBy");
        Admin admin = con.getAdmin();

        admin.disableTable(tableName);

        TableDescriptorBuilder tableDescriptorBuilder =
                TableDescriptorBuilder.newBuilder(tableName)
                        .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes("f")).setMaxVersions(3).build());
        admin.modifyTable(tableDescriptorBuilder.build());
        admin.enableTable(tableName);
    }
}
