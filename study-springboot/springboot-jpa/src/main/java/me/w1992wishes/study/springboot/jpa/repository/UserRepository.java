package me.w1992wishes.study.springboot.jpa.repository;

import me.w1992wishes.study.springboot.jpa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @Description study-records
 * @Author w1992wishes
 * @Date 2018/6/21 17:13
 * @Version 1.0
 */
public interface UserRepository extends JpaRepository<User, Long> {

    //查询用户名称包含username字符串的用户对象
    List<User> findByUsernameContaining(String username);

    //获得单个用户对象，根据username和pwd的字段匹配
    User getByUsernameIsAndUserpwdIs(String username,String pwd);

    //精确匹配username的用户对象
    User getByUsernameIs(String username);

}
