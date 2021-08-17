package me.w1992wishes.web.interceptor;

import me.w1992wishes.common.domain.User;

import java.util.Objects;

public class RequestInfoUtils {
    private RequestInfoUtils() {
    }

    private static final ThreadLocal<RequestInformation> REQUEST_INFORMATION_THREAD_LOCAL = new ThreadLocal<>();

    public static RequestInformation createNewRequestInfo() {
        RequestInformation requestInformation = new RequestInformation();
        REQUEST_INFORMATION_THREAD_LOCAL.set(requestInformation);
        return requestInformation;
    }

    public static RequestInformation get() {
        return REQUEST_INFORMATION_THREAD_LOCAL.get();
    }

    public static User getUser() {
        if (Objects.isNull(get())) {
            return null;
        }
        return get().getUser();
    }

    public static void remove() {
        REQUEST_INFORMATION_THREAD_LOCAL.remove();
    }
}
