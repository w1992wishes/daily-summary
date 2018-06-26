# 服务消费（Feign)

## Feign简介

Feign是一种声明式、模板化的HTTP客户端。在Spring Cloud中使用Feign, 我们可以做到使用HTTP请求远程服务时能与调用本地方法一样的编码体验，开发者完全感知不到这是远程方法，更感知不到这是个HTTP请求，这整个调用过程和Dubbo的RPC非常类似。开发起来非常的优雅。

## 搭建步骤

### 第一步：引入依赖

这里踩了一个大坑，大部分教程引入的 版本 都过低，如果采用新版本，但引入的还是旧的 maven 构建，@EnableFeignClients 无法引入进来，简单说就是：

新版本是用下面的依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

而不是：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-feign</artifactId>
</dependency>
```

而且一定要注意引入：

```xml
<dependencyManagement>
   <dependencies>
       <dependency>
           <groupId>org.springframework.cloud</groupId>
           <artifactId>spring-cloud-dependencies</artifactId>
           <version>2.0.0.RELEASE</version>
           <type>pom</type>
           <scope>import</scope>
       </dependency>
   </dependencies>
</dependencyManagement>
```

候选版本 2.0.0.RC2 同样不行。简直是巨坑。

完整的如下：

```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <java.version>1.8</java.version>
    <spring-cloud.version>2.0.0.RELEASE</spring-cloud.version>
</properties>

<dependencies>
    <!-- springcloud -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
   <dependencies>
       <dependency>
           <groupId>org.springframework.cloud</groupId>
           <artifactId>spring-cloud-dependencies</artifactId>
           <version>2.0.0.RELEASE</version>
           <type>pom</type>
           <scope>import</scope>
       </dependency>
   </dependencies>
</dependencyManagement>
```

所以最好还是参考最新的官网比较靠谱啊。

这里引入了 spring-boot-starter-actuator 是因为配置文件中有相应的健康监控配置。

### 第二步：创建配置文件

```yaml
spring:
  application:
    name: service-consumer-feign #指定服务名称（全部小写）

server:
  port: 8765

eureka:
  client:
    healthcheck:
      enabled: true
    serviceUrl:
      defaultZone: http://peer1:1111/eureka/,http://peer2:1112/eureka/
  instance:
    prefer-ip-address: true
```

### 第三步：构建服务

在程序的启动类ServiceConsumerFeignApplication，加上@EnableFeignClients注解开启Feign的功能：

```java
@SpringBootApplication
@EnableFeignClients
public class ServiceConsumerFeignApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceConsumerFeignApplication.class, args);
    }

}
```

定义一个feign接口，通过@ FeignClient（“服务名”），来指定调用哪个服务。比如在代码中调用了service-provider服务的“/hi”接口，代码如下：

```java
@FeignClient(value = "service-provider")
public interface SchedualServiceProvider {

    @RequestMapping(value = "/hi",method = RequestMethod.GET)
    String sayHiFromProviderOne(@RequestParam(value = "name") String name);

}
```

在Web层的controller层，对外暴露一个”/hi”的API接口，通过上面定义的Feign客户端 SchedualServiceProvider 来消费服务。代码如下：

```java
@RestController
public class HiController {

    @Autowired
    SchedualServiceProvider schedualServiceProvider;

    @RequestMapping(value = "/hi",method = RequestMethod.GET)
    public String sayHi(@RequestParam String name){
        return schedualServiceProvider.sayHiFromProviderOne(name);
    }
}
```

启动程序，多次访问http://localhost:8765/hi?name=w1992wishes,浏览器交替显示：

    hi w1992wishes,i am from port:8763
    hi w1992wishes,i am from port:8762