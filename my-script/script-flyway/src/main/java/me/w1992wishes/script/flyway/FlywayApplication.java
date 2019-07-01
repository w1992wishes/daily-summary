package me.w1992wishes.script.flyway;

import me.w1992wishes.script.flyway.config.FlywayConfig;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.Resource;
import java.io.FileNotFoundException;

/**
 * 启动类
 */
@SpringBootApplication
public class FlywayApplication implements CommandLineRunner {

    @Resource
    private FlywayConfig config;

    @Resource
    private ApplicationArguments arguments;

    public static void main(String[] args) {
        SpringApplication.run(FlywayApplication.class, args);
    }

    @Override
    public void run(String... args) throws FileNotFoundException {
        config.migrate();
    }

}
