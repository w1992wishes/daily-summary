package me.w1992wishes.sync.standard.constant;

/**
 * @author w1992wishes 2019/3/28 17:28
 */
public class Key {

    public static final String COLUMNS_MAPPING = "columns_mapping";

    public static final String READER = "reader";

    public static final String WRITER = "writer";

    public static final String SPECIAL_COLUMNS = "special_columns";

    public static final String TRANSPORT_CHANNEL_CAPACITY = "transport.channel.capacity";

    public static final String TRANSPORT_EXCHANGER_BUFFERSIZE = "transport.exchanger.bufferSize";

    public static final String JDBC_URL = "jdbcUrl";

    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";

    public static final String TABLE = "table";

    public static final String FETCH_SIZE = "fetchSize";

    public static final String BATCH_SIZE = "batchSize";

    // 自增策略：支持时间和 id 两种
    public static final String AUTO_INCREMENT_STRATEGY = "auto_increment.strategy";
    // 自增列
    public static final String AUTO_INCREMENT_COLUMN = "auto_increment.column";
    // timeRange
    public static final String AUTO_INCREMENT_TIMERANGE = "auto_increment.timeRange";
    // limit
    public static final String AUTO_INCREMENT_LIMIT = "auto_increment.limit";
}
