package me.w1992wishes.hbase.common.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.regionserver.IncreasingToUpperBoundRegionSplitPolicy;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author w1992wishes 2019/6/10 15:20
 */
public class HBaseUtils {

    // 声明静态配置
    private static Configuration conf;
    // 初始化连接
    private static Connection con;

    static {
        conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "192.168.11.72,192.168.11.73");
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        try {
            con = ConnectionFactory.createConnection(conf);// 获得连接对象
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 获得连接
    public static Connection getCon() {
        if (con == null || con.isClosed()) {
            try {
                con = ConnectionFactory.createConnection(conf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return con;
    }

    // 关闭连接
    public static void close() {
        if (con != null) {
            try {
                con.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 创建数据库表
    public static void createTable(String tableName, String... columnFamilys) throws IOException {
        // 建立一个数据库的连接
        Connection conn = getCon();
        // 创建一个数据库管理员
        Admin admin = conn.getAdmin();
        TableName tn = TableName.valueOf(tableName);
        if (admin.tableExists(tn)) {
            System.out.println(tableName + "表已存在");
            //conn.close();
            //System.exit(0);
        } else {
            // 新建一个表描述
            TableDescriptorBuilder tableBuilder =
                    TableDescriptorBuilder.newBuilder(tn)
                    .setRegionSplitPolicyClassName(IncreasingToUpperBoundRegionSplitPolicy.class.getName())
                    .setValue("hbase.increasing.policy.initial.size", "134217728");
            // 在表描述里添加列族
            for (String columnFamily : columnFamilys) {
                tableBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily)).build());
            }
            // 根据配置好的表描述建表
            admin.createTable(tableBuilder.build());
            System.out.println("创建" + tableName + "表成功");
        }
        conn.close();
    }

    // 创建数据库表
    public static void createTable(String tableName, byte[][] splitKeys, String... columnFamilys) throws IOException {
        // 建立一个数据库的连接
        Connection conn = getCon();
        // 创建一个数据库管理员
        Admin admin = conn.getAdmin();
        TableName tn = TableName.valueOf(tableName);
        if (admin.tableExists(tn)) {
            System.out.println(tableName + "表已存在");
            //conn.close();
            //System.exit(0);
        } else {
            // 新建一个表描述
            TableDescriptorBuilder tableBuilder =
                    TableDescriptorBuilder.newBuilder(tn)
                            /*.setRegionSplitPolicyClassName(IncreasingToUpperBoundRegionSplitPolicy.class.getName())
                            .setValue("hbase.increasing.policy.initial.size", "134217728")*/;
            // 在表描述里添加列族
            for (String columnFamily : columnFamilys) {
                tableBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily)).build());
            }
            // 根据配置好的表描述建表
            admin.createTable(tableBuilder.build(), splitKeys);
            System.out.println("创建" + tableName + "表成功");
        }
        conn.close();
    }

    // 删除数据库表
    public static void deleteTable(String tableName) throws IOException {
        // 建立一个数据库的连接
        Connection conn = getCon();
        // 创建一个数据库管理员
        Admin admin = conn.getAdmin();
        TableName tn = TableName.valueOf(tableName);
        if (admin.tableExists(tn)) {
            // 失效表
            admin.disableTable(tn);
            // 删除表
            admin.deleteTable(tn);
            System.out.println("删除" + tableName + "表成功");
            conn.close();
        } else {
            System.out.println("需要删除的" + tableName + "表不存在");
            conn.close();
            System.exit(0);
        }
    }

    // 添加一条数据 或者 更新数据
    public static void addRow(String tableName, String rowKey, String columnFamily, String column, String value)
            throws IOException {
        // 建立一个数据库的连接
        Connection conn = getCon();
        // 获取表
        Table table = conn.getTable(TableName.valueOf(tableName));
        // 通过rowkey创建一个put对象
        Put put = new Put(Bytes.toBytes(rowKey));
        // 在put对象中设置列族、列、值
        put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column), Bytes.toBytes(value));
        // 插入数据,可通过put(List<Put>)批量插入
        table.put(put);
        // 关闭资源
        table.close();
        conn.close();
    }

    // 通过 rowkey 取到多个族列的值
    public static Map<String, Map<String, String>> getRow(String tableName, String rowKey) throws IOException {
        // 建立一个数据库的连接
        Connection conn = getCon();
        // 获取表
        Table table = conn.getTable(TableName.valueOf(tableName));
        // 通过rowkey创建一个get对象
        Get get = new Get(Bytes.toBytes(rowKey));
        // 输出结果
        Result r = table.get(get);
        Map<String, Map<String, String>> results = new HashMap<>();
        for (Cell cell : r.listCells()) {
            String familyName = Bytes.toString(CellUtil.cloneFamily(cell));
            if (results.get(familyName) == null) {
                results.put(familyName, new HashMap<>());
            }
            results.get(familyName).put(Bytes.toString(CellUtil.cloneQualifier(cell)), Bytes.toString(CellUtil.cloneValue(cell)));
            /*System.out.println(
                    "行键:" + new String(CellUtil.cloneRow(cell)) + "\t" +
                            "列族:" + new String(CellUtil.cloneFamily(cell)) + "\t" +
                            "列名:" + new String(CellUtil.cloneQualifier(cell)) + "\t" +
                            "值:" + new String(CellUtil.cloneValue(cell)) + "\t" +
                            "时间戳:" + cell.getTimestamp());*/
        }
        // 关闭资源
        table.close();
        conn.close();
        return results;
    }

    //取到一个族列的值
    public static Map<String, String> getRow(String tableName, String rowKey, String family) throws IOException {
        Map<String, String> result = null;
        Connection conn = getCon();
        Table t = conn.getTable(TableName.valueOf(tableName));
        Get get = new Get(Bytes.toBytes(rowKey));
        get.addFamily(Bytes.toBytes(family));
        Result r = t.get(get);
        List<Cell> cs = r.listCells();
        result = cs.size() > 0 ? new HashMap<>() : result;
        for (Cell cell : cs) {
            result.put(Bytes.toString(CellUtil.cloneQualifier(cell)), Bytes.toString(CellUtil.cloneValue(cell)));
        }
        conn.close();
        t.close();
        return result;
    }

    // 取到一列的值
    public static String getRow(String tableName, String rowKey, String family,
                                String qualifier) throws IOException {
        Connection conn = getCon();
        Table t = conn.getTable(TableName.valueOf(tableName));
        Get get = new Get(Bytes.toBytes(rowKey));
        get.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier));
        Result r = t.get(get);
        conn.close();
        t.close();
        return Bytes.toString(CellUtil.cloneValue(r.listCells().get(0)));
    }

    // 全表扫描
    public static void scanTable(String tableName) throws IOException {
        // 建立一个数据库的连接
        Connection conn = getCon();
        // 获取表
        Table table = conn.getTable(TableName.valueOf(tableName));
        // 创建一个扫描对象
        Scan scan = new Scan();
        // 扫描全表输出结果
        ResultScanner results = table.getScanner(scan);
        for (Result result : results) {
            for (Cell cell : result.rawCells()) {
                System.out.println(
                        "行键:" + new String(CellUtil.cloneRow(cell)) + "\t" +
                                "列族:" + new String(CellUtil.cloneFamily(cell)) + "\t" +
                                "列名:" + new String(CellUtil.cloneQualifier(cell)) + "\t" +
                                "值:" + new String(CellUtil.cloneValue(cell)) + "\t" +
                                "时间戳:" + cell.getTimestamp());
            }
        }
        // 关闭资源
        results.close();
        table.close();
        conn.close();
    }

    // 删除
    public static void delRow(String tableName, String rowKey, String family,
                              String qualifier) throws IOException {
        // 建立一个数据库的连接
        Connection conn = getCon();
        Table table = conn.getTable(TableName.valueOf(tableName));
        Delete del = new Delete(Bytes.toBytes(rowKey));

        if (qualifier != null) {
            del.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier));
        } else if (family != null) {
            del.addFamily(Bytes.toBytes(family));
        }
        table.delete(del);
        // 关闭资源
        table.close();
        conn.close();
    }

    //删除一行
    public static void delRow(String tableName, String rowKey) throws IOException {
        delRow(tableName, rowKey, null, null);
    }

    //删除一行下的一个列族
    public static void delRow(String tableName, String rowKey, String family) throws IOException {
        delRow(tableName, rowKey, family, null);
    }

    // 删除多条数据
    public static void delRows(String tableName, String[] rows) throws IOException {
        // 建立一个数据库的连接
        Connection conn = getCon();
        // 获取表
        Table table = conn.getTable(TableName.valueOf(tableName));
        // 删除多条数据
        List<Delete> list = new ArrayList<>();
        for (String row : rows) {
            Delete delete = new Delete(Bytes.toBytes(row));
            list.add(delete);
        }
        table.delete(list);
        // 关闭资源
        table.close();
        conn.close();
    }

    // 删除列族
    public static void delColumnFamily(String tableName, String columnFamily) throws IOException {
        // 建立一个数据库的连接
        Connection conn = getCon();
        // 创建一个数据库管理员
        Admin hAdmin = conn.getAdmin();
        // 删除一个表的指定列族
        hAdmin.deleteColumnFamily(TableName.valueOf(tableName), Bytes.toBytes(columnFamily));
        // 关闭资源
        conn.close();
    }

    // 追加插入(将原有value的后面追加新的value，如原有value=a追加value=bc则最后的value=abc)
    public static void appendData(String tableName, String rowKey, String columnFamily, String column, String value)
            throws IOException {
        // 建立一个数据库的连接
        Connection conn = getCon();
        // 获取表
        Table table = conn.getTable(TableName.valueOf(tableName));
        // 通过rowkey创建一个append对象
        Append append = new Append(Bytes.toBytes(rowKey));
        // 在append对象中设置列族、列、值
        append.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column), Bytes.toBytes(value));
        // 追加数据
        table.append(append);
        // 关闭资源
        table.close();
        conn.close();
    }

    // 符合条件后添加数据(只能针对某一个rowkey进行原子操作)
    public static boolean checkAndPut(String tableName, String rowKey, String columnFamilyCheck, String columnCheck, String columnFamily, String column, String value) throws IOException {
        // 建立一个数据库的连接
        Connection conn = getCon();
        // 获取表
        Table table = conn.getTable(TableName.valueOf(tableName));
        // 设置需要添加的数据
        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column), Bytes.toBytes(value));
        // 当判断条件为真时添加数据
        boolean result = table.checkAndMutate(Bytes.toBytes(rowKey), Bytes.toBytes(columnFamilyCheck))
                .qualifier(Bytes.toBytes(columnCheck))
                .ifNotExists()
                .thenPut(put);
        // 关闭资源
        table.close();
        conn.close();

        return result;
    }

/*    // 符合条件后刪除数据(只能针对某一个rowkey进行原子操作)
    public static boolean checkAndDelete(String tableName, String rowKey, String columnFamilyCheck, String columnCheck,
                                         String valueCheck) throws IOException {
        // 建立一个数据库的连接
        Connection conn = getCon();
        // 获取表
        Table table = conn.getTable(TableName.valueOf(tableName));
        // 设置需要刪除的delete对象
        Delete delete = new Delete(Bytes.toBytes(rowKey));
        delete.addColumn(Bytes.toBytes(columnFamilyCheck), Bytes.toBytes(columnCheck));
        // 当判断条件为真时添加数据
        boolean result = table.checkAndMutate(Bytes.toBytes(rowKey), Bytes.toBytes(columnFamilyCheck))
                .ifEquals(Bytes.toBytes(valueCheck))
                .thenDelete(delete);
        // 关闭资源
        table.close();
        conn.close();

        return result;
    }*/

    // 计数器(amount为正数则计数器加，为负数则计数器减，为0则获取当前计数器的值)
    public static long incrementColumnValue(String tableName, String rowKey, String columnFamily, String column, long amount)
            throws IOException {
        // 建立一个数据库的连接
        Connection conn = getCon();
        // 获取表
        Table table = conn.getTable(TableName.valueOf(tableName));
        // 计数器
        long result = table.incrementColumnValue(Bytes.toBytes(rowKey), Bytes.toBytes(columnFamily), Bytes.toBytes(column), amount);
        // 关闭资源
        table.close();
        conn.close();

        return result;
    }
}
