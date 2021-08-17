package me.w1992wishes.web.interceptor;

import me.w1992wishes.common.domain.User;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserHandlerInterceptor extends BaseHandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String name = request.getHeader("user-name");
        if (StringUtils.isEmpty(name)) {
            processResponse(response, "-1", "user login fail.");
            return false;
        }

        User user = getUser(name);
        RequestInfoUtils.get().setUser(user);

        return true;
    }

    private User getUser(String name) {
        return new User();
    }

}
