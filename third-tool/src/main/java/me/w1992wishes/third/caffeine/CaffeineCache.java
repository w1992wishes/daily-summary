package me.w1992wishes.third.caffeine;

import com.github.benmanes.caffeine.cache.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CaffeineCache {

    public void sync() {
        Cache<String, String> cache = Caffeine.newBuilder()
                // 数量上限
                .maximumSize(1024)
                // 过期机制
                .expireAfterWrite(5, TimeUnit.MINUTES)
                // 弱引用key
                .weakKeys()
                // 弱引用value
                .weakValues()
                // 剔除监听
                .removalListener((RemovalListener<String, String>) (key, value, cause) ->
                        System.out.println("key:" + key + ", value:" + value + ", 删除原因:" + cause.toString()))
                .build();
        // 将数据放入本地缓存中
        cache.put("username", "afei");
        cache.put("password", "123456");
        // 从本地缓存中取出数据
        System.out.println(cache.getIfPresent("username"));
        System.out.println(cache.getIfPresent("password"));
        // 本地缓存没有的话，从数据库或者Redis中获取
        System.out.println(cache.get("blog", this::getValue));
    }

    private void asyn() throws ExecutionException, InterruptedException, TimeoutException {
        AsyncLoadingCache<String, String> cache = Caffeine.newBuilder()
                // 数量上限
                .maximumSize(2)
                // 失效时间
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .refreshAfterWrite(1, TimeUnit.MINUTES)
                // 异步加载机制
                .buildAsync(new CacheLoader<String, String>() {
                    @Nullable
                    @Override
                    public String load(@NonNull String key) throws Exception {
                        return getValue(key);
                    }
                });
        System.out.println(cache.get("username").get());
        System.out.println(cache.get("password").get(10, TimeUnit.MINUTES));
        System.out.println(cache.get("username").get(10, TimeUnit.MINUTES));
        System.out.println(cache.get("blog").get());
    }

    private String getValue(String key) {
        return key + "";
    }
}
