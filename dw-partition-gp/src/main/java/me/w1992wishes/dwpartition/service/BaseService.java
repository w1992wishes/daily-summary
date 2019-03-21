package me.w1992wishes.dwpartition.service;

import java.time.LocalDateTime;

/**
 * @author w1992wishes 2019/3/21 14:43
 */
public interface BaseService {

    /**
     * 创建事件表
     */
    void createEventFaceTables();

    /**
     * 根据业务 code 和算法 version 删除事件表
     */
    void deleteTables();

    /**
     * 从开始时间起，新增 days 天的分区，默认从当天时间开始
     */
    void addDayPartitions(int days);

    /**
     * 从开始时间起，新增 days 天的分区
     */
    void addDayPartitions(LocalDateTime startDate, int days);

    /**
     * 增加一个一天的分区
     */
    void addOneDayPartitions(LocalDateTime startDate);

    /**
     * 增加一个一天的分区，默认是当前日期
     */
    void addOneDayPartitions();
}
