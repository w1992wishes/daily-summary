package me.w1992wishes.dwpartition.service;

import me.w1992wishes.dwpartition.mapper.dwd.DwdMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author w1992wishes 2019/3/21 17:30
 */
@Service("dwdService")
public class DwdServiceImpl extends CommonService {

    private final DwdMapper dwdMapper;

    @Autowired
    public DwdServiceImpl(DwdMapper dwdMapper) {
        this.dwdMapper = dwdMapper;
    }

    @Override
    public void createEventFaceTables() {
        String partitionName = getPartitionName(LocalDateTime.now());
        String partitionTime = getPartitionTime(LocalDateTime.now());
        String endPartitionTime = getPartitionTime(LocalDateTime.now().plusDays(1));
        dwdMapper.createEventFaceTable(appConfig.getEventFaceTablePreprocessed(), partitionName, partitionTime, endPartitionTime);
    }

    @Override
    public void deleteTables() {
        dwdMapper.deleteTable(appConfig.getEventFaceTablePreprocessed());
    }

    @Override
    public void addOneDayPartitions(LocalDateTime startDate) {
        String partitionName = getPartitionName(startDate);
        String partitionTime = getPartitionTime(startDate);
        String endPartitionTime = getPartitionTime(startDate.plusDays(1));
        // odl_event_face_preprocessed
        dwdMapper.addPartitionByTimeRange(appConfig.getEventFaceTablePreprocessed(), partitionName, partitionTime, endPartitionTime);
    }
}
