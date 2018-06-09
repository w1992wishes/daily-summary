# 高可用注册中心

为了保障可靠性，提高容灾能力，一般注册中心会搭建集群，用来保证可靠性，即使一个注册中心挂了，也可以有其他的注册中心顶上来。

## 搭建步骤

这里以一个项目启动两个 eureka 注册中心，步骤同 搭建单台 eureka 没有太大差别，具体步骤如下：

第一步： pom 文件引入 eureka 注册依赖:

```pom
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

第二步： 创建配置文件 application.yml

```cfml
spring:
  application:
    name: eureka-server
  profiles:
    active: peer1
```

以同一个项目打算启动两个server服务，占用不同的端口，以此模拟eureka服务集群，所以再添加两个配置文件：

application-peer1.yml

```cfml
server:
  port: 1111

eureka:
  instance:
    hostname: peer1
  client:
    serviceUrl:
      defaultZone: http://peer2:1112/eureka/
```

application-peer2.yml

```cfml
server:
  port: 1112

eureka:
  instance:
    hostname: peer2
  client:
    serviceUrl:
      defaultZone: http://peer1:1111/eureka/
```

第三步： 创建Application

```java
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EurekaServerApplication.class, args);
	}
}
```

第四步： 修改hosts 文件：

127.0.0.1 peer1
127.0.0.1 peer2

第五步： 通过 spring.profiles.active 指定不同的配置，启动两个注册服务

![](http://p9hx3bbrj.bkt.clouddn.com/springcloud_08.png)

![](http://p9hx3bbrj.bkt.clouddn.com/springcloud_09.png)

原本应该只有一个启动项，点击+号，给两个启动项都设置一下Program arguments，就是--spring.profiles.active分别设置为peer1和peer2，代表分别以两个不同的配置来启动项目。

然后把两个启动项都启动起来，分别访问各自的端口：

![](http://p9hx3bbrj.bkt.clouddn.com/springcloud_11.png)

![](http://p9hx3bbrj.bkt.clouddn.com/springcloud_10.png)

可以看到 peer1 的 registered-replicas 已经有 peer2 的 eureka-server，并且在可用节点 available-replicas，去看 peer2 也是相似的。

## 提供服务方

在设置了多节点的注册中心之后，服务提供方需要做一些简单修改将服务注册到集群中。

```cfml
eureka:
  client:
    serviceUrl:
      defaultZone: http://peer1:1111/eureka/,http://peer2:1112/eureka/
server:
  port: 8762
spring:
  application:
    name: eureka-client
```