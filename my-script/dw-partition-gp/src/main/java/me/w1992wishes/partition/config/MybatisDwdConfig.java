package me.w1992wishes.partition.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author w1992wishes 2019/3/20 14:43
 */
@Configuration
@MapperScan(basePackages = {"me.w1992wishes.partition.mapper.dwd"}, sqlSessionFactoryRef = "sqlSessionFactoryDwd")
public class MybatisDwdConfig extends MybatisConfig{

    @Autowired
    public MybatisDwdConfig(@Qualifier("dwdDS") DataSource dwdDS) {
        this.ds = dwdDS;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactoryDwd() throws Exception {
        return getSqlSessionFactory(ds, "classpath*:mapper/dwd/*.xml");
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplateDwd() throws Exception {
        return new SqlSessionTemplate(sqlSessionFactoryDwd());
    }
}