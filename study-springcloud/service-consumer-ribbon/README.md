# 服务消费者（ribbon + restTemplate）

Spring cloud有两种服务调用方式，一种是ribbon+restTemplate，另一种是feign。ribbon是一个负载均衡客户端，可以很好的控制htt和tcp的一些行为。Feign默认集成了ribbon。

## 先准备两个服务提供者实例

将先前创建的 service-provider 项目用不同的端口启动两次，相当两个服务提供，可以在管理页面看到：

![]()

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

