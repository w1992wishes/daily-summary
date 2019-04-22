package me.w1992wishes.sync.standard.handler;

import me.w1992wishes.sync.standard.element.Column;
import me.w1992wishes.sync.standard.element.DateColumn;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 将当前需要特殊处理的 Timestamp 列值设置为当前时间
 *
 * @author w1992wishes 2019/4/1 9:47
 */
public class DefaultCurrentTimeHandler implements ColumnHandler {

    public Column getColumnValue(Column column) {
        return new DateColumn(Timestamp.valueOf(LocalDateTime.now().withNano(0)));
    }

}
