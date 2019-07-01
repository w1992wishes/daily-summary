package me.w1992wishes.script.flyway.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.List;

/**
 * 数据库配置类
 * @author percyl
 */
@Configuration
public class DataSourceConfig {

    @Resource
    private ApplicationArguments arguments;

    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;

    @Autowired
    private Environment env;
    
    @Bean
    public DataSource getDataSource() {
        BasicDataSource dataSource = new BasicDataSource();

        List<String> dirNames = arguments.getOptionValues("dirName");
        if(CollectionUtils.isEmpty(dirNames)) {
            throw new RuntimeException("启动脚本需带有dirName参数");
        }
        String dirName = dirNames.get(0);
        String dbName = env.getProperty(dirName);
        if(StringUtils.isEmpty(dbName)) {
        	throw new RuntimeException("配置文件需要有"+dirName+"参数");
        }

        dataSource.setUrl(url + "/"+dbName);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}