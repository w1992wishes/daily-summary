package me.w1992wishes.common.thrift.failover;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.EvictingQueue;
import me.w1992wishes.common.thrift.pool.ThriftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author w1992wishes 2019/6/20 16:14
 */
public class FailoverStrategy<T> {

    private final Logger logger = LogManager.getLogger(getClass());

    private static final int DEFAULT_FAIL_COUNT = 10;
    private static final long DEFAULT_FAIL_DURATION = TimeUnit.MINUTES.toMillis(1);
    private static final long DEFAULT_RECOVER_DURATION = TimeUnit.MINUTES.toMillis(1);

    private final long failDuration;
    private final Cache<T, Boolean> failedList;
    private final LoadingCache<T, EvictingQueue<Long>> failCountMap;

    /**
     * 使用默认 failover 策略
     */
    public FailoverStrategy() {
        this(DEFAULT_FAIL_COUNT, DEFAULT_FAIL_DURATION, DEFAULT_RECOVER_DURATION);
    }

    /**
     * 自定义 failover 策略
     * @param failCount 失败次数
     * @param failDuration 失效持续时间
     * @param recoverDuration 恢复持续时间
     */
    public FailoverStrategy(final int failCount, long failDuration, long recoverDuration) {
        this.failDuration = failDuration;
        this.failedList = CacheBuilder.newBuilder().weakKeys().expireAfterWrite(recoverDuration, TimeUnit.MILLISECONDS).build();
        this.failCountMap = CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<T, EvictingQueue<Long>>() {
            @Override
            public EvictingQueue<Long> load(T key) throws Exception {
                return EvictingQueue.create(failCount);
            }
        });
    }

    public void fail(T object) {
        logger.info("Server {}:{} failed.", ((ThriftServer)object).getHost(),((ThriftServer)object).getPort());
        boolean addToFail = false;
        try {
            EvictingQueue<Long> evictingQueue = failCountMap.get(object);
            synchronized (evictingQueue) {
                evictingQueue.add(System.currentTimeMillis());
                if (evictingQueue.remainingCapacity() == 0 && evictingQueue.element() >= (System.currentTimeMillis() - failDuration)) {
                    addToFail = true;
                }
            }
        } catch (ExecutionException e) {
            logger.error("Ops.", e);
        }
        if (addToFail) {
            failedList.put(object, Boolean.TRUE);
            logger.info("Server {}:{} failed. Add to fail list.", ((ThriftServer)object).getHost(), ((ThriftServer)object).getPort());
        }
    }

    public Set<T> getFailed() {
        return failedList.asMap().keySet();
    }

}
