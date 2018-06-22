# springboot-flyway

## 一、Flyway简介

Flyway是一个简单开源数据库版本控制器（约定大于配置），主要提供migrate、clean、info、validate、baseline、repair等命令。它支持SQL（PL/SQL、T-SQL）方式和Java方式，支持命令行客户端等，还提供一系列的插件支持（Maven、Gradle、SBT、ANT等）。

官方网站：https://flywaydb.org/

## 二、动手试试

### 2.1、引入依赖

```mxml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2.2、配置数据源

```yaml
## Spring DATASOURCE (DataSourceAutoConfiguration & DataSourceProperties)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test?useSSL=false
    username: root
    password: introcks1234

## Hibernate Properties
# The SQL dialect makes Hibernate generate better SQL for the chosen database
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL5InnoDBDialect

## This is important
# Hibernate ddl auto (create, create-drop, validate, update)
    hibernate:
      ddl-auto: validate
```

### 2.3、按Flyway的规范创建版本化的SQL脚本

* 在 /src/mian/resources 目录下创建 db/migration 目录（flyway 找脚本的时候默认去 /src/mian/resources 下面的 db/migration ，当然这个是可以修改的，flyway.locations=classpath:/db）
* 在 db目录下创建版本化的SQL脚本 V1.0__init.sql（使用 V<VERSION>__DESCRIPTION.sql方式命名脚本，其中下划线是两个下划线）

### 2.4、运行

直接运行可以看到如下信息：

![](http://p9hx3bbrj.bkt.clouddn.com/springboot_flyway_01.png) 

然后到数据库可以发现数据表已经创建，脚本执行成功：

![](http://p9hx3bbrj.bkt.clouddn.com/springboot_flyway_02.png)