package me.w1992wishes.web.interceptor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;

public class InterceptorAdapter extends HandlerInterceptorAdapter {

    private final List<HandlerInterceptor> interceptorList = new LinkedList<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        RequestInformation requestInformation = RequestInfoUtils.createNewRequestInfo();
        requestInformation.setRequestUrl(getRequestUrl(request));
        requestInformation.setMethod(HttpMethod.valueOf(request.getMethod()));
        for (HandlerInterceptor interceptor : interceptorList) {
            if (!interceptor.preHandle(request, response, handler)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        for (HandlerInterceptor interceptor : interceptorList) {
            interceptor.afterCompletion(request, response, handler);
        }
        RequestInfoUtils.remove();
    }

    private String getRequestUrl(HttpServletRequest request) {
        String requestUti = request.getRequestURI();
        String queryString = request.getQueryString();
        if (!StringUtils.isEmpty(queryString)) {
            requestUti += "?" + queryString;
        }
        return requestUti;
    }

    public void addHandlerInterceptor(HandlerInterceptor interceptor) {
        interceptorList.add(interceptor);
    }
}
