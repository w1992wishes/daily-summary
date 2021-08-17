package me.w1992wishes.web.interceptor;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiredRole {
    String[] roles();
}
