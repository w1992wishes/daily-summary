package me.w1992wishes.dwpartition;

import me.w1992wishes.dwpartition.service.BaseService;
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

    public static void main(String[] args) {
        SpringApplication.run(PartitionGpApp.class, args);
    }

    @Override
    public void run(String... args) {
        odlService.addOneDayPartitions(LocalDateTime.now().plusDays(1));
        dwdService.addOneDayPartitions(LocalDateTime.now().plusDays(1));
    }
}
