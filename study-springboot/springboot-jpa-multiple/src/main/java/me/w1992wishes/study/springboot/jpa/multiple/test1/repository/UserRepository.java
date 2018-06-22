package me.w1992wishes.study.springboot.jpa.multiple.test1.repository;

import me.w1992wishes.study.springboot.jpa.multiple.test1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @Description study-records
 * @Author w1992wishes
 * @Date 2018/6/22 16:42
 * @Version 1.0
 */
public interface UserRepository extends JpaRepository<User, Long> {
}
