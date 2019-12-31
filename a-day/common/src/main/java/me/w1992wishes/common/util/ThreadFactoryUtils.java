package me.w1992wishes.common.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author w1992wishes 2019/4/18 14:49
 */
public class ThreadFactoryUtils {

    private ThreadFactoryUtils() {
    }

    public static ThreadFactory getNameThreadFactory(String namePrefix) {
        return new ThreadFactory() {
            //原子类，线程池编号
            private final AtomicInteger poolNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                //真正创建线程的地方，设置了线程的线程组及线程名
                Thread t = new Thread(r, namePrefix + poolNumber.getAndIncrement());
                //默认是正常优先级
                if (t.getPriority() != Thread.NORM_PRIORITY) {
                    t.setPriority(Thread.NORM_PRIORITY);
                }
                if (t.isDaemon()) {
                    t.setDaemon(false);
                }
                return t;
            }
        };
    }

}
