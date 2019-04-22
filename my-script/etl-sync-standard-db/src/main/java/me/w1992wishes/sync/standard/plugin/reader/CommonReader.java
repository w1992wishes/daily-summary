package me.w1992wishes.sync.standard.plugin.reader;

import me.w1992wishes.common.constant.DataBaseType;
import me.w1992wishes.common.exception.MyException;
import me.w1992wishes.common.exception.MyRuntimeException;
import me.w1992wishes.common.util.Configuration;
import me.w1992wishes.common.util.DBUtils;
import me.w1992wishes.sync.standard.constant.Key;
import me.w1992wishes.sync.standard.element.*;
import me.w1992wishes.sync.standard.element.*;
import me.w1992wishes.sync.standard.handler.ColumnHandler;
import me.w1992wishes.sync.standard.transport.exchanger.RecordSender;
import me.w1992wishes.sync.standard.transport.record.DefaultRecord;
import me.w1992wishes.sync.standard.transport.record.Record;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author w1992wishes 2019/3/29 13:45
 */
public abstract class CommonReader implements Reader {

    private static final Logger LOG = LogManager.getLogger(CommonReader.class);

    // 额外需特殊处理的列及handler
    private List<String> specialCols;
    // handler map
    private Map<String, ColumnHandler> columnHandlerMap = new ConcurrentHashMap<>();
    // 类型
    DataBaseType dataBaseType;

    private String username;
    private String password;
    private String jdbcUrl;
    private int fetchSize;

    private String readRecordSql;

    CommonReader(Configuration configuration, String querySqlTemplate, List<String> specialColumns) {
        // 获取 reader 源配置
        Configuration readerConf = configuration.getConfiguration(Key.READER);
        this.username = readerConf.getString(Key.USERNAME);
        this.password = readerConf.getString(Key.PASSWORD);
        this.jdbcUrl = readerConf.getString(Key.JDBC_URL);
        this.fetchSize = readerConf.getInt(Key.FETCH_SIZE);

        String table = readerConf.getString(Key.TABLE);
        this.readRecordSql = String.format(querySqlTemplate, table);

        this.specialCols = specialColumns;
    }

    public void registerColumnHandler(String columnName, ColumnHandler columnHandler) {
        columnHandlerMap.put(columnName, columnHandler);
    }

    public void startRead(RecordSender recordSender) {
        Connection conn = DBUtils.getConnection(this.dataBaseType, jdbcUrl,
                username, password);

        ResultSet rs;
        int columnNumber;
        try {
            LOG.info("Already connect with db, now execute query.");
            rs = DBUtils.query(conn, this.readRecordSql, fetchSize);

            ResultSetMetaData metaData = rs.getMetaData();
            columnNumber = metaData.getColumnCount();
            while (rs.next()) {
                this.transportOneRecord(recordSender, rs,
                        metaData, columnNumber);
            }

            //目前大盘是依赖这个打印
            LOG.info("Finished read record by Sql: {}\n.", this.readRecordSql);

        } catch (Exception e) {
            throw new MyRuntimeException(e);
        } finally {
            DBUtils.closeDBResources(null, conn);
        }
    }

    private void transportOneRecord(RecordSender recordSender, ResultSet rs,
                                    ResultSetMetaData metaData, int columnNumber) throws MyException {
        Record record = buildRecord(rs, metaData, columnNumber);
        recordSender.sendToWriter(record);
    }

    private Record buildRecord(ResultSet rs, ResultSetMetaData metaData, int columnNumber) throws MyException {

        Record record = new DefaultRecord();
        try {
            for (int i = 1; i <= columnNumber; i++) {
                switch (metaData.getColumnType(i)) {
                    case Types.CHAR:
                    case Types.NCHAR:
                    case Types.VARCHAR:
                    case Types.LONGVARCHAR:
                    case Types.NVARCHAR:
                    case Types.LONGNVARCHAR:
                        record.addColumn(new StringColumn(rs.getString(i)));
                        break;

                    case Types.CLOB:
                    case Types.NCLOB:
                        record.addColumn(new StringColumn(rs.getString(i)));
                        break;

                    case Types.SMALLINT:
                    case Types.TINYINT:
                    case Types.INTEGER:
                    case Types.BIGINT:
                        record.addColumn(new LongColumn(rs.getString(i)));
                        break;

                    case Types.NUMERIC:
                    case Types.DECIMAL:
                        record.addColumn(new DoubleColumn(rs.getString(i)));
                        break;

                    case Types.FLOAT:
                    case Types.REAL:
                    case Types.DOUBLE:
                        record.addColumn(new DoubleColumn(rs.getString(i)));
                        break;

                    case Types.TIME:
                        record.addColumn(new DateColumn(rs.getTime(i)));
                        break;

                    // for mysql bug, see http://bugs.mysql.com/bug.php?id=35115
                    case Types.DATE:
                        if (metaData.getColumnTypeName(i).equalsIgnoreCase("year")) {
                            record.addColumn(new LongColumn(rs.getInt(i)));
                        } else {
                            record.addColumn(new DateColumn(rs.getDate(i)));
                        }
                        break;

                    case Types.TIMESTAMP:
                        record.addColumn(new DateColumn(rs.getTimestamp(i)));
                        break;

                    case Types.BINARY:
                    case Types.VARBINARY:
                    case Types.BLOB:
                    case Types.LONGVARBINARY:
                        record.addColumn(new BytesColumn(rs.getBytes(i)));
                        break;

                    // warn: bit(1) -> Types.BIT 可使用BoolColumn
                    // warn: bit(>1) -> Types.VARBINARY 可使用BytesColumn
                    case Types.BOOLEAN:
                    case Types.BIT:
                        record.addColumn(new BoolColumn(rs.getBoolean(i)));
                        break;

                    case Types.NULL:
                        String stringData = null;
                        if(rs.getObject(i) != null) {
                            stringData = rs.getObject(i).toString();
                        }
                        record.addColumn(new StringColumn(stringData));
                        break;

                    default:
                        throw new MyRuntimeException(
                                String.format(
                                        "您的配置文件中的列配置信息有误. 程序不支持数据库读取这种字段类型. 字段名:[%s], 字段名称:[%s], 字段Java类型:[%s]. 请尝试使用数据库函数将其转换datax支持的类型 或者不同步该字段 .",
                                        metaData.getColumnName(i),
                                        metaData.getColumnType(i),
                                        metaData.getColumnClassName(i)));
                }
            }

            // 特殊处理的列也需进行处理
            for (String specialCol : specialCols) {
                record.addColumn(columnHandlerMap.get(specialCol).getColumnValue(new DateColumn()));
            }
        } catch (Exception e) {
            throw new MyException(e);
        }
        return record;
    }
}
