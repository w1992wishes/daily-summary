package me.w1992wishes.study.service.consumer.feign.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "service-provider")
public interface SchedualServiceProvider {

    @RequestMapping(value = "/hi",method = RequestMethod.GET)
    String sayHiFromProviderOne(@RequestParam(value = "name") String name);

}