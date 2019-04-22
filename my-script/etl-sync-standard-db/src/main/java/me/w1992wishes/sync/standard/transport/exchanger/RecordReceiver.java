package me.w1992wishes.sync.standard.transport.exchanger;

import me.w1992wishes.common.exception.MyException;
import me.w1992wishes.sync.standard.transport.record.Record;

public interface RecordReceiver {

	Record getFromReader() throws MyException;

	public void flush() ;
}
