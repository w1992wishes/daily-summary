package me.w1992wishes.web.interceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebSecurityConfiguration implements WebMvcConfigurer {

    private static final List<String> IGNORES = Arrays.asList("/*.html", "/swagger-ui");

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(getHandlerInterceptor())
        .addPathPatterns("/**")
        .excludePathPatterns(IGNORES);
    }

    private InterceptorAdapter getHandlerInterceptor() {
        InterceptorAdapter adapter = new InterceptorAdapter();
        adapter.addHandlerInterceptor(new UserHandlerInterceptor());
        adapter.addHandlerInterceptor(new PermissionInterceptor());
        adapter.addHandlerInterceptor(new TimeConsumingInterceptor());
        return adapter;
    }
}
