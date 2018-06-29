# 路由网关(zuul)

外部的应用如何来访问内部各种各样的微服务呢？在微服务架构中，后端服务往往不直接开放给调用端，而是通过一个API网关根据请求的url，路由到相应的服务。
当添加API网关后，在第三方调用端和服务提供方之间就创建了一面墙，这面墙直接与调用方通信进行权限控制，后将请求均衡分发给后台服务端。

## 服务路由

在之前的环境上搭建，首先将注册中心等服务启动。

### 第一步：引入依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-zuul</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
</dependencies>
```

spring-cloud-starter-netflix-zuul 用于网关，spring-cloud-starter-netflix-eureka-client用于服务注册。

#### 第二步：配置文件

```yaml
spring:
  application:
    name: service-zuul #指定服务名称（全部小写）

server:
  port: 8768

eureka:
  client:
    serviceUrl:
      defaultZone: http://peer1:1111/eureka/,http://peer2:1112/eureka/
  instance:
    prefer-ip-address: true

zuul:
  routes:
    api-a:
      path: /api-a/**
      serviceId: service-ribbon-hystrix
    api-b:
      path: /api-b/**
      serviceId: service-feign-hystrix
```

首先指定服务注册中心的地址为 http://peer1:1111/eureka/,http://peer2:1112/eureka/，服务的端口为8768，服务名为 service-zuul ；
以 /api-a/ 开头的请求都转发给 service-ribbon-hystrix 服务；以 /api-b/ 开头的请求都转发给 service-feign-hystrix 服务；

依次启动服务，浏览器访问： 

http://localhost:8768/api-a/hi?name=w1992wishes

    hi w1992wishes,i am from port:8763
    
http://localhost:8768/api-b/hi?name=w1992wishes
    
    hi w1992wishes,i am from port:8763
   
上面 zuul 起到了路由的作用。

## 服务过滤

zuul不仅只是路由，并且还能过滤，做一些安全验证。继续改造工程，继承 ZuulFilter 实现自己的过滤器：

```java
@Component
public class MyFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(MyFilter.class);
    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        log.info(String.format("%s >>> %s", request.getMethod(), request.getRequestURL().toString()));
        Object accessToken = request.getParameter("token");
        if(accessToken == null) {
            log.warn("token is empty");
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(401);
            try {
                ctx.getResponse().getWriter().write("token is empty");
            }catch (Exception e){}

            return null;
        }
        log.info("ok");
        return null;
    }
}
```

解释如下：

* filterType：返回一个字符串代表过滤器的类型，在zuul中定义了四种不同生命周期的过滤器类型，具体如下： 
    * pre：路由之前
    * routing：路由之时
    * post： 路由之后
    * error：发送错误调用
* filterOrder：过滤的顺序
* shouldFilter：这里可以写逻辑判断，是否要过滤，这里true,永远过滤。
* run：过滤器的具体逻辑。可用很复杂，包括查sql，nosql去判断该请求到底有没有权限访问。

这时访问：

http://localhost:8768/api-b/hi?name=w1992wishes&token=22

    hi w1992wishes,i am from port:8763

http://localhost:8768/api-b/hi?name=w1992wishes

    token is empty