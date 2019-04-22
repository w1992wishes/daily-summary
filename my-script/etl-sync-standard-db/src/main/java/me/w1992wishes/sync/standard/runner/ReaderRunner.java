package me.w1992wishes.sync.standard.runner;

import me.w1992wishes.common.exception.MyRuntimeException;
import me.w1992wishes.common.util.Configuration;
import me.w1992wishes.sync.standard.handler.ColumnHandler;
import me.w1992wishes.sync.standard.plugin.reader.Reader;
import me.w1992wishes.sync.standard.transport.exchanger.RecordSender;
import me.w1992wishes.sync.standard.plugin.reader.MysqlReader;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author w1992wishes 2019/3/27 17:10
 */
public class ReaderRunner extends AbstractRunner {

    private List<String> specialHandlers;

    private RecordSender recordSender;

    public ReaderRunner(Configuration configuration, List<String> columns, List<String> specialColumns) {
        super(configuration, columns, specialColumns);
    }

    @Override
    public void run() {
        // query sql template
        String querySqlTemplate = getReadDataSqlTemplate();

        Reader reader = new MysqlReader(configuration, querySqlTemplate, specialColumns);
        try {
            for (int i = 0; i < specialColumns.size(); i++) {
                Class<?> clazz = Class.forName(specialHandlers.get(i));
                ColumnHandler columnHandler = (ColumnHandler) clazz.newInstance();
                reader.registerColumnHandler(specialColumns.get(i), columnHandler);
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new MyRuntimeException("ReaderRunner instance ColumnHandler encounter exception", e);
        }

        reader.startRead(recordSender);
    }

    public void setRecordSender(RecordSender recordSender) {
        this.recordSender = recordSender;
    }

    public void setSpecialHandlers(List<String> specialHandlers) {
        this.specialHandlers = specialHandlers;
    }

    // query sql
    private String getReadDataSqlTemplate() {
        return "SELECT " + StringUtils.join(columns, ",") + " FROM %s";
    }

}
