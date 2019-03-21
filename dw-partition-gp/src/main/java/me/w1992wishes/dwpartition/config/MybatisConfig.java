package me.w1992wishes.dwpartition.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * @author w1992wishes 2019/3/20 16:48
 */
public class MybatisConfig {

    DataSource ds;

    SqlSessionFactory getSqlSessionFactory(DataSource dwdDS, String locatinPattern) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dwdDS);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        factoryBean.setMapperLocations(resolver.getResources(locatinPattern));
        return factoryBean.getObject();
    }

}
