package me.w1992wishes.partition.service;

import me.w1992wishes.partition.mapper.dwd.DwdMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author w1992wishes 2019/3/21 17:30
 */
@Service("dwdService")
public class DwdServiceImpl extends CommonService {

    @Autowired
    public DwdServiceImpl(DwdMapper dwdMapper) {
        this.mapper = dwdMapper;
    }

    @Override
    public void createEventFaceTables(LocalDateTime startDate) {
        String partitionName = getPartitionName(startDate);
        String partitionTime = getPartitionTime(startDate);
        String endPartitionTime = getPartitionTime(startDate.plusDays(1));
        mapper.createEventFaceTable(appConfig.getEventFaceTablePreprocessed(), partitionName, partitionTime, endPartitionTime);
    }

    @Override
    public void deleteTables() {
        mapper.deleteTable(appConfig.getEventFaceTablePreprocessed());
    }

    @Override
    public void addOneDayPartitions(LocalDateTime startDate) {
        String partitionName = getPartitionName(startDate);
        String partitionTime = getPartitionTime(startDate);
        String endPartitionTime = getPartitionTime(startDate.plusDays(1));
        // odl_event_face_preprocessed
        String table = appConfig.getEventFaceTablePreprocessed();
        String partitionFullName = getPartitionFullName(table, partitionName);
        if (!checkTableExist(partitionFullName)) {
            mapper.addPartitionByTimeRange(table, partitionName, partitionTime, endPartitionTime);
            log.info("======> {} addOneDayPartitions[{} {}] finished", appConfig.getEventFaceTablePreprocessed(), partitionName, partitionTime);
        } else {
            log.info("======> {} exist", partitionFullName);
        }
    }
}
