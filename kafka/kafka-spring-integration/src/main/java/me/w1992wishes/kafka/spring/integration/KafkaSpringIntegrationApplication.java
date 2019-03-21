package me.w1992wishes.kafka.spring.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KafkaSpringIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaSpringIntegrationApplication.class, args);
    }

   /* // 加载YML格式自定义配置文件
    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        //yaml.setResources(new FileSystemResource("config.yml"));//File引入
        yaml.setResources(new ClassPathResource("kafka/kafka.yml"));//class引入
        configurer.setProperties(yaml.getObject());
        return configurer;
    }*/
}
