package me.w1992wishes.partition.service;

import me.w1992wishes.partition.mapper.odl.OdlMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author w1992wishes 2019/3/21 14:43
 */
@Service("odlService")
public class OdlServiceImpl extends CommonService {

    @Autowired
    public OdlServiceImpl(OdlMapper odlMapper) {
        this.mapper = odlMapper;
    }

    @Override
    public void createEventFaceTables(LocalDateTime startDate) {
        String partitionName = getPartitionName(startDate);
        String partitionTime = getPartitionTime(startDate);
        String endPartitionTime = getPartitionTime(startDate.plusDays(1));
        mapper.createEventFaceTable(appConfig.getEventFaceTable(), partitionName, partitionTime, endPartitionTime);
        mapper.createEventFaceTable(appConfig.getEventFaceTableRefreshed(), partitionName, partitionTime, endPartitionTime);
    }

    @Override
    public void deleteTables() {
        mapper.deleteTable(appConfig.getEventFaceTable());
        mapper.deleteTable(appConfig.getEventFaceTableRefreshed());
    }

    @Override
    public void addOneDayPartitions(LocalDateTime startDate) {
        String partitionName = getPartitionName(startDate);
        String partitionTime = getPartitionTime(startDate);
        String endPartitionTime = getPartitionTime(startDate.plusDays(1));

        // odl_event_face
        String eventFaceTable = appConfig.getEventFaceTable();
        String partitionFullName = getPartitionFullName(eventFaceTable, partitionName);
        if (!checkTableExist(partitionFullName)) {
            mapper.addPartitionByTimeRange(eventFaceTable, partitionName, partitionTime, endPartitionTime);
            log.info("======> {} addOneDayPartitions[{} {}] finished", appConfig.getEventFaceTable(), partitionName, partitionTime);
        } else {
            log.info("======> {} exist", partitionFullName);
        }

        // odl_event_face_refreshed
        String eventFaceTableRefreshed = appConfig.getEventFaceTableRefreshed();
        partitionFullName = getPartitionFullName(eventFaceTableRefreshed, partitionName);
        if (!checkTableExist(partitionFullName)) {
            mapper.addPartitionByTimeRange(eventFaceTableRefreshed, partitionName, partitionTime, endPartitionTime);
            log.info("======> {} addOneDayPartitions[{} {}] finished", appConfig.getEventFaceTableRefreshed(), partitionName, partitionTime);
        } else {
            log.info("======> {} exist", partitionFullName);
        }
    }
}