package me.w1992wishes.sync.standard.transport.record;

import me.w1992wishes.sync.standard.element.Column;

/**
 * 作为标示 生产者已经完成生产的标志
 *
 * @author w1992wishes 2019/3/26 17:48
 */
public class TerminateRecord implements Record {

	private static final TerminateRecord SINGLE = new TerminateRecord();

	private TerminateRecord() {
	}

	public static TerminateRecord get() {
		return SINGLE;
	}

	@Override
	public void addColumn(Column column) {

    }

	@Override
	public Column getColumn(int i) {
		return null;
	}

	@Override
	public int getColumnNumber() {
		return 0;
	}

	@Override
	public void setColumn(int i, Column column) {

    }
}
