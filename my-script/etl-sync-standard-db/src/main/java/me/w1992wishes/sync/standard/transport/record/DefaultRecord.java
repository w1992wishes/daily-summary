package me.w1992wishes.sync.standard.transport.record;

import com.alibaba.fastjson.JSON;
import me.w1992wishes.common.exception.MyRuntimeException;
import me.w1992wishes.sync.standard.element.Column;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author w1992wishes 2019/3/26 17:48
 */
public class DefaultRecord implements Record {

    private static final int RECORD_AVERGAE_COLUMN_NUMBER = 16;

    private List<Column> columns;

    public DefaultRecord() {
        this.columns = new ArrayList<>(RECORD_AVERGAE_COLUMN_NUMBER);
    }

    @Override
    public void addColumn(Column column) {
        columns.add(column);
    }

    @Override
    public Column getColumn(int i) {
        if (i < 0 || i >= columns.size()) {
            return null;
        }
        return columns.get(i);
    }

    @Override
    public void setColumn(int i, final Column column) {
        if (i < 0) {
            throw new MyRuntimeException(
                    "不能给index小于0的column设置值");
        }

        if (i >= columns.size()) {
            expandCapacity(i + 1);
        }

        this.columns.set(i, column);
    }

    @Override
    public String toString() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("size", this.getColumnNumber());
        json.put("data", this.columns);
        return JSON.toJSONString(json);
    }

    @Override
    public int getColumnNumber() {
        return this.columns.size();
    }

    private void expandCapacity(int totalSize) {
        if (totalSize <= 0) {
            return;
        }

        int needToExpand = totalSize - columns.size();
        while (needToExpand-- > 0) {
            this.columns.add(null);
        }
    }

}
