package me.w1992wishes.web.interceptor;

import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseHandlerInterceptor implements HandlerInterceptor {

    protected void processResponse(HttpServletResponse response, String code, String msg) {
        response.setCharacterEncoding("utf-8");
        try {
            PrintWriter out = response.getWriter();
            Map<String, String> result = new HashMap<>();
            result.put(code, msg);
            out.append(JSON.toJSONString(result));
            response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.SC_OK);
        } catch (Exception e) {
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // nothing
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // nothing
    }
}
