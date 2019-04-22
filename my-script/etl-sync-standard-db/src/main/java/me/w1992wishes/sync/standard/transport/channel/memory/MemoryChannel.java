package me.w1992wishes.sync.standard.transport.channel.memory;

import me.w1992wishes.common.exception.MyException;
import me.w1992wishes.common.exception.MyRuntimeException;
import me.w1992wishes.common.util.Configuration;
import me.w1992wishes.sync.standard.constant.Key;
import me.w1992wishes.sync.standard.transport.record.Record;
import me.w1992wishes.sync.standard.transport.channel.Channel;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 内存Channel的具体实现，底层其实是一个ArrayBlockingQueue
 */
public class MemoryChannel extends Channel {

    private int bufferSize;

    private ArrayBlockingQueue<Record> queue;

    private ReentrantLock lock;

    private Condition notInsufficient;
    private Condition notEmpty;

    public MemoryChannel(final Configuration configuration) {
        super(configuration);

        this.queue = new ArrayBlockingQueue<>(capacity);
        this.bufferSize = configuration.getInt(Key.TRANSPORT_EXCHANGER_BUFFERSIZE);

        lock = new ReentrantLock();
        notInsufficient = lock.newCondition();
        notEmpty = lock.newCondition();
    }

    @Override
    protected void doPush(Record r) {
        try {
            this.queue.put(r);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected void doPushAll(Collection<Record> rs) {
        try {
            lock.lockInterruptibly();
            while (rs.size() > this.queue.remainingCapacity()) {
                notInsufficient.await(200L, TimeUnit.MILLISECONDS);
            }
            this.queue.addAll(rs);
            notEmpty.signalAll();
        } catch (InterruptedException e) {
            throw new MyRuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected Record doPull() {
        try {
            return this.queue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void doPullAll(Collection<Record> rs) throws MyException {
        assert rs != null;
        rs.clear();
        try {
            lock.lockInterruptibly();
            while (this.queue.drainTo(rs, bufferSize) <= 0) {
                notEmpty.await(200L, TimeUnit.MILLISECONDS);
            }
            notInsufficient.signalAll();
        } catch (InterruptedException e) {
            throw new MyException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        return this.queue.size();
    }

    @Override
    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

}
