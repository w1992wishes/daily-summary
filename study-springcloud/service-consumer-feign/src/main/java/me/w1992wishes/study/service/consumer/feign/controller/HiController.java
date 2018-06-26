package me.w1992wishes.study.service.consumer.feign.controller;

import me.w1992wishes.study.service.consumer.feign.service.SchedualServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HiController {

    @Autowired
    SchedualServiceProvider schedualServiceProvider;

    @RequestMapping(value = "/hi",method = RequestMethod.GET)
    public String sayHi(@RequestParam String name){
        return schedualServiceProvider.sayHiFromProviderOne(name);
    }
}
