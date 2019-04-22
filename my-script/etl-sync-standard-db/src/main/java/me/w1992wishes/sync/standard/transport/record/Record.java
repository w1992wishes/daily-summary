package me.w1992wishes.sync.standard.transport.record;

import me.w1992wishes.sync.standard.element.Column;

/**
 * @author w1992wishes 2019/3/26 17:48
 */
public interface Record {

	void addColumn(Column column);

	void setColumn(int i, final Column column);

	Column getColumn(int i);

	String toString();

	int getColumnNumber();

}
