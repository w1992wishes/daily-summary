package me.w1992wishes.web.interceptor;

import me.w1992wishes.common.domain.User;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PermissionInterceptor extends BaseHandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (this.hasPermission(handler)) {
            return true;
        }
        processResponse(response, "-1", "no permission");
        return false;
    }

    private boolean hasPermission(Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            // 方法注解
            RequiredRole requiredRole = handlerMethod.getMethod().getAnnotation(RequiredRole.class);
            // 方法注解为 null，则获取类上注解
            if (requiredRole == null) {
                requiredRole = ((HandlerMethod) handler).getMethod().getDeclaringClass().getAnnotation(RequiredRole.class);
            }

            if (requiredRole == null) {
                return true;
            }

            User user = RequestInfoUtils.getUser();
            if (Objects.isNull(user)) {
                return false;
            }
            return Arrays.stream(requiredRole.roles()).anyMatch(r -> user.getRoles().contains(r));
        }
        return true;
    }
}
