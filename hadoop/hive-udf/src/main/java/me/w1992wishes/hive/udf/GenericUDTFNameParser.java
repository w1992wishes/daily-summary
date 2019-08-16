package me.w1992wishes.hive.udf;

import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDTF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Administrator
 */
public class GenericUDTFNameParser extends GenericUDTF {

    private PrimitiveObjectInspector stringOI = null;

    @Override
    public StructObjectInspector initialize(ObjectInspector[] args) throws UDFArgumentException {

        if (args.length != 1) {
            throw new UDFArgumentException("GenericUDTFNameParser() takes exactly one argument");
        }

        if (args[0].getCategory() != ObjectInspector.Category.PRIMITIVE
                && ((PrimitiveObjectInspector) args[0]).getPrimitiveCategory() != PrimitiveObjectInspector.PrimitiveCategory.STRING) {
            throw new UDFArgumentException("GenericUDTFNameParser() takes a string as a parameter");
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