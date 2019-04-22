package me.w1992wishes.sync.standard.plugin.writer;

import me.w1992wishes.common.constant.DataBaseType;
import me.w1992wishes.common.exception.MyException;
import me.w1992wishes.common.exception.MyRuntimeException;
import me.w1992wishes.common.util.Configuration;
import me.w1992wishes.common.util.DBUtils;
import me.w1992wishes.sync.standard.constant.Key;
import me.w1992wishes.sync.standard.element.Column;
import me.w1992wishes.sync.standard.transport.exchanger.RecordReceiver;
import me.w1992wishes.sync.standard.transport.record.Record;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * @author w1992wishes 2019/3/29 14:54
 */
abstract class CommonWriter implements Writer {

    private static final Logger LOG = LogManager.getLogger(CommonWriter.class);

    private String table;
    private String username;
    private String password;
    private String jdbcUrl;
    private int batchSize;

    private List<String> columns;
    private int columnNumber;
    // 字段名，字段类型，字段类型名
    private Triple<List<String>, List<Integer>, List<String>> resultSetMetaData;

    DataBaseType dataBaseType;

    private String writeRecordSql;

    CommonWriter(Configuration configuration, List<String> columns, String sqlTemplate) {
        // 获取 writer 源配置
        Configuration writerConf = configuration.getConfiguration(Key.WRITER);
        this.table = writerConf.getString(Key.TABLE);
        this.username = writerConf.getString(Key.USERNAME);
        this.password = writerConf.getString(Key.PASSWORD);
        this.jdbcUrl = writerConf.getString(Key.JDBC_URL);
        this.batchSize = writerConf.getInt(Key.BATCH_SIZE);

        this.columns = columns;
        this.columnNumber = columns.size();

        // sql 需更新
        this.writeRecordSql = String.format(sqlTemplate, this.table);
    }

    public void startWrite(RecordReceiver recordReceiver) {
        Connection connection = DBUtils.getConnection(this.dataBaseType, this.jdbcUrl, username, password);

        startWriteWithConnection(recordReceiver, connection);
    }

    private void startWriteWithConnection(RecordReceiver recordReceiver, Connection connection) {

        // 用于写入数据的时候的类型根据目的表字段类型转换
        this.resultSetMetaData = DBUtils.getColumnMetaData(connection, this.table, StringUtils.join(this.columns, ","));

        List<Record> writeBuffer = new ArrayList<>(this.batchSize);
        try {
            Record record;
            while ((record = recordReceiver.getFromReader()) != null) {
                if (record.getColumnNumber() != this.columnNumber) {
                    // 源头读取字段列数与目的表字段写入列数不相等，直接报错
                    throw new MyRuntimeException(
                            String.format(
                                    "列配置信息有错误. 因为您配置的任务中，源头读取字段数:%s 与 目的表要写入的字段数:%s 不相等. 请检查您的配置并作出修改.",
                                    record.getColumnNumber(),
                                    this.columnNumber));
                }

                writeBuffer.add(record);

                if (writeBuffer.size() >= batchSize) {
                    doBatchInsert(connection, writeBuffer);
                    writeBuffer.clear();
                }
            }
            if (!writeBuffer.isEmpty()) {
                doBatchInsert(connection, writeBuffer);
                writeBuffer.clear();
            }
        } catch (Exception e) {
            throw new MyRuntimeException(e);
        } finally {
            writeBuffer.clear();
            DBUtils.closeDBResources(null, null, connection);
        }
    }

    private void doBatchInsert(Connection connection, List<Record> buffer)
            throws SQLException {
        PreparedStatement preparedStatement = null;
        try {
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(this.writeRecordSql);

            for (Record record : buffer) {
                preparedStatement = fillPreparedStatement(preparedStatement, record);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            connection.commit();
            LOG.info("batch insert data sieze {}", buffer.size());
        } catch (SQLException e) {
            LOG.warn("回滚此次写入, 采用每次写入一行方式提交. 因为:" + e.getMessage());
            LOG.warn(e.getNextException());
            connection.rollback();
            doOneInsert(connection, buffer);
        } catch (Exception e) {
            throw new MyRuntimeException(e);
        } finally {
            DBUtils.closeDBResources(preparedStatement, null);
        }
    }

    private void doOneInsert(Connection connection, List<Record> buffer) {
        PreparedStatement preparedStatement = null;
        try {
            connection.setAutoCommit(true);
            preparedStatement = connection.prepareStatement(this.writeRecordSql);

            for (Record record : buffer) {
                try {
                    preparedStatement = fillPreparedStatement(preparedStatement, record);
                    preparedStatement.execute();
                } catch (SQLException e) {
                    LOG.debug(e.toString());
                } finally {
                    // 最后不要忘了关闭 preparedStatement
                    preparedStatement.clearParameters();
                }
            }
        } catch (Exception e) {
            throw new MyRuntimeException(e);
        } finally {
            DBUtils.closeDBResources(preparedStatement, null);
        }
    }

    // 直接使用了两个类变量：columnNumber,resultSetMetaData
    private PreparedStatement fillPreparedStatement(PreparedStatement preparedStatement, Record record)
            throws SQLException, MyException {
        for (int i = 0; i < this.columnNumber; i++) {
            int columnSqltype = this.resultSetMetaData.getMiddle().get(i);
            preparedStatement = fillPreparedStatementColumnType(preparedStatement, i, columnSqltype, record.getColumn(i));
        }
        return preparedStatement;
    }

    private PreparedStatement fillPreparedStatementColumnType(PreparedStatement preparedStatement, int columnIndex, int columnSqltype, Column column) throws SQLException, MyException {
        java.util.Date utilDate;
        switch (columnSqltype) {
            case Types.CHAR:
            case Types.NCHAR:
            case Types.CLOB:
            case Types.NCLOB:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
                preparedStatement.setString(columnIndex + 1, column
                        .asString());
                break;

            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.NUMERIC:
            case Types.DECIMAL:
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
                String strValue = column.asString();
                if ("".equals(strValue)) {
                    preparedStatement.setString(columnIndex + 1, null);
                } else {
                    preparedStatement.setString(columnIndex + 1, strValue);
                }
                break;

            //tinyint is a little special in some database like mysql {boolean->tinyint(1)}
            case Types.TINYINT:
                Long longValue = column.asLong();
                if (null == longValue) {
                    preparedStatement.setString(columnIndex + 1, null);
                } else {
                    preparedStatement.setString(columnIndex + 1, longValue.toString());
                }
                break;

            // for mysql bug, see http://bugs.mysql.com/bug.php?id=35115
            case Types.DATE:
                if (this.resultSetMetaData.getRight().get(columnIndex)
                        .equalsIgnoreCase("year")) {
                    if (column.asBigInteger() == null) {
                        preparedStatement.setString(columnIndex + 1, null);
                    } else {
                        preparedStatement.setInt(columnIndex + 1, column.asBigInteger().intValue());
                    }
                } else {
                    java.sql.Date sqlDate = null;
                    try {
                        utilDate = column.asDate();
                    } catch (MyException e) {
                        throw new SQLException(String.format(
                                "Date 类型转换错误：[%s]", column));
                    }

                    if (null != utilDate) {
                        sqlDate = new java.sql.Date(utilDate.getTime());
                    }
                    preparedStatement.setDate(columnIndex + 1, sqlDate);
                }
                break;

            case Types.TIME:
                java.sql.Time sqlTime = null;
                try {
                    utilDate = column.asDate();
                } catch (MyException e) {
                    throw new SQLException(String.format(
                            "TIME 类型转换错误：[%s]", column));
                }

                if (null != utilDate) {
                    sqlTime = new java.sql.Time(utilDate.getTime());
                }
                preparedStatement.setTime(columnIndex + 1, sqlTime);
                break;

            case Types.TIMESTAMP:
                java.sql.Timestamp sqlTimestamp = null;
                try {
                    utilDate = column.asDate();
                } catch (MyException e) {
                    throw new SQLException(String.format(
                            "TIMESTAMP 类型转换错误：[%s]", column));
                }

                if (null != utilDate) {
                    sqlTimestamp = new java.sql.Timestamp(
                            utilDate.getTime());
                }
                preparedStatement.setTimestamp(columnIndex + 1, sqlTimestamp);
                break;

            case Types.BINARY:
            case Types.VARBINARY:
            case Types.BLOB:
            case Types.LONGVARBINARY:
                preparedStatement.setBytes(columnIndex + 1, column
                        .asBytes());
                break;

            case Types.BOOLEAN:
                preparedStatement.setString(columnIndex + 1, column.asString());
                break;

            // warn: bit(1) -> Types.BIT 可使用setBoolean
            // warn: bit(>1) -> Types.VARBINARY 可使用setBytes
            case Types.BIT:
                if (this.dataBaseType == DataBaseType.MySql) {
                    preparedStatement.setBoolean(columnIndex + 1, column.asBoolean());
                } else {
                    preparedStatement.setString(columnIndex + 1, column.asString());
                }
                break;
            default:
                throw new MyRuntimeException(
                        String.format(
                                "您的配置文件中的列配置信息有误. 因为DataX 不支持数据库写入这种字段类型. 字段名:[%s], 字段类型:[%d], 字段Java类型:[%s]. 请修改表中该字段的类型或者不同步该字段.",
                                this.resultSetMetaData.getLeft()
                                        .get(columnIndex),
                                this.resultSetMetaData.getMiddle()
                                        .get(columnIndex),
                                this.resultSetMetaData.getRight()
                                        .get(columnIndex)));
        }
        return preparedStatement;
    }
}
