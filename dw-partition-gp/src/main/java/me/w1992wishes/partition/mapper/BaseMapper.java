package me.w1992wishes.partition.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 基础 mapper
 *
 * @author w1992wishes 2019/3/21 9:40
 */
public interface BaseMapper {

    /**
     * 创建事件表
     */
    void createEventFaceTable(@Param("tableName") String tableName,
                              @Param("partitionName") String partitionName,
                              @Param("partitionTime") String partitionTime,
                              @Param("endPartitionTime") String endPartitionTime);

    /**
     * 删除事件表
     */
    void deleteTable(@Param("tableName") String tableName);

    /**
     * 根据时间增加分区表
     */
    void addPartitionByTimeRange(@Param("tableName") String tableName,
                                 @Param("partitionName") String partitionName,
                                 @Param("partitionTime") String partitionTime,
                                 @Param("endPartitionTime") String endPartitionTime);

    /**
     * 判断表是否存在（分区需写全名）
     */
    int checkTableExist(@Param("tableFullName") String tableFullName);
}
