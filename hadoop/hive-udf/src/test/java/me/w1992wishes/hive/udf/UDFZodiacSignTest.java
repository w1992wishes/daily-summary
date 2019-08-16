package me.w1992wishes.hive.udf;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author w1992wishes 2019/8/15 9:49
 */
public class UDFZodiacSignTest {

    @Test
    public void testUDFZodiacSign() {
        UDFZodiacSign example = new UDFZodiacSign();
        Assert.assertEquals("魔蝎座", example.evaluate(1, 1));
        Assert.assertEquals("魔蝎座", example.evaluate("2019-01-01"));
    }

}
