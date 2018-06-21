package me.w1992wishes.study.springboot.jpa;

import me.w1992wishes.study.springboot.jpa.entity.User;
import me.w1992wishes.study.springboot.jpa.service.UserService;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = {"classpath:application.yml"})
@SpringBootTest
public abstract class AbstractServiceTest {

    @Autowired
    protected UserService userService;

    protected User generateUser(String username, String userpwd){
        User user = new User();
        user.setUserId(1);
        user.setUsername(username);
        user.setUserpwd(userpwd);
        return user;
    }
}