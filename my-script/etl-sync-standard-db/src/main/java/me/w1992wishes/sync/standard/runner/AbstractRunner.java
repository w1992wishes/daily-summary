package me.w1992wishes.sync.standard.runner;

import me.w1992wishes.common.util.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * @author w1992wishes 2019/3/27 17:10
 */
public abstract class AbstractRunner implements Runnable {

    final Logger log = LogManager.getLogger(getClass());

    // 数据列
    List<String> columns;
    // 特殊处理列
    List<String> specialColumns;
    // 包含所有的配置
    Configuration configuration;

    AbstractRunner(Configuration configuration, List<String> columns, List<String> specialColumns) {
        this.columns = columns;
        this.configuration = configuration;
        this.specialColumns = specialColumns;
    }

}
