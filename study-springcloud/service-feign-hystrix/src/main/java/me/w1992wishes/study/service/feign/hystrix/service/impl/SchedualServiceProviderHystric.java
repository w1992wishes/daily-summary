package me.w1992wishes.study.service.feign.hystrix.service.impl;

import me.w1992wishes.study.service.feign.hystrix.service.SchedualServiceProvider;
import org.springframework.stereotype.Service;

/**
 * @Description study-records
 * @Author w1992wishes
 * @Date 2018/6/27 11:52
 * @Version 1.0
 */
@Service
public class SchedualServiceProviderHystric implements SchedualServiceProvider {
    @Override
    public String sayHiFromProviderOne(String name) {
        return "sorry "+name;
    }
}
