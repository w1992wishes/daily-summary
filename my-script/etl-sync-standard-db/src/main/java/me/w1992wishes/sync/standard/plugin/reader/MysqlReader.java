package me.w1992wishes.sync.standard.plugin.reader;


import me.w1992wishes.common.constant.DataBaseType;
import me.w1992wishes.common.util.Configuration;

import java.util.List;

/**
 * @author w1992wishes 2019/3/27 19:14
 */
public class MysqlReader extends CommonReader {

    public MysqlReader(Configuration configuration, String querySqlTemplate, List<String> specialColumns) {
        super(configuration, querySqlTemplate, specialColumns);

        this.dataBaseType = DataBaseType.MySql;
    }
}
