package me.w1992wishes.hive.udf;

import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoFactory;
import org.apache.hadoop.io.IntWritable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author w1992wishes 2019/8/15 10:09
 */
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
