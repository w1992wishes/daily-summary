package me.w1992wishes.jvm;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

/**
 * VM Args： -XX:PermSize=10M -XX:MaxPermSize=10M
 *
 * JDK 1.8 不再由持久代，类信息改为放在元空间 -XX:MetaspaceSize=10m -XX:MaxMetaspaceSize=10m
 */
public class JavaMethodAreaOOM {

    public static void main(String[] args) {
        while (true) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(OOMObject.class);
            enhancer.setUseCache(false);
            enhancer.setCallback((MethodInterceptor) (obj, method, arg, proxy) -> proxy.invokeSuper(obj, arg));
            enhancer.create();
        }
    }

    static class OOMObject {

    }
}