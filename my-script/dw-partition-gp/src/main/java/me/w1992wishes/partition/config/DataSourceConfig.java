package me.w1992wishes.partition.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author w1992wishes 2019/3/20 14:43
 */
@Configuration
public class DataSourceConfig {

    @Bean(name = "odlDS")
    @ConfigurationProperties(prefix = "spring.datasource.odl")
    public DataSource dataSourceOdl() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "dwdDS")
    @ConfigurationProperties(prefix = "spring.datasource.dwd")
    public DataSource dataSourceDwd() {
        return DataSourceBuilder.create().build();
    }

}
