# springcloud 断路器 + feign

大致同 ribbon 搭建过程。

## 搭建步骤

### 第一步：引入依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### 第二步：配置文件

```yaml
spring:
  application:
    name: service-feign-hystrix #指定服务名称（全部小写）

server:
  port: 8767

eureka:
  client:
    serviceUrl:
      defaultZone: http://peer1:1111/eureka/,http://peer2:1112/eureka/
  instance:
    prefer-ip-address: true
```

### 第三步：创建服务

```java
@SpringBootApplication
@EnableFeignClients
@EnableHystrix
public class ServiceFeignHystrixApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceFeignHystrixApplication.class, args);
    }
}
```

基于 service-consumer-feign 工程进行改造，只需要在 FeignClient 的 SchedualServiceProvider 接口的注解中加上 fallback 的指定类就行了：

```java
@FeignClient(value = "service-provider", fallback = SchedualServiceProviderHystric.class )
public interface SchedualServiceProvider {

    @RequestMapping(value = "/hi",method = RequestMethod.GET)
    String sayHiFromProviderOne(@RequestParam(value = "name") String name);

}
```

SchedualServiceProviderHystric 实现 SchedualServiceProvider 并注入 spring 容器：

```java
@Service
public class SchedualServiceProviderHystric implements SchedualServiceProvider {
    @Override
    public String sayHiFromProviderOne(String name) {
        return "sorry "+name;
    }
}
```

启动服务，访问 http://localhost:8767/hi?name=w1992wishes，返回：

    hi w1992wishes,i am from port:8763
    
此时关闭 service-provider 工程，当我们再访问 http://localhost:8766/hi?name=w1992wishes ，直接报错了。

原来Feign是自带断路器的，它没有默认打开。需要在配置文件中配置打开它，在配置文件加以下代码：

```yaml
feign:
  hystrix:
    enabled: true
```

这时运行正常，返回：

    sorry w1992wishes