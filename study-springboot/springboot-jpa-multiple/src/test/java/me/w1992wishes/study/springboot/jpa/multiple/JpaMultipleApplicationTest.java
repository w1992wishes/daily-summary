package me.w1992wishes.study.springboot.jpa.multiple;

import me.w1992wishes.study.springboot.jpa.multiple.test1.entity.User;
import me.w1992wishes.study.springboot.jpa.multiple.test1.repository.UserRepository;
import me.w1992wishes.study.springboot.jpa.multiple.test2.entity.Message;
import me.w1992wishes.study.springboot.jpa.multiple.test2.repository.MessageRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @Description study-records
 * @Author w1992wishes
 * @Date 2018/6/22 16:50
 * @Version 1.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = {"classpath:application.yml"})
@SpringBootTest
public class JpaMultipleApplicationTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MessageRepository messageRepository;

    @Test
    public void test() throws Exception {

        userRepository.save(new User("aaa", 10));
        userRepository.save(new User("bbb", 20));
        userRepository.save(new User("ccc", 30));
        userRepository.save(new User("ddd", 40));
        userRepository.save(new User("eee", 50));

        Assert.assertEquals(5, userRepository.findAll().size());

        messageRepository.save(new Message("o1", "aaaaaaaaaa"));
        messageRepository.save(new Message("o2", "bbbbbbbbbb"));
        messageRepository.save(new Message("o3", "cccccccccc"));

        Assert.assertEquals(3, messageRepository.findAll().size());
    }

    @Test
    public void delete(){

        Assert.assertEquals(1, userRepository.deleteByName("ccc"));

    }
}
