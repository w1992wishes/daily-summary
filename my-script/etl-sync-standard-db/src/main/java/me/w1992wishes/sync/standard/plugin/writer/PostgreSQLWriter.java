package me.w1992wishes.sync.standard.plugin.writer;

import me.w1992wishes.common.constant.DataBaseType;
import me.w1992wishes.common.util.Configuration;

import java.util.List;

/**
 * @author w1992wishes 2019/3/28 19:45
 */
public class PostgreSQLWriter extends CommonWriter {

    public PostgreSQLWriter(Configuration configuration, List<String> columns, String sqlTemplate) {
        super(configuration, columns, sqlTemplate);

        this.dataBaseType = DataBaseType.PostgreSQL;
    }

}
