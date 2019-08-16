package me.w1992wishes.hive.udf;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.parse.SemanticException;
import org.apache.hadoop.hive.ql.udf.generic.AbstractGenericUDAFResolver;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorUtils;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoUtils;
import org.apache.hadoop.io.IntWritable;

/**
 * @author Administrator
 */
@Description(
        name = "letters",
        value = "_FUNC_(expr) - 返回该列中所有字符串的字符总数")
public class GenericUDAFTotalNumOfLetters extends AbstractGenericUDAFResolver {

    @Override
    public GenericUDAFEvaluator getEvaluator(TypeInfo[] parameters)
            throws SemanticException {
        if (parameters.length != 1) {
            throw new UDFArgumentTypeException(parameters.length - 1,
                    "Exactly one argument is expected.");
        }

        ObjectInspector oi = TypeInfoUtils.getStandardJavaObjectInspectorFromTypeInfo(parameters[0]);

        if (oi.getCategory() != ObjectInspector.Category.PRIMITIVE) {
            throw new UDFArgumentTypeException(0,
                    "Argument must be PRIMITIVE, but "
                            + oi.getCategory().name()
                            + " was passed.");
        }

        PrimitiveObjectInspector inputOI = (PrimitiveObjectInspector) oi;

        if (inputOI.getPrimitiveCategory() != PrimitiveObjectInspector.PrimitiveCategory.STRING) {
            throw new UDFArgumentTypeException(0,
                    "Argument must be String, but "
                            + inputOI.getPrimitiveCategory().name()
                            + " was passed.");
        }

        return new TotalNumOfLettersEvaluator();
    }

    public static class TotalNumOfLettersEvaluator extends GenericUDAFEvaluator {

        PrimitiveObjectInspector inputOI;
        PrimitiveObjectInspector integerOI;

        private IntWritable result;

        @Override
        public ObjectInspector init(Mode m, ObjectInspector[] parameters)
                throws HiveException {

            super.init(m, parameters);
            result = new IntWritable(0);
            inputOI = (PrimitiveObjectInspector) parameters[0];
            integerOI = PrimitiveObjectInspectorFactory.writableIntObjectInspector;
            // 指定各个阶段输出数据格式都为Integer类型
            return PrimitiveObjectInspectorFactory.writableIntObjectInspector;

        }

        /**
         * 存储当前字符总数的类
         */
        static class LetterSumAgg implements AggregationBuffer {
            int sum = 0;

            void add(int num) {
                sum += num;
            }
        }

        /**
         * 创建新的聚合计算的需要的内存，用来存储mapper,combiner,reducer运算过程中的相加总和。
         */
        @Override
        public AggregationBuffer getNewAggregationBuffer() throws HiveException {
            LetterSumAgg sum = new LetterSumAgg();
            reset(sum);
            return sum;
        }

        /**
         * mapreduce支持mapper和reducer的重用，所以为了兼容，也需要做内存的重用。
         */
        @Override
        public void reset(AggregationBuffer agg) throws HiveException {
            LetterSumAgg myagg = (LetterSumAgg) agg;
            myagg.sum = 0;
        }

        /**
         * map阶段调用，把保存当前和的对象agg，再加上输入的参数传入。
         */
        @Override
        public void iterate(AggregationBuffer agg, Object[] parameters)
                throws HiveException {
            if (parameters[0] != null) {
                LetterSumAgg myagg = (LetterSumAgg) agg;
                Object p1 = inputOI.getPrimitiveJavaObject(parameters[0]);
                myagg.add(String.valueOf(p1).length());
            }
        }

        /**
         * mapper 结束要返回的结果，还有 combiner 结束返回的结果
         */
        @Override
        public Object terminatePartial(AggregationBuffer agg) throws HiveException {
            return terminate(agg);
        }

        /**
         * combiner合并map返回的结果，还有reducer合并mapper或combiner返回的结果。
         */
        @Override
        public void merge(AggregationBuffer agg, Object partial)
                throws HiveException {
            if (partial != null) {

                LetterSumAgg myagg = (LetterSumAgg) agg;

                myagg.sum += PrimitiveObjectInspectorUtils.getInt(partial, integerOI);
            }
        }

        /**
         * reducer返回结果，或者是只有mapper，没有reducer时，在mapper端返回结果。
         */
        @Override
        public Object terminate(AggregationBuffer agg) throws HiveException {
            LetterSumAgg myagg = (LetterSumAgg) agg;
            result.set(myagg.sum);
            return result;
        }

    }
}