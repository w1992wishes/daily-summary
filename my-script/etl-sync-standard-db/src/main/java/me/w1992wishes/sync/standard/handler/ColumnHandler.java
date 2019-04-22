package me.w1992wishes.sync.standard.handler;

import me.w1992wishes.sync.standard.element.Column;

/**
 * @author w1992wishes 2019/4/1 9:45
 */
public interface ColumnHandler {

    /**
     * 将当前 column 处理后返回新的 column
     */
    Column getColumnValue(Column column);

}
