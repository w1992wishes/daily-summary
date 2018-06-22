package me.w1992wishes.study.springboot.jpa.multiple.test2.repository;

import me.w1992wishes.study.springboot.jpa.multiple.test2.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @Description study-records
 * @Author w1992wishes
 * @Date 2018/6/22 16:42
 * @Version 1.0
 */
public interface MessageRepository extends JpaRepository<Message, Long> {
}
