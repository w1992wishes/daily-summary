package me.w1992wishes.hive.udf;

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