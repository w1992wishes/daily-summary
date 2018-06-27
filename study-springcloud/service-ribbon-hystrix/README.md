# springcloud 断路器

在微服务架构中，根据业务来拆分成一个个的服务，服务与服务之间可以相互调用（RPC），在Spring Cloud可以用RestTemplate+Ribbon和Feign来调用。为了保证其高可用，单个服务通常会集群部署。由于网络原因或者自身的原因，服务并不能保证100%可用，如果单个服务出现问题，调用这个服务就会出现线程阻塞，此时若有大量的请求涌入，Servlet容器的线程资源会被消耗完毕，导致服务瘫痪。服务与服务之间的依赖性，故障会传播，会对整个微服务系统造成灾难性的严重后果，这就是服务故障的“雪崩”效应。

Netflix开源了Hystrix组件，实现了断路器模式，SpringCloud对这一组件进行了整合。用于解决上述问题。

![](http://p9hx3bbrj.bkt.clouddn.com/springcloud_13.png)

## 准备工作

先启动注册中心和服务提供。

## 搭建步骤

### 第一步：引入依赖

在消费服务中引入 Histrix 依赖：

```xml
 <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

或者新建消费服务：
 
![](http://p9hx3bbrj.bkt.clouddn.com/springcloud_14.png)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### 第二步：配置文件

在原服务中引入 Histrix 依赖，不需修改配置文件。

新服务配置文件直接copy，修改端口和服务名即可。

### 第三步：创建服务

在程序的启动类ServiceRibbonHystrixApplication 加@EnableHystrix注解开启Hystrix：

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableHystrix
public class ServiceRibbonHystrixApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceRibbonHystrixApplication.class, args);
    }

    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
```

改造HelloService类，在hiService方法上加上@HystrixCommand注解。该注解对该方法创建了熔断器的功能，并指定了fallbackMethod熔断方法，熔断方法直接返回了一个字符串，字符串为”hi,”+name+”,sorry,error!”，代码如下：

```java
@Service
public class HelloService {

    @Autowired
    RestTemplate restTemplate;

    @HystrixCommand(fallbackMethod = "hiError")
    public String hiService(String name) {
        return restTemplate.getForObject("http://service-provider/hi?name=" + name, String.class);
    }

    public String hiError(String name) {
        return "hi,"+name+",sorry,error!";
    }
}
```

启动服务，访问 http://localhost:8766/hi?name=w1992wishes，返回：

    hi w1992wishes,i am from port:8763
    
此时关闭 service-provider 工程，当我们再访问 http://localhost:8766/hi?name=w1992wishes ，浏览器会显示：

    hi,w1992wishes,sorry,error!