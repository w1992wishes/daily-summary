package me.w1992wishes.common.util;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.intellif.bigdata.common.util.DateUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The unique id has 64bits (long), default allocated as blow:<br>
 * <li>sign: The highest bit is 0
 * <li>delta seconds: The next 28 bits, represents delta seconds since a customer epoch(2016-05-20 00:00:00.000).
 * Supports about 8.7 years until to 2024-11-20 21:24:16
 * <li>worker id: The next 22 bits, represents the worker's id which assigns based on database, max id is about 420W
 * <li>sequence: The next 13 bits, represents a sequence within the same second, max for 8192/s<br><br>
 *
 * The {@link DefaultUidGenerator#parseUID(long)} is a tool method to parse the bits
 *
 * <pre>{@code
 * +------+----------------------+----------------+-----------+
 * | sign |     delta seconds    | worker node id | sequence  |
 * +------+----------------------+----------------+-----------+
 *   1bit          28bits              22bits         13bits
 * }</pre>
 *
 * You can also specified the bits by Spring property setting.
 * <li>timeBits: default as 28
 * <li>workerBits: default as 22
 * <li>seqBits: default as 13
 * <li>epochStr: Epoch date string format 'yyyy-MM-dd'. Default as '2016-05-20'<p>
 *
 * <b>Note that:</b> The total bits must be 64 -1
 *
 * @author yutianbao
 */
public class DefaultUidGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUidGenerator.class);

    private int timeBits;
    private int workerBits;
    private int seqBits;

    /**
     * Customer epoch, unit as second. For example 2016-05-20 (ms: 1463673600000)
     */
    private long epochSeconds = TimeUnit.MILLISECONDS.toSeconds(1463673600000L);

    /**
     * default
     */
    private BitsAllocator bitsAllocator = new BitsAllocator(28, 22, 13);
    /**
     * workerId
     */
    private long workerId;

    /**
     * Volatile fields caused by nextId()
     */
    private long sequence = 0L;
    private long lastSecond = -1L;

    public DefaultUidGenerator(int workerId) {
        // 初始化
        this.timeBits = bitsAllocator.getTimestampBits();
        this.workerBits = bitsAllocator.getWorkerIdBits();
        this.seqBits = bitsAllocator.getSequenceBits();

        this.workerId = workerId;
        if (workerId > bitsAllocator.getMaxWorkerId()) {
            throw new UidGenerateException("Worker id " + workerId + " exceeds the max " + bitsAllocator.getMaxWorkerId());
        }

        LOGGER.info("Initialized bits(1, {}, {}, {}) for workerID:{}", timeBits, workerBits, seqBits, workerId);
    }

    public long getUID() {
        try {
            return nextId();
        } catch (Exception e) {
            LOGGER.error("Generate unique id exception. ", e);
            throw new UidGenerateException(e);
        }
    }

    public String parseUID(long uid) {
        long totalBits = BitsAllocator.TOTAL_BITS;
        long signBits = bitsAllocator.getSignBits();
        long timestampBits = bitsAllocator.getTimestampBits();
        long workerIdBits = bitsAllocator.getWorkerIdBits();
        long sequenceBits = bitsAllocator.getSequenceBits();

        // parse UID
        long sequenceInSecond = (uid << (totalBits - sequenceBits)) >>> (totalBits - sequenceBits);
        long useWorkerId = (uid << (timestampBits + signBits)) >>> (totalBits - workerIdBits);
        long deltaSeconds = uid >>> (workerIdBits + sequenceBits);

        Date thatTime = new Date(TimeUnit.SECONDS.toMillis(epochSeconds + deltaSeconds));
        String thatTimeStr = DateUtil.formatByDateTimePattern(thatTime);

        // format as string
        return String.format("{\"UID\":\"%d\",\"timestamp\":\"%s\",\"workerId\":\"%d\",\"sequence\":\"%d\"}",
                uid, thatTimeStr, useWorkerId, sequenceInSecond);
    }

    /**
     * Get UID
     *
     * @return UID
     * @throws UidGenerateException in the case: Clock moved backwards; Exceeds the max timestamp
     */
    private synchronized long nextId() {
        long currentSecond = getCurrentSecond();

        // Clock moved backwards, refuse to generate uid
        if (currentSecond < lastSecond) {
            long refusedSeconds = lastSecond - currentSecond;
            throw new UidGenerateException("Clock moved backwards. Refusing for %d seconds", refusedSeconds);
        }

        // At the same second, increase sequence
        if (currentSecond == lastSecond) {
            sequence = (sequence + 1) & bitsAllocator.getMaxSequence();
            // Exceed the max sequence, we wait the next second to generate uid
            if (sequence == 0) {
                currentSecond = getNextSecond(lastSecond);
            }

            // At the different second, sequence restart from zero
        } else {
            sequence = 0L;
        }

        lastSecond = currentSecond;

        // Allocate bits for UID
        return bitsAllocator.allocate(currentSecond - epochSeconds, workerId, sequence);
    }

    /**
     * Get next millisecond
     */
    private long getNextSecond(long lastTimestamp) {
        long timestamp = getCurrentSecond();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentSecond();
        }

        return timestamp;
    }

    /**
     * Get current second
     */
    private long getCurrentSecond() {
        long currentSecond = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        if (currentSecond - epochSeconds > bitsAllocator.getMaxDeltaSeconds()) {
            throw new UidGenerateException("Timestamp bits is exhausted. Refusing UID generate. Now: " + currentSecond);
        }

        return currentSecond;
    }

    public void setTimeBits(int timeBits) {
        if (timeBits > 0) {
            this.timeBits = timeBits;
        }
    }

    public void setWorkerBits(int workerBits) {
        if (workerBits > 0) {
            this.workerBits = workerBits;
        }
    }

    public void setSeqBits(int seqBits) {
        if (seqBits > 0) {
            this.seqBits = seqBits;
        }
    }

    /**
     * default"2016-05-20";
     *
     * @param epochStr default = "2016-05-20";
     */
    public void setEpochStr(String epochStr) {
        if (StringUtils.isNotBlank(epochStr)) {
            this.epochSeconds = TimeUnit.MILLISECONDS.toSeconds(DateUtil.parseByDayPattern(epochStr).getTime());
        }
    }

    public void setBitsAllocator(BitsAllocator bitsAllocator) {
        this.bitsAllocator = bitsAllocator;
    }

    /**
     * Allocate 64 bits for the UID(long)<br>
     * sign (fixed 1bit) -> deltaSecond -> workerId -> sequence(within the same second)
     *
     * @author yutianbao
     */
    public static class BitsAllocator {
        /**
         * Total 64 bits
         */
        public static final int TOTAL_BITS = 1 << 6;

        /**
         * Bits for [sign-> second-> workId-> sequence]
         */
        private int signBits = 1;
        private final int timestampBits;
        private final int workerIdBits;
        private final int sequenceBits;

        /**
         * Max value for workId & sequence
         */
        private final long maxDeltaSeconds;
        private final long maxWorkerId;
        private final long maxSequence;

        /**
         * Shift for timestamp & workerId
         */
        private final int timestampShift;
        private final int workerIdShift;

        /**
         * Constructor with timestampBits, workerIdBits, sequenceBits<br>
         * The highest bit used for sign, so <code>63</code> bits for timestampBits, workerIdBits, sequenceBits
         */
        public BitsAllocator(int timestampBits, int workerIdBits, int sequenceBits) {
            // make sure allocated 64 bits
            int allocateTotalBits = signBits + timestampBits + workerIdBits + sequenceBits;
            if (TOTAL_BITS != allocateTotalBits) {
                throw new IllegalStateException("allocate not enough 64 bits");
            }

            // initialize bits
            this.timestampBits = timestampBits;
            this.workerIdBits = workerIdBits;
            this.sequenceBits = sequenceBits;

            // initialize max value
            this.maxDeltaSeconds = ~(-1L << timestampBits);
            this.maxWorkerId = ~(-1L << workerIdBits);
            this.maxSequence = ~(-1L << sequenceBits);

            // initialize shift
            this.timestampShift = workerIdBits + sequenceBits;
            this.workerIdShift = sequenceBits;
        }

        /**
         * Allocate bits for UID according to delta seconds & workerId & sequence<br>
         * <b>Note that: </b>The highest bit will always be 0 for sign
         */
        public long allocate(long deltaSeconds, long workerId, long sequence) {
            return (deltaSeconds << timestampShift) | (workerId << workerIdShift) | sequence;
        }

        /**
         * Getters
         */
        public int getSignBits() {
            return signBits;
        }

        public int getTimestampBits() {
            return timestampBits;
        }

        public int getWorkerIdBits() {
            return workerIdBits;
        }

        public int getSequenceBits() {
            return sequenceBits;
        }

        public long getMaxDeltaSeconds() {
            return maxDeltaSeconds;
        }

        public long getMaxWorkerId() {
            return maxWorkerId;
        }

        public long getMaxSequence() {
            return maxSequence;
        }

        public int getTimestampShift() {
            return timestampShift;
        }

        public int getWorkerIdShift() {
            return workerIdShift;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }

    /**
     * UidGenerateException
     *
     * @author yutianbao
     */
    private static class UidGenerateException extends RuntimeException {

        /**
         * Serial Version UID
         */
        private static final long serialVersionUID = -27048199131316992L;

        /**
         * Default constructor
         */
        public UidGenerateException() {
            super();
        }

        /**
         * Constructor with message & cause
         */
        private UidGenerateException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructor with message
         */
        private UidGenerateException(String message) {
            super(message);
        }

        /**
         * Constructor with message format
         */
        private UidGenerateException(String msgFormat, Object... args) {
            super(String.format(msgFormat, args));
        }

        /**
         * Constructor with cause
         */
        private UidGenerateException(Throwable cause) {
            super(cause);
        }

    }
}