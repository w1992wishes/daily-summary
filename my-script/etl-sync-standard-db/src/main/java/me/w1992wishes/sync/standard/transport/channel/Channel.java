package me.w1992wishes.sync.standard.transport.channel;

import me.w1992wishes.common.exception.MyException;
import me.w1992wishes.common.util.Configuration;
import me.w1992wishes.sync.standard.constant.Key;
import me.w1992wishes.sync.standard.transport.record.Record;
import org.apache.commons.lang3.Validate;

import java.util.Collection;

/**
 * Created by jingxing on 14-8-25.
 * <p/>
 * 统计和限速都在这里
 */
public abstract class Channel {

    protected Configuration configuration;

    protected int capacity;

    public Channel(final Configuration configuration) {
        this.configuration = configuration;
        this.capacity = configuration.getInt(Key.TRANSPORT_CHANNEL_CAPACITY, 2048);
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public void push(final Record r) {
        Validate.notNull(r, "record不能为空.");
        this.doPush(r);
    }

    public void pushAll(final Collection<Record> rs) {
        Validate.notNull(rs);
        Validate.noNullElements(rs);
        this.doPushAll(rs);
    }

    public Record pull() {
        return this.doPull();
    }

    public void pullAll(final Collection<Record> rs) throws MyException {
        Validate.notNull(rs);
        this.doPullAll(rs);
    }

    protected abstract void doPush(Record r);

    protected abstract void doPushAll(Collection<Record> rs);

    protected abstract Record doPull();

    protected abstract void doPullAll(Collection<Record> rs) throws MyException;

    public abstract int size();

    public abstract boolean isEmpty();

}
