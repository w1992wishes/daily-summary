package me.w1992wishes.dwpartition.service;

import me.w1992wishes.dwpartition.mapper.odl.OdlMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author w1992wishes 2019/3/21 14:43
 */
@Service("odlService")
public class OdlServiceImpl extends CommonService {

    private final OdlMapper odlMapper;

    @Autowired
    public OdlServiceImpl(OdlMapper odlMapper) {
        this.odlMapper = odlMapper;
    }

    @Override
    public void createEventFaceTables() {
        String partitionName = getPartitionName(LocalDateTime.now());
        String partitionTime = getPartitionTime(LocalDateTime.now());
        String endPartitionTime = getPartitionTime(LocalDateTime.now().plusDays(1));
        odlMapper.createEventFaceTable(appConfig.getEventFaceTable(), partitionName, partitionTime, endPartitionTime);
        odlMapper.createEventFaceTable(appConfig.getEventFaceTableRefreshed(), partitionName, partitionTime, endPartitionTime);
    }

    @Override
    public void deleteTables() {
        odlMapper.deleteTable(appConfig.getEventFaceTable());
        odlMapper.deleteTable(appConfig.getEventFaceTableRefreshed());
    }

    @Override
    public void addOneDayPartitions(LocalDateTime startDate) {
        String partitionName = getPartitionName(startDate);
        String partitionTime = getPartitionTime(startDate);
        String endPartitionTime = getPartitionTime(startDate.plusDays(1));
        // odl_event_face
        odlMapper.addPartitionByTimeRange(appConfig.getEventFaceTable(), partitionName, partitionTime, endPartitionTime);
        // odl_event_face_refreshed
        odlMapper.addPartitionByTimeRange(appConfig.getEventFaceTableRefreshed(), partitionName, partitionTime, endPartitionTime);
    }
}
