package me.w1992wishes.web.interceptor;

import me.w1992wishes.common.domain.User;
import org.springframework.http.HttpMethod;

public class RequestInformation {

    private User user;
    private String requestUrl;
    private HttpMethod method;
    /**
     * 请求时间
     */
    private long startTime;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
}
