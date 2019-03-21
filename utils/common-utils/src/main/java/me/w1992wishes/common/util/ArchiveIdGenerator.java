package me.w1992wishes.common.util;

import static java.lang.System.*;

/**
 * ArchiveIdGenerator 基于 Twitter_Snowflake<br>
 *
 * ArchiveIdGenerator 的结构如下(每部分用-分开):<br>
 * 0 - 0 - 0000000000 0000000000 0000000000 0000 - 00000000 - 0000000000 0000000000 <br>
 * 1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0<br>
 * 1位标识，区分哪方建立的档案：0 异构， 1 数据平台<br>
 * 34位时间戳(秒级)，注意，34位时间戳不是存储当前时间的时间戳，而是存储时间戳的差值（当前时间戳 - 开始时间戳)
 * 得到的值），这里的的开始时间戳，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下下面程序IdWorker类的startTime属性）。34位的时间戳，可以使用544年，年T = (1L << 34) / (60 * 60 * 24 * 365) = 544<br>
 * 8位的数据机器位，可以部署在256个节点<br>
 * 20位序列号，代表每一秒可以产生1048576 个不同的 ID<br>
 * 加起来刚好64位，为一个Long型。<br>
 * SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，经测试，SnowFlake每秒能够产生26万ID左右。
 *
 * @author w1992wishes 2019/02/27 17:31
 */
public class ArchiveIdGenerator {

    // ==============================Fields===========================================
    // 开始时间戳 (2019-02-27) 秒级
    private static final long TWEPOCH = 1551196800;

    // 区分哪方建立的档案 占用位数
    private static final long SIDE_ID_BITS = 1L;

    // 时间戳占用的位数
    private static final long TIME_BITS = 34L;

    // 机器id所占的位数
    private static final long WORKER_ID_BITS = 8L;

    // 序列在id中占的位数
    private static final long SEQUENCE_BITS = 20L;

    // 支持的最大代表方 id
    private static final long MAX_SIDE_ID = -1L ^ (-1L << SIDE_ID_BITS);

    // 支持的最大机器id，结果是255 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
    private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);

    // 生成序列的掩码，1048575
    private static final long SEQUENCE_MASK = -1L ^ (-1L << SEQUENCE_BITS);

    // side Id 像左位移 62 位
    private static final long SIDE_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + TIME_BITS;

    // 时间戳向左移28位(8+20)
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    // 机器ID向左移20位
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    // 哪方建立的档案 side Id：0 异构， 1 数据平台
    private long sideId;

    // 工作机器ID(0~255)
    private long workerId;

    // 毫秒内序列(0~1048575)
    private long sequence = 0L;

    /** 上次生成ID的时间戳 */
    private long lastTimestamp = -1L;

    //==============================Constructors=====================================
    /**
     * 构造函数
     * @param workerId 工作ID (0~255)
     */
    public ArchiveIdGenerator(long workerId, long sideId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        this.workerId = workerId;
        if (sideId > MAX_SIDE_ID || sideId < 0){
            throw new IllegalArgumentException(String.format("side Id can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        this.sideId = sideId;
    }

    // ==============================Methods==========================================
    /**
     * 获得下一个ID (该方法是线程安全的)
     * @return SnowflakeId
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw new UnsupportedOperationException(
                    String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        //如果是同一时间生成的，则进行秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            //秒内序列溢出
            if (sequence == 0) {
                //阻塞到下一秒,获得新的时间戳
                timestamp = tilNextSecs(lastTimestamp);
            }
        }
        //时间戳改变，秒内序列重置
        else {
            sequence = 0L;
        }

        //上次生成ID的时间戳
        lastTimestamp = timestamp;

        //移位并通过或运算拼到一起组成64位的ID
        return ((sideId << SIDE_ID_SHIFT)
                |(timestamp - TWEPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 阻塞到下一个秒，直到获得新的时间戳
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 当前时间戳
     */
    private long tilNextSecs(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 返回以秒为单位的当前时间
     * @return 当前时间(秒)
     */
    private long timeGen() {
        return currentTimeMillis()/1000;
    }

    //==============================Test=============================================
    /** 测试 */
    public static void main(String[] args) {

      ArchiveIdGenerator idWorker = new ArchiveIdGenerator(0, 1);
        for (int i = 0; i < 1000; i++) {
            long id = idWorker.nextId();
            out.println(Long.toBinaryString(id));
            out.println(id);
        }
    }
}