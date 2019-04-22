package me.w1992wishes.sync.standard.plugin.writer;

import me.w1992wishes.sync.standard.transport.exchanger.RecordReceiver;

/**
 * @author w1992wishes 2019/3/29 14:54
 */
public interface Writer {

    void startWrite(RecordReceiver recordReceiver);

}
