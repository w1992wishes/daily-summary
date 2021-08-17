package me.w1992wishes.web.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface HandlerInterceptor {

    /**
     * 拦截处理
     *
     * @param request  request
     * @param response response
     * @param handler  method handler
     * @return true/false
     */
    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler);

    /**
     * 请求处理结束
     *
     * @param request  request
     * @param response response
     * @param handler  method handler
     */
    void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler);

    /**
     * 完成后
     *
     * @param request  request
     * @param response response
     * @param handler  method handler
     */
    void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler);

}
