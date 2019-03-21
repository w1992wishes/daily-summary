package me.w1992wishes.dwpartition.service;

import me.w1992wishes.common.util.DateUtils;
import me.w1992wishes.dwpartition.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

/**
 * @author w1992wishes 2019/3/21 15:11
 */
public abstract class CommonService implements BaseService {

    @Autowired
    AppConfig appConfig;

    String getPartitionName(LocalDateTime dateTime) {
        return "p".concat(DateUtils.dateTimeToStr(dateTime, DateUtils.DF_YMD_NO_LINE));
    }

    String getPartitionTime(LocalDateTime dateTime) {
        return DateUtils.dateTimeToStr(dateTime, DateUtils.DF_YMD);
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
}
