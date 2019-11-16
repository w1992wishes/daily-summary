package me.w1992wishes.hbase.inaction.split;

import me.w1992wishes.hbase.common.util.HBaseUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * @author w1992wishes 2019/11/12 15:20
 */
public class SplitMain {

    public static void main(String[] args) {
        try {
            HBaseUtils.createTable("dwd:bigdata_event_face_person_5029", getSplitKeys(), "basic_info");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 在使用 HBase API建表的时候，需要产生 splitkeys 二维数组，这个数组存储的 rowkey 的边界值。
     * 必须要对 rowkey 排序，否则在调用admin.createTable(tableDescriptor,splitKeys) 的时候会出错
     */
    private static byte[][] getSplitKeys() {
        String[] keys = new String[] { "10|", "20|", "30|", "40|", "50|",
                "60|", "70|", "80|", "90|" };
        byte[][] splitKeys = new byte[keys.length][];
        // 升序排序
        TreeSet<byte[]> rows = new TreeSet<>(Bytes.BYTES_COMPARATOR);
        for (String key : keys) {
            rows.add(Bytes.toBytes(key));
        }
        Iterator<byte[]> rowKeyIter = rows.iterator();
        int i=0;
        while (rowKeyIter.hasNext()) {
            byte[] tempRow = rowKeyIter.next();
            rowKeyIter.remove();
            splitKeys[i] = tempRow;
            i++;
        }
        return splitKeys;
    }


    private static String getRandomNumber() {
        String ranStr = Math.random() + "";
        int pointIndex = ranStr.indexOf(".");
        return ranStr.substring(pointIndex + 1, pointIndex + 3);
    }

    private static List<Put> batchPut() {
        List<Put> list = new ArrayList<Put>();
        for (int i = 1; i <= 10000; i++) {
            byte[] rowkey = Bytes.toBytes(getRandomNumber() + "-" + System.currentTimeMillis() + "-" + i);
            Put put = new Put(rowkey);
            put.addColumn(Bytes.toBytes("face"), Bytes.toBytes("name"), Bytes.toBytes("zs" + i));
            list.add(put);
        }
        return list;
    }

    private void doPut() throws IOException {
        Connection conn = HBaseUtils.getCon();
        Table table = conn.getTable(TableName.valueOf("mid:bigdata_event_face_person_5030"));
        table.put(batchPut());
    }

}
