package me.w1992wishes.azkaban.config;

import me.w1992wishes.azkaban.api.AzkabanApi;
import me.w1992wishes.azkaban.proxy.AzkabanApiProxyBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置API，创建Bean，并注入Spring
 *
 * @author Administrator
 */
@Configuration
public class AzkabanApiConfig {
    @Value("${azkaban.url}")
    private String uri;

    @Value("${azkaban.username}")
    private String username;

    @Value("${azkaban.password}")
    private String password;

    @Bean
    public AzkabanApi azkabanApi() {
        return AzkabanApiProxyBuilder.create()
                .setUri(uri)
                .setUsername(username)
                .setPassword(password)
                .build();
    }
}
