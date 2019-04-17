package me.w1992wishes.partition;

import me.w1992wishes.common.util.RetryUtil;
import me.w1992wishes.partition.config.AppConfig;
import me.w1992wishes.partition.service.BaseService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 主程序
 *
 * @author w1992wishes 2019/3/20 14:00
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class PartitionGpApp implements CommandLineRunner {

    @Resource(name = "odlService")
    private BaseService odlService;

    @Resource(name = "dwdService")
    private BaseService dwdService;

    @Resource
    private AppConfig appConfig;

    public static void main(String[] args) {
        SpringApplication.run(PartitionGpApp.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (appConfig.getEnableCreateEventTable()) {
            // 从昨天创建分区表
            odlService.createEventFaceTables(LocalDateTime.now().minusDays(1));
            dwdService.createEventFaceTables(LocalDateTime.now().minusDays(1));
            // 增加当天的分区
            odlService.addOneDayPartitions(LocalDateTime.now());
            dwdService.addOneDayPartitions(LocalDateTime.now());
        }
        // 定时增加往后的分区
        RetryUtil.executeWithRetry(() -> {
            odlService.addDayPartitions(LocalDateTime.now(), appConfig.getPartitionNumOneDay());
            return true;
        }, 12, 10000, true);
        RetryUtil.executeWithRetry(() -> {
            dwdService.addDayPartitions(LocalDateTime.now(), appConfig.getPartitionNumOneDay());
            return true;
        }, 12, 10000, true);
    }
}

