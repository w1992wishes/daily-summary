# 配置中心

随着线上项目变的日益庞大，每个项目都散落着各种配置文件，如果采用分布式的开发模式，需要的配置文件随着服务增加而不断增多。某一个基础服务信息变更，
都会引起一系列的更新和重启，运维苦不堪言也容易出错。配置中心便是解决此类问题的灵丹妙药。

在Spring Cloud中，有分布式配置中心组件spring cloud config ，它支持配置服务放在配置服务的内存中（即本地），也支持放在远程Git仓库中。
在实现cloud config中，主要有两个角色：作为配置中心连接配置路径的 config server,连接配置中心读取配置的config client。

## 搭建步骤

### 第一步：引入依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-config-server</artifactId>
    </dependency>
</dependencies>
```

只需要加入spring-cloud-config-server包引用既可。

### 第二步：配置文件

```yaml
server:
  port: 8769

spring:
  application:
    name: service-config

  cloud:
    config:
      server:
        git:
          uri: https://github.com/w1992wishes/study-records/tree/master/study-springcloud/     # 配置git仓库的地址
          search-paths: service-config-repository               # git仓库地址下的相对地址，可以配置多个，用,分割。
          username:                                             # git仓库的账号
          password:    
```

* spring.cloud.config.server.git.uri：配置git仓库地址
* spring.cloud.config.server.git.searchPaths：配置仓库路径
* spring.cloud.config.label：配置仓库的分支
* spring.cloud.config.server.git.username：访问git仓库的用户名
* spring.cloud.config.server.git.password：访问git仓库的用户密码

如果Git仓库为公开仓库，可以不填写用户名和密码，如果是私有仓库需要填写。

### 第三步：配置仓库

这里将配置文件放在github上，所以先创建一个专用仓库 service-config-repository。为了模拟生产环境，创建三个配置文件：

    // 开发环境
    neo-config-dev.properties
    // 测试环境
    neo-config-test.properties
    // 生产环境
    neo-config-pro.properties
    
每个配置文件中都写一个属性neo.hello,属性值分别是 hello im dev/test/pro 。

### 第四步：搭建服务

