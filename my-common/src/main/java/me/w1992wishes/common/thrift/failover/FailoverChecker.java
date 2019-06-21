package me.w1992wishes.common.thrift.failover;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.w1992wishes.common.thrift.pool.ThriftConnectionPool;
import me.w1992wishes.common.thrift.pool.ThriftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.transport.TTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author w1992wishes 2019/6/20 15:53
 */
public class FailoverChecker {

    private static final Logger LOG = LogManager.getLogger(FailoverChecker.class);

    private volatile List<ThriftServer> serverList;
    private FailoverStrategy<ThriftServer> failoverStrategy = new FailoverStrategy<>();
    private ThriftConnectionPool poolProvider;
    private ConnectionValidator connectionValidator;
    private ScheduledExecutorService checkExecutor;

    public FailoverChecker(ConnectionValidator connectionValidator) {
        this.connectionValidator = connectionValidator;
    }

    public void setConnectionPool(ThriftConnectionPool poolProvider) {
        this.poolProvider = poolProvider;
    }

    public void startChecking() {
        if (connectionValidator != null) {
            ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("Fail Check Worker").build();
            checkExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
            checkExecutor.scheduleAtFixedRate(new Checker(), 5000, 5000, TimeUnit.MILLISECONDS);
        }
    }

    private class Checker implements Runnable {
        @Override
        public void run() {
            for (ThriftServer thriftServer : getAvailableServers()) {
                TTransport tt = null;
                boolean valid = false;
                try {
                    tt = poolProvider.getConnection(thriftServer);
                    valid = connectionValidator.isValid(tt);
                } catch (Exception e) {
                    LOG.warn(e.getMessage(), e);
                } finally {
                    if (tt != null) {
                        if (valid) {
                            poolProvider.returnConnection(thriftServer, tt);
                        } else {
                            failoverStrategy.fail(thriftServer);
                            poolProvider.returnBrokenConnection(thriftServer, tt);
                        }
                    } else {
                        failoverStrategy.fail(thriftServer);
                    }
                }
            }
        }
    }

    public void setServerList(List<ThriftServer> serverList) {
        this.serverList = serverList ;
    }

    public List<ThriftServer> getAvailableServers() {
        List<ThriftServer> returnList = new ArrayList<>();
        Set<ThriftServer> failedServers = failoverStrategy.getFailed();
        for (ThriftServer thriftServer : serverList) {
            if (!failedServers.contains(thriftServer)) {
                returnList.add(thriftServer);
            }
        }
        return returnList;
    }

    public FailoverStrategy<ThriftServer> getFailoverStrategy() {
        return failoverStrategy;
    }

    public ConnectionValidator getConnectionValidator() {
        return connectionValidator;
    }

    public void stopChecking() {
        if (checkExecutor != null) {
            checkExecutor.shutdown();
        }
    }

}
