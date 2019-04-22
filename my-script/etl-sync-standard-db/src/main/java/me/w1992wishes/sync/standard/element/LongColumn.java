package me.w1992wishes.sync.standard.element;

import me.w1992wishes.common.exception.MyException;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * @author w1992wishes 2019/3/29 13:45
 */
public class LongColumn extends Column {

    /**
     * 从整形字符串表示转为LongColumn，支持Java科学计数法
     *
     * NOTE: <br>
     * 如果data为浮点类型的字符串表示，数据将会失真，请使用DoubleColumn对接浮点字符串
     *
     * */
    public LongColumn(final String data) throws MyException {
        super(null, Column.Type.LONG, 0);
        if (null == data) {
            return;
        }

        try {
            BigInteger rawData = NumberUtils.createBigDecimal(data)
                    .toBigInteger();
            super.setRawData(rawData);

            // 当 rawData 为[0-127]时，rawData.bitLength() < 8，导致其 byteSize = 0，简单起见，直接认为其长度为 data.length()
            // super.setByteSize(rawData.bitLength() / 8);
            super.setByteSize(data.length());
        } catch (Exception e) {
            throw new MyException(String.format("String[%s]不能转为Long .", data));
        }
    }

    public LongColumn(Long data) {
        this(null == data ? (BigInteger) null : BigInteger.valueOf(data));
    }

    public LongColumn(Integer data) {
        this(null == data ? (BigInteger) null : BigInteger.valueOf(data));
    }

    public LongColumn(BigInteger data) {
        this(data, null == data ? 0 : 8);
    }

    private LongColumn(BigInteger data, int byteSize) {
        super(data, Column.Type.LONG, byteSize);
    }

    public LongColumn() {
        this((BigInteger) null);
    }

    @Override
    public BigInteger asBigInteger() {
        if (null == this.getRawData()) {
            return null;
        }

        return (BigInteger) this.getRawData();
    }

    @Override
    public Long asLong() {
        BigInteger rawData = (BigInteger) this.getRawData();
        if (null == rawData) {
            return null;
        }

        return rawData.longValue();
    }

    @Override
    public Double asDouble() {
        if (null == this.getRawData()) {
            return null;
        }

        BigDecimal decimal = this.asBigDecimal();

        return decimal.doubleValue();
    }

    @Override
    public Boolean asBoolean() {
        if (null == this.getRawData()) {
            return null;
        }

        return this.asBigInteger().compareTo(BigInteger.ZERO) != 0 ? true
                : false;
    }

    @Override
    public BigDecimal asBigDecimal() {
        if (null == this.getRawData()) {
            return null;
        }

        return new BigDecimal(this.asBigInteger());
    }

    @Override
    public String asString() {
        if (null == this.getRawData()) {
            return null;
        }
        return ((BigInteger) this.getRawData()).toString();
    }

    @Override
    public Date asDate() {
        if (null == this.getRawData()) {
            return null;
        }
        return new Date(this.asLong());
    }

    @Override
    public byte[] asBytes() throws MyException {
        throw new MyException("Long类型不能转为Bytes .");
    }

}
