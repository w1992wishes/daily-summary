package me.w1992wishes.third.guava;

import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GuavaRateLimiter {

    public static ConcurrentHashMap<String, RateLimiter> resourceRateLimiter = new ConcurrentHashMap<>();
    
    static {
        createResourceRateLimiter("order", 50);
    }

    private static void createResourceRateLimiter(String resource, double qps) {
        RateLimiter rateLimiter = resourceRateLimiter.computeIfAbsent(resource, k -> RateLimiter.create(qps));
        rateLimiter.setRate(qps);
    }

    public static void main(String[] args) {
        for (int i=0; i<5000; i++) {
            new Thread(() -> {
                if (resourceRateLimiter.get("order").tryAcquire(10, TimeUnit.MICROSECONDS)) {
                    System.out.println("execute");
                } else {
                    System.out.println("wait");
                }
            }).start();
        }
    }

}
