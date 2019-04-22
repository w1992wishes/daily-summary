package me.w1992wishes.sync.standard.element;

import me.w1992wishes.common.exception.MyException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * @author w1992wishes 2019/3/29 13:45
 */
public class StringColumn extends Column {

    public StringColumn() {
        this((String) null);
    }

    public StringColumn(final String rawData) {
        super(rawData, Column.Type.STRING, (null == rawData ? 0 : rawData
                .length()));
    }

    @Override
    public String asString() {
        if (null == this.getRawData()) {
            return null;
        }

        return (String) this.getRawData();
    }

    private void validateDoubleSpecific(final String data) throws MyException {
        if ("NaN".equals(data) || "Infinity".equals(data)
                || "-Infinity".equals(data)) {
            throw new MyException(
                    String.format("String[\"%s\"]属于Double特殊类型，不能转为其他类型 .", data));
        }

        return;
    }

    @Override
    public BigInteger asBigInteger() throws MyException {
        if (null == this.getRawData()) {
            return null;
        }

        this.validateDoubleSpecific((String) this.getRawData());

        try {
            return this.asBigDecimal().toBigInteger();
        } catch (Exception e) {
            throw new MyException(String.format(
                            "String[\"%s\"]不能转为BigInteger .", this.asString()));
        }
    }

    @Override
    public Long asLong() throws MyException {
        if (null == this.getRawData()) {
            return null;
        }

        this.validateDoubleSpecific((String) this.getRawData());

        try {
            BigInteger integer = this.asBigInteger();
            return integer.longValue();
        } catch (Exception e) {
            throw new MyException(
                    String.format("String[\"%s\"]不能转为Long .", this.asString()));
        }
    }

    @Override
    public BigDecimal asBigDecimal() throws MyException {
        if (null == this.getRawData()) {
            return null;
        }

        this.validateDoubleSpecific((String) this.getRawData());

        try {
            return new BigDecimal(this.asString());
        } catch (Exception e) {
            throw new MyException(String.format("String [\"%s\"] 不能转为BigDecimal .", this.asString()));
        }
    }

    @Override
    public Double asDouble() throws MyException {
        if (null == this.getRawData()) {
            return null;
        }

        String data = (String) this.getRawData();
        if ("NaN".equals(data)) {
            return Double.NaN;
        }

        if ("Infinity".equals(data)) {
            return Double.POSITIVE_INFINITY;
        }

        if ("-Infinity".equals(data)) {
            return Double.NEGATIVE_INFINITY;
        }

        BigDecimal decimal = this.asBigDecimal();

        return decimal.doubleValue();
    }

    @Override
    public Boolean asBoolean() throws MyException {
        if (null == this.getRawData()) {
            return null;
        }

        if ("true".equalsIgnoreCase(this.asString())) {
            return true;
        }

        if ("false".equalsIgnoreCase(this.asString())) {
            return false;
        }

        throw new MyException(
                String.format("String[\"%s\"]不能转为Bool .", this.asString()));
    }

    @Override
    public Date asDate() throws MyException {
        try {
            return ColumnCast.string2Date(this);
        } catch (Exception e) {
            throw new MyException(
                    String.format("String[\"%s\"]不能转为Date .", this.asString()));
        }
    }

    @Override
    public byte[] asBytes() throws MyException {
        try {
            return ColumnCast.string2Bytes(this);
        } catch (Exception e) {
            throw new MyException(String.format("String[\"%s\"]不能转为Bytes .", this.asString()));
        }
    }
}
