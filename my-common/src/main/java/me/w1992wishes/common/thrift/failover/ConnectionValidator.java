package me.w1992wishes.common.thrift.failover;

import org.apache.thrift.transport.TTransport;

/**
 * @author w1992wishes 2019/6/20 16:13
 */
public interface ConnectionValidator {

    boolean isValid(TTransport object);

}
