package me.w1992wishes.study.springboot.jpa.repository;

import me.w1992wishes.study.springboot.jpa.entity.User;
import me.w1992wishes.study.springboot.jpa.entity.Weibo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Description study-records
 * @Author w1992wishes
 * @Date 2018/6/21 17:35
 * @Version 1.0
 */
public interface WeiboRepository extends JpaRepository<Weibo, Long> {

    @Query("select w from Weibo w where w.user.username = :username")
    List<Weibo> searchUserWeibo(@Param("username") String username);

    @Query("select w from Weibo w where w.user.username = :username")
    List<Weibo> searchUserWeibo(@Param("username") String username, Sort sort);

    @Modifying
    @Transactional(readOnly = false)
    @Query("update Weibo w set w.weiboText = :text where w.user = :user")
    int setUserWeiboContent(@Param("text")String weiboText,@Param("user")User user);

    Page<Weibo> findByUserIsAndWeiboTextContaining(User user, String weiboText, Pageable pageable);

    @Transactional(readOnly = false)
    int deleteByUser(User user);

}
