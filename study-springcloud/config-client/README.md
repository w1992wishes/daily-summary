# 从配置中心获取配置

有了配置中心服务，再搭建一个客户端，从配置中心获取配置。

## 搭建步骤

### 引入依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-config-client</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 第二步：配置文件

需要配置两个配置文件，application.ymal 和 bootstrap.ymal

application.ymal指定client 一些基本信息：

```yaml
server:
  port: 8770

spring:
  application:
    name: config-client
```

bootstrap.ymal 指定配置中心信息：

```yaml
spring:
  cloud:
    config:
      name: neo-config
      uri: http://localhost:8769
      profile: dev
      label: master
```

* spring.application.name：对应{application}部分
* spring.cloud.config.profile：对应{profile}部分
* spring.cloud.config.label：对应git的分支。如果配置中心使用的是本地存储，则该参数无用
* spring.cloud.config.uri：配置中心的具体地址
* spring.cloud.config.discovery.service-id：指定配置中心的service-id，便于扩展为高可用配置集群。

PS：上面这些与配置中心相关的属性必须配置在bootstrap.yml中，config部分内容才能被正确加载。因为config的相关配置会先于application.yml，
而bootstrap.yml的加载也是先于application.yml。

### 第三步：搭建服务

```java
@SpringBootApplication
@RestController
public class ConfigClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigClientApplication.class, args);
    }

    @Value("${neo.hello}")
    String hello;
    @RequestMapping(value = "/hi")
    public String hi(){
        return hello;
    }
}
```

启动访问 http://localhost:8770/hi：

    hello im dev
    
手动修改neo-config-dev.properties中配置信息为：neo.hello=hello im dev update1提交到github,再次在浏览器访问http://localhost:8770/hi，返回：hello im dev，说明获取的信息还是旧的参数，这是为什么呢？

因为spirngboot项目只有在启动的时候才会获取配置文件的值，修改github信息后，client端并没有在次去获取，所以导致这个问题。