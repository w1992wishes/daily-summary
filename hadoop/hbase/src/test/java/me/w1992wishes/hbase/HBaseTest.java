package me.w1992wishes.hbase;

import me.w1992wishes.hbase.example.HBaseUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author w1992wishes 2019/6/10 15:43
 */
public class HBaseTest {

    @Test
    public void testCreateTable() throws IOException {
        //创建表
        HBaseUtils.createTable("myTest", "myfc1", "myfc2", "myfc3");
        HBaseUtils.createTable("myTest02", "myfc1", "myfc2", "myfc3");
    }


    @Test
    public void testAddRow() throws IOException {
        //插入数据
        HBaseUtils.addRow("myTest", "1", "myfc1", "sex", "men");
        HBaseUtils.addRow("myTest", "1", "myfc1", "name", "xiaoming");
        HBaseUtils.addRow("myTest", "1", "myfc1", "age", "32");
        HBaseUtils.addRow("myTest", "1", "myfc2", "name", "xiaohong");
        HBaseUtils.addRow("myTest", "1", "myfc2", "sex", "woman");
        HBaseUtils.addRow("myTest", "1", "myfc2", "age", "23");
    }

    @Test
    public void testGetRow1() throws IOException {
        //得到一行的所有列族的数据
        Map<String, Map<String, String>> result = HBaseUtils.getRow("myTest", "1");
        System.out.println("所有列族的数据是:  " + result);
        assertNotNull(result);
    }

    @Test
    public void testGetRow2() throws IOException {
        //得到一行下一个列族下的所有列的数据
        Map<String, String> result = HBaseUtils.getRow("myTest", "1", "myfc1");
        System.out.println("myfc1 结果： " + result);
        assertNotNull(result);
    }

    @Test
    public void testGetRow3() throws IOException {
        //得到一行下一个列族下的某列的数据
        String result = HBaseUtils.getRow("myTest", "1", "myfc1", "name");
        System.out.println("myfc1.name 结果： " + result);
        assertEquals("xiaoming", result);
    }

    @Test
    public void testScan() throws IOException {
        //得到一行下一个列族下的某列的数据
        HBaseUtils.scanTable("myTest");
    }

    @Test
    public void testDelRow() throws IOException {
        HBaseUtils.delRow("myTest", "1", "myfc1");
    }

    @Test
    public void testDeleteTable() throws IOException {
        HBaseUtils.deleteTable("myTest");
    }
}
