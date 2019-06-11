package me.w1992wishes.hadoop.map.reduce;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.db.DBConfiguration;
import org.apache.hadoop.mapreduce.lib.db.DBInputFormat;
import org.apache.hadoop.mapreduce.lib.db.DBOutputFormat;
import org.apache.hadoop.mapreduce.lib.db.DBWritable;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 为了方便 MapReduce 直接訪问关系型数据库（Mysql,Oracle）。Hadoop提供了DBInputFormat和DBOutputFormat两个类。
 *
 * @author Administrator
 */
public class MysqlToHdfsMR {

    /**
     * Writable 是为了与 MapReduce 进行对接，DBWritable 是为了与 MySQL 进行对接。
     *
     * 都要实现 write 和 readFields 函数，前者用于将一条数据写入数据流或者数据库中，而后者则从数据流/库中读取数据。
     *
     * Writable 接口是对数据流进行操作的，所以输入输出是 DataInput 和DataOutput 类对象；
     *
     * DBWritable 负责对数据库进行操作，所以输入格式是 ResultSet，输出格式是 PreparedStatement
     */
    private static class MysqlRecord implements DBWritable, Writable {
        protected int size;
        protected Date time;
        protected int input_speed;
        protected int output_speed;

        @Override
        public void write(PreparedStatement preparedStatement) throws SQLException {
            preparedStatement.setInt(1, this.size);
            preparedStatement.setDate(2, this.time);
            preparedStatement.setInt(3, this.input_speed);
            preparedStatement.setInt(4, this.output_speed);
        }

        @Override
        public void readFields(ResultSet resultSet) throws SQLException {
            this.size = resultSet.getInt(1);
            this.time = resultSet.getDate(2);
            this.input_speed = resultSet.getInt(3);
            this.output_speed = resultSet.getInt(4);
        }

        @Override
        public void write(DataOutput dataOutput) throws IOException {
            dataOutput.writeInt(this.size);
            Text.writeString(dataOutput, this.time.toString());
            dataOutput.writeInt(this.input_speed);
            dataOutput.writeInt(this.output_speed);
        }

        @Override
        public void readFields(DataInput dataInput) throws IOException {
            this.size = dataInput.readInt();
            this.time = Date.valueOf(Text.readString(dataInput));
            this.input_speed = dataInput.readInt();
            this.output_speed = dataInput.readInt();
        }

        @Override
        public String toString() {
            return String.format("%d\t%s\t%d\t%d", size, time.toString(), input_speed, output_speed);
        }
    }

    private static class SQLMapper extends Mapper<LongWritable, MysqlRecord, Text, IntWritable> {
        @Override
        protected void map(LongWritable key, MysqlRecord value, Context context)
                throws IOException, InterruptedException {
            Date d = value.time;
            int output = value.output_speed;
            context.write(new Text(d.toString()), new IntWritable(output));
        }
    }

    private static class MyReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable v : values) {
                sum += v.get();
            }
            context.write(key, new IntWritable(sum));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String host = StringUtils.isEmpty(args[0]) ? "localhost" : args[0];
        // 用DBConfiguration.configureDB 来设定连接 MySQL 所需要的一些认证信息。
        String url = "jdbc:mysql://" + host + ":3306/test";
        DBConfiguration.configureDB(
                conf,
                "com.mysql.jdbc.Driver",
                url,
                "root",
                "introcks1234");

        Job job = Job.getInstance(conf, "MysqlMR");
        job.setJarByClass(MysqlToHdfsMR.class);
        job.setMapperClass(SQLMapper.class);
        job.setReducerClass(MyReducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);

        // 设置输入类
        job.setInputFormatClass(DBInputFormat.class);
        // 再设置表名，字段名等信息。
        DBInputFormat.setInput(
                job,                // job
                MysqlRecord.class,  // input class
                "mysql_input",      // table name
                null,               // condition
                "time",             // order by
                "size", "time", "input_speed", "output_speed");            // fields
        Path outPath = new Path("output");
        FileSystem.get(conf).delete(outPath, true);
        FileOutputFormat.setOutputPath(job, outPath);

        job.waitForCompletion(true);
    }

}