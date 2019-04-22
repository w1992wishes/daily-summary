package me.w1992wishes.sync.standard.runner;

import me.w1992wishes.common.exception.MyRuntimeException;
import me.w1992wishes.common.util.Configuration;
import me.w1992wishes.sync.standard.plugin.writer.Writer;
import me.w1992wishes.sync.standard.transport.exchanger.RecordReceiver;
import me.w1992wishes.sync.standard.plugin.writer.PostgreSQLWriter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author w1992wishes 2019/3/27 17:11
 */
public class WriterRunner extends AbstractRunner {

    private static final String VALUE_HOLDER = "?";

    private RecordReceiver recordReceiver;

    public WriterRunner(Configuration writerConfiguration, List<String> columns, List<String> specialColumns) {
        super(writerConfiguration, columns, specialColumns);
    }

    public void setRecordReceiver(RecordReceiver recordReceiver) {
        this.recordReceiver = recordReceiver;
    }

    @Override
    public void run() {

        List<String> fullColumns = fullColumns();
        String sqlTemplate = getWriteDataSqlTemplate(fullColumns);

        Writer writer = new PostgreSQLWriter(configuration, fullColumns, sqlTemplate);
        writer.startWrite(recordReceiver);
    }

    private String getWriteDataSqlTemplate(List<String> fullColumns) {
        if (fullColumns == null || fullColumns.isEmpty()) {
            throw new MyRuntimeException("插入没有指定 column");
        }

        List<String> valueHolders = new ArrayList<>();
        fullColumns.forEach(column -> valueHolders.add(VALUE_HOLDER));

        return "INSERT INTO %s (" +
                StringUtils.join(fullColumns, ",") +
                ") VALUES(" +
                StringUtils.join(valueHolders, ",") +
                ")";
    }

    private List<String> fullColumns() {
        List<String> fullColumns = new ArrayList<>();
        fullColumns.addAll(columns);
        fullColumns.addAll(specialColumns);
        return fullColumns;
    }

}
