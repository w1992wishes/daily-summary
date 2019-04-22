package me.w1992wishes.sync.standard.container;

import me.w1992wishes.common.exception.MyRuntimeException;
import me.w1992wishes.common.util.Configuration;
import me.w1992wishes.sync.standard.constant.Key;
import me.w1992wishes.sync.standard.element.ColumnCast;
import me.w1992wishes.sync.standard.runner.AbstractRunner;
import me.w1992wishes.sync.standard.runner.ReaderRunner;
import me.w1992wishes.sync.standard.runner.WriterRunner;
import me.w1992wishes.sync.standard.transport.exchanger.BufferedRecordExchanger;
import me.w1992wishes.sync.standard.transport.channel.Channel;
import me.w1992wishes.sync.standard.transport.channel.memory.MemoryChannel;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author w1992wishes 2019/3/27 16:50
 */
public class TaskGroupContainer extends AbstractContainer {

    public TaskGroupContainer(Configuration configuration) {
        super(configuration);
    }

    @Override
    public void start() {

        // 绑定column转换信息
        ColumnCast.bind(configuration);

        // 获取源字段、sink 源字段
        Map<String, Object> columnMapping = configuration.getMap(Key.COLUMNS_MAPPING);
        if (columnMapping == null) {
            throw new MyRuntimeException("columns_mapping 配置为空，无法继续");
        }
        List<String> sinkColumns = new ArrayList<>();
        List<String> sourceColumns = new ArrayList<>();
        for (Map.Entry<String, Object> entry : columnMapping.entrySet()) {
            String key = entry.getKey();
            String value = String.valueOf(entry.getValue());
            if (StringUtils.isNotEmpty(value.trim())) {
                sinkColumns.add(key);
                sourceColumns.add(value);
            }
        }

        // 获取要特殊处理的列及处理 handler
        Map<String, Object> specialColMap = configuration.getMap(Key.SPECIAL_COLUMNS);
        List<String> specialColumns = new ArrayList<>();
        List<String> specialHandlers = new ArrayList<>();
        if (specialColMap != null) {
            for (Map.Entry<String, Object> specialColEntry : specialColMap.entrySet()) {
                specialColumns.add(specialColEntry.getKey());
                specialHandlers.add(String.valueOf(specialColEntry.getValue()));
            }
        }

        if (!sinkColumns.isEmpty()) {
            Channel channel = new MemoryChannel(configuration);
            AbstractRunner readerRunner = new ReaderRunner(configuration, sourceColumns, specialColumns);
            ((ReaderRunner) readerRunner).setRecordSender(new BufferedRecordExchanger(channel));
            ((ReaderRunner) readerRunner).setSpecialHandlers(specialHandlers);
            new Thread(readerRunner).start();

            AbstractRunner writerRunner = new WriterRunner(configuration, sinkColumns, specialColumns);
            ((WriterRunner) writerRunner).setRecordReceiver(new BufferedRecordExchanger(channel));
            new Thread(writerRunner).start();
        }
    }
}
