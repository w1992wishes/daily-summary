package me.w1992wishes.classloader.executor.proxy;

import me.w1992wishes.classloader.executor.common.Executor;

import java.lang.reflect.Method;

public class ExecutorProxy implements Executor {
    private String version;
    private StandardExecutorClassLoader classLoader;
    
    public ExecutorProxy(String version) {
        this.version = version;
        classLoader = new StandardExecutorClassLoader(version);
    }
    
    @Override
    public void execute(String name) {
        try {
            // Load ExecutorProxy class
            Class<?> executorClazz = classLoader.loadClass("me.w1992wishes.classloader.executor." + version + ".Executor" + version.toUpperCase());
            Object executorInstance = executorClazz.newInstance();
            Method method = executorClazz.getMethod("execute", String.class);
            method.invoke(executorInstance, name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}