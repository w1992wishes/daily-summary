package me.w1992wishes.study.springboot.jpa.service.impl;

import me.w1992wishes.study.springboot.jpa.entity.User;
import me.w1992wishes.study.springboot.jpa.repository.UserRepository;
import me.w1992wishes.study.springboot.jpa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Description study-records
 * @Author w1992wishes
 * @Date 2018/6/21 17:51
 * @Version 1.0
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }
}
