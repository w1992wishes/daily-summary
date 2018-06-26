package me.w1992wishes.study.service.consumer.feign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Description study-records
 * @Author w1992wishes
 * @Date 2018/6/26 13:50
 * @Version 1.0
 */
@SpringBootApplication
@EnableFeignClients
public class ServiceConsumerFeignApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceConsumerFeignApplication.class, args);
    }

}
