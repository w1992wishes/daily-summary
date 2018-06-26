# 服务消费者（ribbon + restTemplate）

Spring cloud有两种服务调用方式，一种是ribbon+restTemplate，另一种是feign。ribbon是一个负载均衡客户端，可以很好的控制htt和tcp的一些行为。Feign默认集成了ribbon。

## 先准备两个服务提供者实例

将先前创建的 service-provider 项目用不同的端口启动两次，相当两个服务提供，可以在管理页面看到：

![](http://p9hx3bbrj.bkt.clouddn.com/springcloud_12.png)

## 搭建步骤

### 第一步： pom 文件引入依赖

这里需要引入dependencyManagement， 否则 springcloud 相应的包引入不进来。

```xml
<!-- springcloud -->
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-ribbon</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>Finchley.RC2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 第二步： 创建配置文件 application.yml

```yaml
spring:
  application:
    name: service-consumer-ribbon #指定服务名称（全部小写）

server:
  port: 8764

eureka:
  client:
    healthcheck:
      enabled: true
    serviceUrl:
      defaultZone: http://peer1:1111/eureka/,http://peer2:1112/eureka/
  instance:
    prefer-ip-address: true
```

### 第三步：创建消费服务

在项目启动类Application上添加@EnableDiscoveryClient注解，用于发现服务：

```java
@SpringBootApplication
@EnableDiscoveryClient
public class ServiceConsumerRibbonApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceConsumerRibbonApplication.class, args);
    }

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
```

写一个测试类HelloService，通过上面注入ioc容器的restTemplate来消费service-provider服务的“/hi”接口，在这里我们直接用的程序名替代了具体的url地址，在ribbon中它会根据服务名来选择具体的服务实例，根据服务实例在请求的时候会用具体的url替换掉服务名，代码如下：

```java
@Service
public class HelloService {

    @Autowired
    RestTemplate restTemplate;

    public String hiService(String name) {
        return restTemplate.getForObject("http://service-provider/hi?name="+name,String.class);
    }

}
```

写一个controller，在controller中用调用HelloService 的方法，代码如下：

```java

@RestController
public class HelloController {

    @Autowired
    HelloService helloService;

    @RequestMapping(value = "/hi")
    public String hi(@RequestParam String name){
        return helloService.hiService(name);
    }

}
```

在浏览器上多次访问http://localhost:8764/hi?name=w1992wishes，浏览器交替显示：

    hi w1992wishes,i am from port:8763
    hi w1992wishes,i am from port:8762