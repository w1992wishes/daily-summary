package me.w1992wishes.hive.udf;

import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.StringObjectInspector;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author w1992wishes 2019/8/15 9:53
 */
public class GenericUDFNvlTest {

    @Test
    public void testGenericUDFNvl() throws HiveException {
        // 建立需要的模型
        GenericUDFNvl example = new GenericUDFNvl();
        ObjectInspector stringOI1 = PrimitiveObjectInspectorFactory.javaStringObjectInspector;
        ObjectInspector stringOI2 = PrimitiveObjectInspectorFactory.javaStringObjectInspector;
        StringObjectInspector resultInspector = (StringObjectInspector) example.initialize(new ObjectInspector[]{stringOI1, stringOI2});

        // 测试结果
        Object result1 = example.evaluate(new GenericUDF.DeferredObject[]{new GenericUDF.DeferredJavaObject(null), new GenericUDF.DeferredJavaObject("a")});
        Assert.assertEquals("a", resultInspector.getPrimitiveJavaObject(result1));

        // 测试结果
        Object result2 = example.evaluate(new GenericUDF.DeferredObject[]{new GenericUDF.DeferredJavaObject("dd"), new GenericUDF.DeferredJavaObject("a")});
        Assert.assertNotEquals("a", resultInspector.getPrimitiveJavaObject(result2));
    }

}
