package me.w1992wishes.study.springboot.jpa;

import me.w1992wishes.study.springboot.jpa.entity.User;
import org.junit.Assert;
import org.junit.Test;

/**
 * @Description study-records
 * @Author w1992wishes
 * @Date 2018/6/21 18:09
 * @Version 1.0
 */
public class UserServiceImplTest extends AbstractServiceTest {

    @Test
    public void saveUser(){
        User user = generateUser("alibaba", "alibaba");
        User newUser = userService.save(user);
        Assert.assertEquals(user.getUsername(), newUser.getUsername());
    }

}
