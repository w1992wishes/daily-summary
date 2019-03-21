package me.w1992wishes.dwpartition.config;

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
@MapperScan(basePackages = {"me.w1992wishes.dwpartition.mapper.odl"}, sqlSessionFactoryRef = "sqlSessionFactoryOdl")
public class MybatisOdlConfig extends MybatisConfig {

    @Autowired
    public MybatisOdlConfig(@Qualifier("odlDS") DataSource odlDS) {
        this.ds = odlDS;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactoryOdl() throws Exception {
        return getSqlSessionFactory(ds, "classpath*:mapper/odl/*.xml");
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplateOdl() throws Exception {
        return new SqlSessionTemplate(sqlSessionFactoryOdl());
    }
}