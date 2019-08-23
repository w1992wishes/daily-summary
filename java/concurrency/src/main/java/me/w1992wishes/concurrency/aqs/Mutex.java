package me.w1992wishes.concurrency.aqs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 *
 * @author w1992wishes
 * @date 2017/6/1
 */
public class Mutex implements Lock {

    /**
     * 静态内部类，自定义同步器
     */
    private static class Sync extends AbstractQueuedSynchronizer {

        /**
         * 是否处于占用状态
         */
        @Override
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }

        /**
         * 当状态为0的时候获取锁
         */
        @Override
        public boolean tryAcquire(int acquires) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        /**
         * 释放锁，将当前状态设置为0
         */
        @Override
        protected boolean tryRelease(int releases) {
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        /**
         * 返回一个Condition，每个condition都包含了一个condition队列
         */
        Condition newCondition() {
            return new ConditionObject();
        }
    }

    /**
     * 仅需要将操作代理到Sync上即可
     */
    private final Sync sync = new Sync();

    @Override
    public void lock() {
        // 加锁操作，代理到acquire（模板方法）上就行，acquire 会调用我们重写的 tryAcquire 方法
        sync.acquire(1);
    }

    @Override
    public boolean tryLock() {
        // 释放锁，代理到 release（模板方法）上就行，release 会调用我们重写的 tryRelease 方法。
        return sync.tryAcquire(1);
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }

    public boolean isLocked(){
        return sync.isHeldExclusively();
    }

}