# 创建一个服务提供者 

## 搭建步骤

### 第一步： pom 文件引入依赖（这里直接通过 Idea 生成，会直接引入依赖）

当服务提供者向注册中心注册时，它会提供一些元数据，例如主机和端口，URL，主页等。Eureka server 从每个client实例接收心跳消息。 如果心跳超时，则通常将该实例从注册server中删除。

创建过程同server类似，不过最后一步选择稍有不同：

![](http://p9hx3bbrj.bkt.clouddn.com/springcloud_05.jpg)

生成的pom.xml如下：

```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>me.w1992wishes.study</groupId>
	<artifactId>service-provider</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<packaging>jar</packaging>

	<name>service-provider</name>
	<description>Demo project for Spring Boot</description>

	<parent>
		<artifactId>study-springcloud</artifactId>
		<groupId>me.w1992wishes.study</groupId>
		<version>1.0-SNAPSHOT</version>
	</parent>

	<properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
		<java.version>1.8</java.version>
		<spring-cloud.version>Finchley.RC2</spring-cloud.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
			<version>RELEASE</version>
		</dependency>
	</dependencies>

	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>${spring-cloud.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

	<repositories>
		<repository>
			<id>spring-milestones</id>
			<name>Spring Milestones</name>
			<url>https://repo.spring.io/milestone</url>
			<snapshots>
				<enabled>false</enabled>
			</snapshots>
		</repository>
	</repositories>


</project>
```

其中starter-web依赖是手动添加的。

### 第二步： 创建配置文件 application.yml

在配置文件中注明自己的服务注册中心的地址，application.yml配置文件如下：

```
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
server:
  port: 8762
spring:
  application:
    name: eureka-client
```

* spring.application.name 指定服务名称（全部小写）
* eureka.instance.instance-id 指定服务在eureka上的显示
* eureka.client.healthcheck.enabled 健康检查

需要指明spring.application.name，这个很重要，这在以后的服务与服务之间相互调用一般都是根据这个name 。

### 第三步： 创建Application

**通过注解@EnableEurekaClient 表明自己是一个 服务提供者**：

```
@EnableEurekaClient
@RestController
@SpringBootApplication
public class ServiceProviderApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceProviderApplication.class, args);
	}

	@Value("${server.port}")
	String port;

	@RequestMapping("/hi")
	public String home(@RequestParam String name) {
		return "hi "+name+",i am from port:" +port;
	}

}
```

启动工程，打开http://localhost:8761 ，即eureka server 的网址：

![](http://p9hx3bbrj.bkt.clouddn.com/springcloud_06.jpg)

会发现一个服务已经注册在服务中了，服务名为EUREKA-CLIENT，端口为8762。

这时打开 http://localhost:8762/hi?name=w1992wishes ，你会在浏览器上看到 :

    hi w1992wishes,i am from port:8762