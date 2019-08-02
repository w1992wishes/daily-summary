package me.w1992wishes.hbase.common.model;

import org.joda.time.DateTime;

/**
 * @author w1992wishes
 */
public abstract class Twit {

    public String user;
    public DateTime dt;
    public String text;

    @Override
    public String toString() {
        return String.format(
                "<Twit: %s %s %s>",
                user, dt, text);
    }
}