package me.w1992wishes.sync.standard.element;

import com.alibaba.fastjson.JSON;
import me.w1992wishes.common.exception.MyException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * @author w1992wishes 2019/3/29 13:45
 */
public abstract class Column {

    private Type type;

    private Object rawData;

    private int byteSize;

    public Column(final Object object, final Type type, int byteSize) {
        this.rawData = object;
        this.type = type;
        this.byteSize = byteSize;
    }

    public Object getRawData() {
        return this.rawData;
    }

    public Type getType() {
        return this.type;
    }

    public int getByteSize() {
        return this.byteSize;
    }

    protected void setType(Type type) {
        this.type = type;
    }

    protected void setRawData(Object rawData) {
        this.rawData = rawData;
    }

    protected void setByteSize(int byteSize) {
        this.byteSize = byteSize;
    }

    public abstract Long asLong() throws MyException;

    public abstract Double asDouble() throws MyException;

    public abstract String asString() throws MyException;

    public abstract Date asDate() throws MyException;

    public abstract byte[] asBytes() throws MyException;

    public abstract Boolean asBoolean() throws MyException;

    public abstract BigDecimal asBigDecimal() throws MyException;

    public abstract BigInteger asBigInteger() throws MyException;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    public enum Type {
        BAD, NULL, INT, LONG, DOUBLE, STRING, BOOL, DATE, BYTES
    }
}
