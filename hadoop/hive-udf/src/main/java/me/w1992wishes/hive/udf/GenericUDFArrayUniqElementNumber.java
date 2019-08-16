package me.w1992wishes.hive.udf;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ListObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorUtils;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.io.IntWritable;

/**
 * UDF:
 * Get number of objects with duplicate elements eliminated
 */
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
