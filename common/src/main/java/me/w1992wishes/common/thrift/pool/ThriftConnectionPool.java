package me.w1992wishes.common.thrift.pool;

import org.apache.thrift.transport.TTransport;


/**
 * Thrift 连接池抽象接口
 */
public interface ThriftConnectionPool {
	
	TTransport getConnection(ThriftServer thriftServer);

    void returnConnection(ThriftServer thriftServer, TTransport transport);

    void returnBrokenConnection(ThriftServer thriftServer, TTransport transport);

	void close();

    void clear(ThriftServer thriftServer);

}
