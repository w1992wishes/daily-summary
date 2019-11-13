package me.w1992wishes.hbase.inaction.split;

import me.w1992wishes.hbase.common.util.HBaseUtils;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * @author w1992wishes 2019/11/12 15:20
 */
public class SplitMain {

    public static void main(String[] args) {
        try {
            HBaseUtils.createTable("mid:bigdata_event_face_person_5030", getSplitKeys(), "face");
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

}
