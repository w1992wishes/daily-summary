package me.w1992wishes.concurrency.aqs;

import java.util.concurrent.CyclicBarrier;

/**
 * 说明:启用 30 个线程，每个线程对i自加 10000 次，同步正常的话，最终结果应为 300000；
 */
public class TestMutex {

    private static CyclicBarrier barrier = new CyclicBarrier(31);
    private static int a = 0;
    private static Mutex mutex = new Mutex();

    public static void main(String[] args) throws Exception {

        //未加锁前
        for (int i = 0; i < 30; i++) {
            Thread t = new Thread(() -> {
                for (int i1 = 0; i1 < 10000; i1++) {
                    increment1();//没有同步措施的a++；
                }
                try {
                    barrier.await();//等30个线程累加完毕
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.start();
        }
        barrier.await();

        System.out.println("加锁前，a=" + a);

        //加锁后
        barrier.reset();//重置CyclicBarrier
        a = 0;
        for (int i = 0; i < 30; i++) {
            new Thread(() -> {
                for (int i12 = 0; i12 < 10000; i12++) {
                    increment2();//a++采用Mutex进行同步处理
                }
                try {
                    barrier.await();//等30个线程累加完毕
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        barrier.await();
        System.out.println("加锁后，a=" + a);
    }

    /**
     * 没有同步措施的a++
     */
    private static void increment1() {
        a++;
    }

    /**
     * 使用自定义的 Mutex 进行同步处理的 a++
     */
    private static void increment2() {
        mutex.lock();
        a++;
        mutex.unlock();
    }
}
