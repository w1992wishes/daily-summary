package me.w1992wishes.sync.standard.transport.exchanger;


import me.w1992wishes.common.exception.MyException;
import me.w1992wishes.sync.standard.constant.Key;
import me.w1992wishes.sync.standard.transport.record.Record;
import me.w1992wishes.sync.standard.transport.record.TerminateRecord;
import me.w1992wishes.sync.standard.transport.channel.Channel;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class BufferedRecordExchanger implements RecordSender, RecordReceiver {

    private static final Logger LOG = LogManager.getLogger(BufferedRecordExchanger.class);

	private final Channel channel;

	private final List<Record> buffer;

	private int bufferIndex = 0;

	private int bufferSize;

	public BufferedRecordExchanger(final Channel channel) {
		this.channel = channel;

        this.bufferSize = channel.getConfiguration().getInt(Key.TRANSPORT_EXCHANGER_BUFFERSIZE);
		this.buffer = new ArrayList<>();
	}

	@Override
	public void sendToWriter(Record record) {
		Validate.notNull(record, "record不能为空.");

        boolean isFull = this.bufferIndex >= this.bufferSize;
        if (isFull) {
            flush();
        }

		this.buffer.add(record);
		this.bufferIndex++;
	}

    @Override
	public Record getFromReader() throws MyException {

		boolean isEmpty = (this.bufferIndex >= this.buffer.size());
		if (isEmpty) {
			receive();
		}

		Record record = this.buffer.get(this.bufferIndex++);
		if (record instanceof TerminateRecord) {
			record = null;
		}
		return record;
	}

	private void receive() throws MyException {
		this.channel.pullAll(this.buffer);
		this.bufferIndex = 0;
	}

    @Override
    public void flush()  {
        LOG.info("buffersize 已满，放 {} 数据至 channel", bufferSize);
        this.channel.pushAll(this.buffer);
        this.buffer.clear();
        this.bufferIndex = 0;
    }
}
