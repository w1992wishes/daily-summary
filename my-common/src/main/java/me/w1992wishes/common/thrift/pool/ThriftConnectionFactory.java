package me.w1992wishes.common.thrift.pool;

import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.transport.*;

/**
 * Thrift Connection 工厂类
 */
public class ThriftConnectionFactory implements KeyedPooledObjectFactory<ThriftServer, TTransport> {
    private static final Logger logger = LogManager.getLogger(ThriftConnectionFactory.class);

    private int timeout;

    public ThriftConnectionFactory(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public PooledObject<TTransport> makeObject(ThriftServer thriftServer) throws Exception {
        logger.info("****** host {}, port {}", thriftServer.getHost(), thriftServer.getPort());
        TSocket tsocket = new TSocket(thriftServer.getHost(), thriftServer.getPort());
        tsocket.setTimeout(timeout);
        //TTransport transport = new TBufferedTransport(tsocket);

        tsocket.open();
        DefaultPooledObject<TTransport> result = new DefaultPooledObject<>(tsocket);
        logger.trace("Make new thrift connection: {}:{}", thriftServer.getHost(), thriftServer.getPort());

        return result;
    }

    @Override
    public boolean validateObject(ThriftServer thriftServer, PooledObject<TTransport> pooledObject) {
        if (pooledObject.getObject() != null) {
            if (pooledObject.getObject().isOpen()) {
                return true;
            }
            try {
                pooledObject.getObject().open();
                return true;
            } catch (TTransportException e) {
                logger.error("", e);
            }
        }
        return false;
    }

    @Override
    public void destroyObject(ThriftServer thriftServer, PooledObject<TTransport> pooledObject) throws Exception {
        TTransport transport = pooledObject.getObject();
        if (transport != null) {
            transport.close();
            logger.trace("Close thrift connection: {}:{}", thriftServer.getHost(), thriftServer.getPort());
        }
    }

    @Override
    public void activateObject(ThriftServer thriftServer, PooledObject<TTransport> pooledObject) throws Exception {
    }

    @Override
    public void passivateObject(ThriftServer arg0, PooledObject<TTransport> pooledObject) throws Exception {
    }
}
