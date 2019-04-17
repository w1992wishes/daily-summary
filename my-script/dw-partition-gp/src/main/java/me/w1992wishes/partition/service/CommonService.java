package me.w1992wishes.partition.service;

import me.w1992wishes.common.util.DateUtils;
import me.w1992wishes.partition.config.AppConfig;
import me.w1992wishes.partition.mapper.BaseMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

/**
 * @author w1992wishes 2019/3/21 15:11
 */
public abstract class CommonService implements BaseService {

    final Logger log = LogManager.getLogger(getClass());

    private static final String PARTITION_UNION_STR = "_1_prt_";

    BaseMapper mapper;

    @Autowired
    AppConfig appConfig;

    String getPartitionFullName(String table, String partiton){
        return table.replace("public.", "") + PARTITION_UNION_STR + partiton;
    }

    String getPartitionName(LocalDateTime dateTime) {
        return "p".concat(DateUtils.dateTimeToStr(dateTime, DateUtils.DF_YMD_NO_LINE));
    }

    String getPartitionTime(LocalDateTime dateTime) {
        return DateUtils.dateTimeToStr(dateTime, DateUtils.DF_YMD);
    }

    @Override
    public void createEventFaceTables() {
        createEventFaceTables(LocalDateTime.now());
    }

    @Override
    public void addOneDayPartitions() {
        addOneDayPartitions(LocalDateTime.now());
    }

    @Override
    public void addDayPartitions(LocalDateTime startDate, int days) {
        for (int i = 0; i < days; i++) {
            addOneDayPartitions(startDate.plusDays(i));
        }
    }

    @Override
    public void addDayPartitions(int days) {
        addDayPartitions(LocalDateTime.now(), days);
    }

    @Override
    public boolean checkTableExist(String tableFullName){
        return mapper.checkTableExist(tableFullName) > 0;
    }

    public void setMapper(BaseMapper mapper) {
        this.mapper = mapper;
    }
}

