package me.w1992wishes.hbase.inaction.filter;

import me.w1992wishes.hbase.common.util.HBaseUtils;
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 * @author w1992wishes 2019/11/4 17:24
 */
public class HelloValueFilter {

    public static void main(String[] args) throws IOException {
        Connection connection = HBaseUtils.getCon();

        Table table = connection.getTable(TableName.valueOf("myTable"));
        Scan scan = new Scan();

        // SubstringComparator 比较器可以判断目标字符串是否包含所指定的字符串。
        Filter filter = valueFilter(CompareOperator.EQUAL, new SubstringComparator("Wan"));
        scan.setFilter(filter);

        ResultScanner rs = table.getScanner(scan);
        for (Result r : rs) {
            String name = Bytes.toString(r.getValue(Bytes.toBytes("myCf"), Bytes.toBytes("name")));
            System.out.println(name);
        }
        rs.close();

    }

    /**
     * 值过滤器
     */
    private static Filter valueFilter(CompareOperator compareOp, ByteArrayComparable comparator) {
        return new ValueFilter(compareOp, comparator);
    }

    /**
     * 单列值过滤器
     */
    private static Filter singleColumnFilter(byte[] columnFamily, byte[] column, CompareOperator compareOp, ByteArrayComparable comparator) {
        return new SingleColumnValueFilter(columnFamily, column, compareOp, comparator);
    }

}
