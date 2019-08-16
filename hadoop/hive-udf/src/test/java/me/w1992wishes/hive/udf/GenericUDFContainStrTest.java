package me.w1992wishes.hive.udf;

import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.JavaBooleanObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试该函数比较复杂的部分是初始化，一旦调用顺序明确了，就知道怎么去构建该对象测试流程
 */
public class GenericUDFContainStrTest {
    
  @Test
  public void testGenericUDFContainStr() throws HiveException {
      
    // 建立需要的模型  
    GenericUDFContainStr example = new GenericUDFContainStr();
    ObjectInspector stringOI = PrimitiveObjectInspectorFactory.javaStringObjectInspector;
    ObjectInspector listOI = ObjectInspectorFactory.getStandardListObjectInspector(stringOI);
    JavaBooleanObjectInspector resultInspector = (JavaBooleanObjectInspector) example.initialize(new ObjectInspector[]{listOI, stringOI});
      
    // create the actual UDF arguments  
    List<String> list = new ArrayList<String>();
    list.add("a");  
    list.add("b");  
    list.add("c");  
      
    // 测试结果  
      
    // 存在的值  
    Object result = example.evaluate(new GenericUDF.DeferredObject[]{new GenericUDF.DeferredJavaObject(list), new GenericUDF.DeferredJavaObject("a")});
    Assert.assertTrue(resultInspector.get(result));
      
    // 不存在的值  
    Object result2 = example.evaluate(new GenericUDF.DeferredObject[]{new GenericUDF.DeferredJavaObject(list), new GenericUDF.DeferredJavaObject("d")});
    Assert.assertFalse(resultInspector.get(result2));
      
    // 为null的参数  
    Object result3 = example.evaluate(new GenericUDF.DeferredObject[]{new GenericUDF.DeferredJavaObject(null), new GenericUDF.DeferredJavaObject(null)});
    Assert.assertNull(result3);  
  }  
} 