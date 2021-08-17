package me.w1992wishes.web.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TimeConsumingInterceptor extends BaseHandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TimeConsumingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RequestInfoUtils.get().setStartTime(System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long endTime = System.currentTimeMillis();
        long startTime = RequestInfoUtils.get().getStartTime();
        log.info("{}: 耗时： {}s", RequestInfoUtils.get().getRequestUrl(), ((endTime - startTime) * 1.000) / 1000);
    }
}
