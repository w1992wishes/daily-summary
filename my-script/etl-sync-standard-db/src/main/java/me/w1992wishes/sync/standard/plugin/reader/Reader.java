package me.w1992wishes.sync.standard.plugin.reader;

import me.w1992wishes.sync.standard.handler.ColumnHandler;
import me.w1992wishes.sync.standard.transport.exchanger.RecordSender;

/**
 * @author w1992wishes 2019/3/29 13:44
 */
public interface Reader {

    void startRead(RecordSender recordSender);

    void registerColumnHandler(String columnName, ColumnHandler columnHandler);

}
