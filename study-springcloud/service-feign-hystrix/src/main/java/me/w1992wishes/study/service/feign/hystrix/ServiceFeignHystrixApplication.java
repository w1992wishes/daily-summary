package me.w1992wishes.study.service.feign.hystrix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableHystrix
public class ServiceFeignHystrixApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceFeignHystrixApplication.class, args);
    }
}
