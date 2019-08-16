# 【Hive】Hive UDF

[TOC]

## 一、UDF 介绍

UDF（User-Defined Functions）即是用户自定义的hive函数。当 Hive 自带的函数并不能完全满足业务的需求，这时可以根据具体需求自定义函数。UDF 函数可以直接应用于 select 语句，对查询结构做格式化处理后，再输出内容。

Hive 自定义函数包括三种：

- UDF: one to one ，进来一个出去一个，row mapping， 如：upper、substr函数；
- UDAF(A：aggregation): many to one，进来多个出去一个，row mapping，如sum/min；
- UDTF(T：table-generating)：one to mang，进来一个出去多行，如 lateral view 与 explode 。

注解使用：

@Describtion 注解是可选的，用于对函数进行说明，其中的 *FUNC* 字符串表示函数名，当使用 DESCRIBE FUNCTION 命令时，替换成函数名。@Describtion包含三个属性：

- name：用于指定 Hive 中的函数名。
- value：用于描述函数的参数。
- extended：额外的说明，如，给出示例，当使用 DESCRIBE FUNCTION EXTENDED name 的时候打印。

## 二、UDF 

开发自定义 UDF 函数有两种方式：

- 如果函数读和返回都是基础数据类型，即 Hadoop 和 Hive 的基本类型，如，Text、IntWritable、LongWritable、DoubleWritable 等，那么继承 org.apache.hadoop.hive.ql.exec.UDF ；
- 如果用来操作内嵌数据结构，如 Map，List 和 Set，则继承 org.apache.hadoop.hive.ql.udf.generic.GenericUDF；

### 2.1、简单 UDF 

用简单 UDF API 来构建一个 UDF 只涉及到编写一个类继承实现一个方法（evaluate），下面的例子来自 《Hive 编程指南》，将表中的生日字段转换为星座。

```java
@UDFType
@Description(
        name = "zodiac",
        value = "_FUNC_ (date) - " +
                " from the input date string " +
                " or separate month and day arguments, \n" +
                " returns the sign of the Zodiac.",
        extended = "Example :\n" +
                "> SELECT _FUNC_ (date_string) FROM src;\n" +
                "> SELECT _FUNC_ (month, day) FROM src;")
public class UDFZodiacSign extends UDF {

    private static final String ERROR_DATE_OF_MONTH = "invalid date of specify month";

    private static final String ERROR_MONTH_ARGS = "invalid argument of month";

    private static final String ERROR_DATE_STRING = "invalid date format";

    public String evaluate(Date bday) {
        return this.evaluate(bday.getMonth() + 1, bday.getDate());
    }

    public String evaluate(String dateString) {
        DateTime dateTime;
        try {
            dateTime = new DateTime(dateString);
        } catch (Exception e) {
            return ERROR_DATE_STRING;
        }
        return this.evaluate(dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
    }

    public String evaluate(Integer month, Integer day) {

        switch (month) {
            //判断是几月
            case 1:
                //判断是当前月的哪一段时间；然后就可以得到星座了；下面代码都一样的
                if (day > 0 && day < 20) {
                    return "魔蝎座";
                } else if (day < 32) {
                    return "水瓶座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 2:
                if (day > 0 && day < 19) {
                    return "水瓶座";
                } else if (day < 29) {
                    return "双鱼座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 3:
                if (day > 0 && day < 21) {
                    return "双鱼座";
                } else if (day < 32) {
                    return "白羊座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 4:
                if (day > 0 && day < 20) {
                    return "白羊座";
                } else if (day < 31) {
                    return "金牛座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 5:
                if (day > 0 && day < 21) {
                    return "金牛座";
                } else if (day < 32) {
                    return "双子座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 6:
                if (day > 0 && day < 22) {
                    return "双子座";
                } else if (day < 31) {
                    return "巨蟹座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 7:
                if (day > 0 && day < 23) {
                    return "巨蟹座";
                } else if (day < 32) {
                    return "狮子座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 8:
                if (day > 0 && day < 23) {
                    return "狮子座";
                } else if (day < 32) {
                    return "处女座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 9:
                if (day > 0 && day < 23) {
                    return "处女座";
                } else if (day < 31) {
                    return "天平座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 10:
                if (day > 0 && day < 24) {
                    return "天平座";
                } else if (day < 32) {
                    return "天蝎座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 11:
                if (day > 0 && day < 23) {
                    return "天蝎座";
                } else if (day < 31) {
                    return "射手座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            case 12:
                if (day > 0 && day < 22) {
                    return "射手座";
                } else if (day < 32) {
                    return "摩羯座";
                } else {
                    return ERROR_DATE_OF_MONTH;
                }
            default:
                return ERROR_MONTH_ARGS;
        }

    }

}
```

测试一下：

```java
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
```

### 2.2、复杂 GenericUDF

GenericUDF API 提供了一种方法去处理那些不是可写类型的对象，例如：struct，map 和 array 类型。

这个 API 需要用户亲自为函数的参数管理对象存储格式，验证接收的参数的数量与类型。

这个 API 要求实现以下方法：

```java
// 这个类似于简单 API 的 evaluate 方法，它可以读取输入数据和返回结果
abstract Object evaluate(GenericUDF.DeferredObject[] arguments);  
  
// 该方法应当是描述该 UDF 的字符串，显示函数的提示信息
abstract String getDisplayString(String[] children);  
  
// 只调用一次，在任何 evaluate() 调用之前，可以接收到一个可以表示函数输入参数类型的 object inspectors 数组
// 是用来验证该函数是否接收正确的参数类型和参数个数的地方
abstract ObjectInspector initialize(ObjectInspector[] arguments);  
```

例子同样来自 《Hive 编程指南》，编写一个用户自定义函数，称之为nvl()，这个函数传入的值如果是 null，那么就返回一个默认值。

函数 nvl() 要求有 2 个参数。如果第 1 个参数是非null值，那么就返回这个值；如果第 1 个参数是 null，那么就返回第 2 个参数的值。

```java
import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDFUtils;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;

@Description(name = "nvl",
        value = "_FUNC_(value, default_value) - Returns default value if value is null else returns value",
        extended = "Example:\n"
                + " > SELECT _FUNC_(null, 'bla') FROM src limit 1; \n")
public class GenericUDFNvl extends GenericUDF {

    private GenericUDFUtils.ReturnObjectInspectorResolver returnOIResolver;
    private ObjectInspector[] argumentOIs;

    @Override
    public ObjectInspector initialize(ObjectInspector[] arguments) throws UDFArgumentException {
        argumentOIs = arguments;
        // 1.检验参数个数
        if (arguments.length != 2) {
            throw new UDFArgumentException("The operator 'NVL' accepts 2 arguments.");
        }

        // 2.检验参数类型
        returnOIResolver = new GenericUDFUtils.ReturnObjectInspectorResolver(true);
        if (!(returnOIResolver.update(arguments[0]) && returnOIResolver.update(arguments[1]))) {
            throw new UDFArgumentTypeException(2, "The 1st and 2nd args of function NLV should have the same type, "
                    + "but they are different: \"" + arguments[0].getTypeName() + "\" and \"" + arguments[1].getTypeName() + "\"");
        }

        // 3.返回类型，和传入的参数类型一致
        return returnOIResolver.get();
    }

    @Override
    public Object evaluate(DeferredObject[] arguments) throws HiveException {
        Object retVal = returnOIResolver.convertIfNecessary(arguments[0].get(), argumentOIs[0]);
        if (retVal == null) {
            retVal = returnOIResolver.convertIfNecessary(arguments[1].get(), argumentOIs[1]);
        }
        return retVal;
    }

    @Override
    public String getDisplayString(String[] children) {
        StringBuilder sb = new StringBuilder();
        sb.append("if ");
        sb.append(children[0]);
        sb.append(" is null ");
        sb.append("returns ");
        sb.append(children[1]);
        return sb.toString();
    }

}
```

测试一下：

```java
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
```

## 三、UDAF

> PS：该段部分来自 [Hive UDAF开发详解](https://blog.csdn.net/kent7306/article/details/50110067)

UDAF 开发主要涉及到以下两个抽象类：

```java
org.apache.hadoop.hive.ql.udf.generic.AbstractGenericUDAFResolver
org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator
```

大致上，UDAF 函数读取数据（mapper），聚集一堆 mapper 输出到部分聚集结果（combiner），并且最终创建一个最终的聚集结果（reducer）。因为需要对多个combiner 进行聚集，所以需要保存部分聚集结果。

### 3.1、AbstractGenericUDAFResolver

Resolver 要覆盖实现 getEvaluator 方法，该方法会根据 sql 传人的参数数据格式指定调用哪个 Evaluator 进行处理。

```java
public GenericUDAFEvaluator getEvaluator(TypeInfo[] info) 
  throws SemanticException {
  throw new SemanticException(
        "This UDAF does not support the deprecated getEvaluator() method.");
}
```

### 3.2、GenericUDAFEvaluator

UDAF 逻辑处理主要发生在 Evaluator 中，要实现该抽象类的几个方法。理解Evaluator 之前，先介绍 ObjectInspector 接口与 GenericUDAFEvaluator 中的内部类 Model。

- ObjectInspector：*主要是解耦数据使用与数据格式，使数据流在输入输出端可以切换不同的输入输出格式，不同的 Operator上使用不同的格式。*

- Model：Model 代表了 UDAF 在 mapreduce 的各个阶段。

  ```java
  public static enum Mode {
      /**
       * PARTIAL1: 这个是mapreduce的map阶段:从原始数据到部分数据聚合
       * 将会调用iterate()和terminatePartial()
       */
      PARTIAL1,
          /**
       * PARTIAL2: 这个是mapreduce的map端的Combiner阶段，负责在map端合并map的数据:从部分数据聚合到部分数据聚合
       * 将会调用merge() 和 terminatePartial() 
       */
      PARTIAL2,
          /**
       * FINAL: mapreduce的reduce阶段:从部分数据的聚合到完全聚合 
       * 将会调用merge()和terminate()
       */
      FINAL,
          /**
       * COMPLETE: 如果出现了这个阶段，表示mapreduce只有map，没有reduce，所以map端就直接出结果了:从原始数据直接到完全聚合
        * 将会调用 iterate()和terminate()
       */
      COMPLETE
    };
  ```

一般情况下，完整的 UDAF 逻辑是一个 mapreduce 过程，如果有mapper 和reducer，就会经历 PARTIAL1(mapper)，FINAL(reducer)，如果还有 combiner，那就会经历 PARTIAL1(mapper)，PARTIAL2(combiner)，FINAL(reducer)。

而有一些情况下的 mapreduce，只有mapper，而没有 reducer，所以就会只有COMPLETE 阶段，这个阶段直接输入原始数据，出结果。

### 3.3、GenericUDAFEvaluator 的方法

```java
// 确定各个阶段输入输出参数的数据格式 ObjectInspectors，一般负责初始化内部字段,通常初始化用来存放最终结果的变量
public  ObjectInspector init(Mode m, ObjectInspector[] parameters) throws HiveException;
 
// 保存数据聚集结果的类
abstract AggregationBuffer getNewAggregationBuffer() throws HiveException;
 
// 重置聚集结果
public void reset(AggregationBuffer agg) throws HiveException;
 
// map阶段，迭代处理输入sql传过来的列数据
public void iterate(AggregationBuffer agg, Object[] parameters) throws HiveException;
 
// map与combiner结束返回结果，得到部分数据聚集结果
public Object terminatePartial(AggregationBuffer agg) throws HiveException;
 
// combiner合并map返回的结果，还有reducer合并mapper或combiner返回的结果。
public void merge(AggregationBuffer agg, Object partial) throws HiveException;
 
// reducer阶段，输出最终结果
public Object terminate(AggregationBuffer agg) throws HiveException;
```

### 3.4、图解Model与Evaluator关系

**Model 各阶段对应 Evaluator 方法调用**

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g5zaxt2v4qj20oy0evwg5.jpg)

**Evaluator 各个阶段下处理 mapreduce 流程**

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g5zb0cm3uej20k40mdmz2.jpg)

### 3.5、编码实例

下面的函数代码是计算指定列中字符的总数（包括空格）：

```java
@Description(
        name = "array_uniq_element_number",
        value = "_FUNC_(array) - Returns nubmer of objects with duplicate elements eliminated.",
        extended = "Example:\n"
        + "  > SELECT _FUNC_(array(1, 2, 2, 3, 3)) FROM src LIMIT 1;\n" + "  3")
public class GenericUDFArrayUniqElementNumber extends GenericUDF {

    private static final int ARRAY_IDX = 0;
    // Number of arguments to this UDF
    private static final int ARG_COUNT = 1;
    // External Name
    private static final String FUNC_NAME = "ARRAY_UNIQ_ELEMENT_NUMBER";

    private ListObjectInspector arrayOI;
    private ObjectInspector arrayElementOI;
    private final IntWritable result = new IntWritable(-1);

    @Override
    public ObjectInspector initialize(ObjectInspector[] arguments)
            throws UDFArgumentException {

        // Check if two arguments were passed
        if (arguments.length != ARG_COUNT) {
            throw new UDFArgumentException("The function " + FUNC_NAME
                    + " accepts " + ARG_COUNT + " arguments.");
        }

        // Check if ARRAY_IDX argument is of category LIST
        if (!arguments[ARRAY_IDX].getCategory().equals(ObjectInspector.Category.LIST)) {
            throw new UDFArgumentTypeException(ARRAY_IDX, "\""
                    + org.apache.hadoop.hive.serde.Constants.LIST_TYPE_NAME
                    + "\" " + "expected at function ARRAY_CONTAINS, but "
                    + "\"" + arguments[ARRAY_IDX].getTypeName() + "\" "
                    + "is found");
        }

        arrayOI = (ListObjectInspector) arguments[ARRAY_IDX];
        arrayElementOI = arrayOI.getListElementObjectInspector();

        return PrimitiveObjectInspectorFactory.writableIntObjectInspector;
    }

    @Override
    public IntWritable evaluate(DeferredObject[] arguments)
            throws HiveException {

        result.set(0);

        Object array = arguments[ARRAY_IDX].get();
        int arrayLength = arrayOI.getListLength(array);
        if (arrayLength <= 1) {
            result.set(arrayLength);
            return result;
        }

        //element compare; Algorithm complexity: O(N^2)
        int num = 1;
        int i, j;
        for (i = 1; i < arrayLength; i++) {
            Object listElement = arrayOI.getListElement(array, i);
            for (j = i - 1; j >= 0; j--) {
                if (listElement != null) {
                    Object tmp = arrayOI.getListElement(array, j);
                    if (ObjectInspectorUtils.compare(tmp, arrayElementOI, listElement,
                            arrayElementOI) == 0) {
                        break;
                    }
                }
            }
            if (-1 == j) {
                num++;
            }
        }

        result.set(num);
        return result;
    }

    @Override
    public String getDisplayString(String[] children) {
        return "array_uniq_element_number(" + children[ARRAY_IDX] + ")";
    }
}
```

测试：

```java
public class GenericUDAFTotalNumOfLettersTest {

    private GenericUDAFTotalNumOfLetters example;
    private GenericUDAFEvaluator evaluator;
    private ObjectInspector[] output;
    private PrimitiveObjectInspector[] poi;

    GenericUDAFTotalNumOfLetters.TotalNumOfLettersEvaluator.LetterSumAgg agg;

    Object[] param1 = {"tom"};
    Object[] param2 = {"tomT"};
    Object[] param3 = {"wu kong"};
    Object[] param4 = {"wu le"};

    @Before
    public void setUp() throws Exception {

        example = new GenericUDAFTotalNumOfLetters();

        //All the data are String
        String[] typeStrs = {"string"/*, "string", "string"*/};
        TypeInfo[] types = makePrimitiveTypeInfoArray(typeStrs);

        evaluator = example.getEvaluator(types);

        poi = new PrimitiveObjectInspector[1];
        poi[0] = PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(
                PrimitiveObjectInspector.PrimitiveCategory.STRING);
/*        poi[1] =  PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(
                PrimitiveObjectInspector.PrimitiveCategory.STRING);
        poi[2] =  PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(
                PrimitiveObjectInspector.PrimitiveCategory.STRING);*/

        //The output inspector
        output = new ObjectInspector[1];
        output[0] = PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(
                PrimitiveObjectInspector.PrimitiveCategory.INT);
        /*output[0] = ObjectInspectorFactory.getStandardListObjectInspector(poi[0]);*/

        agg = (GenericUDAFTotalNumOfLetters.TotalNumOfLettersEvaluator.LetterSumAgg) evaluator.getNewAggregationBuffer();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test(expected = UDFArgumentTypeException.class)
    public void testGetEvaluateorWithComplexTypes() throws Exception {
        TypeInfo[] types = new TypeInfo[1];
        types[0] = TypeInfoFactory.getListTypeInfo(TypeInfoFactory.getPrimitiveTypeInfo("string"));
        example.getEvaluator(types);
    }

    @Test(expected = UDFArgumentTypeException.class)
    public void testGetEvaluateorWithNotSupportedTypes() throws Exception {
        TypeInfo[] types = new TypeInfo[1];
        types[0] = TypeInfoFactory.getPrimitiveTypeInfo("boolean");
        example.getEvaluator(types);
    }

    @Test(expected = UDFArgumentTypeException.class)
    public void testGetEvaluateorWithMultiParams() throws Exception {
        String[] typeStrs3 = {"double", "int", "string"};
        TypeInfo[] types3 = makePrimitiveTypeInfoArray(typeStrs3);
        example.getEvaluator(types3);
    }

    @Test
    public void testIterate() throws HiveException {
        evaluator.init(GenericUDAFEvaluator.Mode.PARTIAL1, poi);
        evaluator.reset(agg);

        evaluator.iterate(agg, param1);
        Assert.assertEquals(3, agg.sum);

        evaluator.iterate(agg, param2);
        Assert.assertEquals(7, agg.sum);

        evaluator.iterate(agg, param3);
        Assert.assertEquals(14, agg.sum);
    }

    @Test
    public void testTerminatePartial() throws Exception {

        testIterate();

        Object partial = evaluator.terminatePartial(agg);

        Assert.assertTrue(partial instanceof IntWritable);
        Assert.assertEquals(new IntWritable(14), partial);
    }

    @Test
    public void testMerge() throws Exception {
        evaluator.init(GenericUDAFEvaluator.Mode.PARTIAL1, poi);
        evaluator.reset(agg);
        evaluator.iterate(agg, param1);
        evaluator.iterate(agg, param2);
        Object partial1 = evaluator.terminatePartial(agg);

        evaluator.init(GenericUDAFEvaluator.Mode.PARTIAL1, poi);
        evaluator.reset(agg);
        evaluator.iterate(agg, param3);
        Object partial2 = evaluator.terminatePartial(agg);

        evaluator.init(GenericUDAFEvaluator.Mode.PARTIAL1, poi);
        evaluator.reset(agg);
        evaluator.iterate(agg, param4);
        Object partial3 = evaluator.terminatePartial(agg);

        evaluator.init(GenericUDAFEvaluator.Mode.PARTIAL2, output);
        evaluator.reset(agg);
        evaluator.merge(agg, partial1);
        Assert.assertEquals(7, agg.sum);

        evaluator.merge(agg, partial2);
        Assert.assertEquals(14, agg.sum);

        evaluator.merge(agg, partial3);
        Assert.assertEquals(19, agg.sum);
    }

    @Test
    public void testTerminate() throws Exception {
        evaluator.init(GenericUDAFEvaluator.Mode.COMPLETE, poi);
        evaluator.reset(agg);

        evaluator.iterate(agg, param1);
        evaluator.iterate(agg, param2);
        evaluator.iterate(agg, param3);
        evaluator.iterate(agg, param4);
        Object term = evaluator.terminate(agg);

        Assert.assertTrue(term instanceof IntWritable);
        Assert.assertEquals(term, new IntWritable(19));
    }

    /**
     * Generate some TypeInfo from the typeStrs
     */
    private TypeInfo[] makePrimitiveTypeInfoArray(String[] typeStrs) {
        int len = typeStrs.length;

        TypeInfo[] types = new TypeInfo[len];

        for (int i = 0; i < len; i++) {
            types[i] = TypeInfoFactory.getPrimitiveTypeInfo(typeStrs[i]);
        }

        return types;
    }
}
```

## 四、UDTF

Hive 中 UDTF 可以将一行转成一行多列，也可以将一行转成多行多列，使用频率较高。

一个 UDTF 必须继承 GenericUDTF 抽象类然后实现抽象类中的 initialize，process，和 close方法。

- initialize：确定传入参数的类型并确定 UDTF 生成表的每个字段的数据类型（即输入类型和输出类型），主要是判断输入类型并确定返回的字段类型。
- process：调用了 initialize()  后，Hive 将把 UDTF 参数传给 process() 方法，处理一条输入记录，输出若干条结果记录，该方法中，每一次调用 forward() 产生一行；如果产生多列可以将多个列的值放在一个数组中，然后将该数组传入到 forward() 函数。
- close：在 process 调用结束后调用，用于进行其它一些额外操作，只执行一次。

```java
public class GenericUDTFNameParserGeneric extends GenericUDTF {

    private PrimitiveObjectInspector stringOI = null;

    @Override
    public StructObjectInspector initialize(ObjectInspector[] args) throws UDFArgumentException {

        if (args.length != 1) {
            throw new UDFArgumentException("GenericUDTFNameParserGeneric() takes exactly one argument");
        }

        if (args[0].getCategory() != ObjectInspector.Category.PRIMITIVE
                && ((PrimitiveObjectInspector) args[0]).getPrimitiveCategory() != PrimitiveObjectInspector.PrimitiveCategory.STRING) {
            throw new UDFArgumentException("GenericUDTFNameParserGeneric() takes a string as a parameter");
        }

        // 输入格式（inspectors）
        stringOI = (PrimitiveObjectInspector) args[0];

        // 输出格式（inspectors） -- 有两个属性的对象
        List<String> fieldNames = new ArrayList<>(2);
        List<ObjectInspector> fieldOIs = new ArrayList<>(2);
        fieldNames.add("name");
        fieldNames.add("surname");
        fieldOIs.add(PrimitiveObjectInspectorFactory.javaStringObjectInspector);
        fieldOIs.add(PrimitiveObjectInspectorFactory.javaStringObjectInspector);
        return ObjectInspectorFactory.getStandardStructObjectInspector(fieldNames, fieldOIs);
    }

    private ArrayList<Object[]> processInputRecord(String name) {
        ArrayList<Object[]> result = new ArrayList<>();

        // 忽略null值与空值
        if (name == null || name.isEmpty()) {
            return result;
        }

        String[] tokens = name.split("\\s+");

        if (tokens.length == 2) {
            result.add(new Object[]{tokens[0], tokens[1]});
        } else if (tokens.length == 4 && tokens[1].equals("and")) {
            result.add(new Object[]{tokens[0], tokens[3]});
            result.add(new Object[]{tokens[2], tokens[3]});
        }

        return result;
    }

    @Override
    public void process(Object[] record) throws HiveException {

        final String name = stringOI.getPrimitiveJavaObject(record[0]).toString();

        ArrayList<Object[]> results = processInputRecord(name);

        for (Object[] r : results) {
            forward(r);
        }
    }

    @Override
    public void close() throws HiveException {
        // do nothing
    }
}
```

## 五、UDF 使用

### 5.1、准备步骤

数据准备：

```shell
cat ./people.txt

John Smith
John and Ann White
Ted Green
Dorothy
```

把该文件上载到 hdfs 目录 /user/wqf 中：

```shell
hadoop fs -mkdir /user/wqf/people
hadoop fs -put ./people.txt /user/wqf/people
```

然后创建 hive 外部表，在 hive shell 中执行：

```shell
CREATE EXTERNAL TABLE people (name string)
ROW FORMAT DELIMITED 
FIELDS TERMINATED BY '\t' 
ESCAPED BY '' 
LINES TERMINATED BY '\n'
STORED AS TEXTFILE 
LOCATION '/user/wqf/people';
```

maven pom 中添加如下配置，然后运行 `mvn assembly:assembly`:

```xml
<build>
    <plugins>
        <plugin>
            <artifactId>maven-assembly-plugin</artifactId>
            <configuration>
                <descriptorRefs>
                    <descriptorRef>jar-with-dependencies</descriptorRef>
                </descriptorRefs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

将 jar 包上传到 hive 服务器。

### 5.2、临时添加 UDF

进入 hive 中：

```sql
hive> add jar /home/hadoop/testdir/hive/hive-udf-1.0-SNAPSHOT.jar
Added [/home/hadoop/testdir/hive/hive-udf-1.0-SNAPSHOT.jar] to class path
Added resources: [/home/hadoop/testdir/hive/hive-udf-1.0-SNAPSHOT.jar]

hive > CREATE TEMPORARY FUNCTION myNvl as 'me.w1992wishes.hive.udf.GenericUDFNvl';
hive> select myNvl(name, 'a') from people limit 1;
OK
John Smith

hive> CREATE TEMPORARY FUNCTION myCount as 'me.w1992wishes.hive.udf.GenericUDAFTotalNumOfLetters';
hive> select myCount(name) from people;
OK
44

hive> CREATE TEMPORARY FUNCTION myParser as 'me.w1992wishes.hive.udf.GenericUDTFNameParser';
hive> select myParser(name) from people;
OK
John	Smith
John	White
Ann	White
Ted	Green
Time taken: 0.18 seconds, Fetched: 4 row(s)
```

这种方式在会话结束后，函数自动销毁，因此每次打开新的会话，都需要重新 `add jar` 并且 `CREATE TEMPORARY FUNCTION`。

### 5.3、永久添加 UDF

不能是本地 jar 包，需要上传 jar 包到 hdfs 目录中：

```shell
hadoop fs -put hive-udf-1.0-SNAPSHOT.jar /user/hive/jars
```

然后进入 hive 中，创建函数：

```sql
hive> create function myCount as 'me.w1992wishes.hive.udf.GenericUDAFTotalNumOfLetters' using jar 'hdfs:/user/hive/jars/hive-udf-1.0-SNAPSHOT.jar';
OK
44
```

## 六、参考资料

1.《Hive 编程指南》
2.[Hive UDAF开发详解](https://blog.csdn.net/kent7306/article/details/50110067)