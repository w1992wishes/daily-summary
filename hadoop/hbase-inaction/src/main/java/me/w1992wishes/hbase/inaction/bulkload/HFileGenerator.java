package me.w1992wishes.hbase.inaction.bulkload;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.HFileOutputFormat2;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

public class HFileGenerator {

    private static org.apache.hadoop.conf.Configuration conf;
    private static Connection connection;

    static {
        conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "master");
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        try {
            // 获得连接对象
            connection = ConnectionFactory.createConnection(conf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {

        Job job = Job.getInstance(conf, "HFile Generator");
        job.setJarByClass(HFileGenerator.class);
        job.setMapperClass(TestHFileMapper.class);
        job.setMapOutputKeyClass(ImmutableBytesWritable.class);
        job.setMapOutputValueClass(Put.class);
        String inputPath = "/test/data.txt";
        String outputPath = "/test/hfiles";
        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));
        RegionLocator regionLocator = connection.getRegionLocator(TableName.valueOf("users"));
        HFileOutputFormat2.configureIncrementalLoad(job, (HTable) connection.getTable(TableName.valueOf("users")), regionLocator);
        System.exit(job.waitForCompletion(true) ? 0 : 1);

    }

    public static class TestHFileMapper extends Mapper {
        private static final byte[] FAMILY_BYTE = Bytes.toBytes("c");
        private static final byte[] QUALIFIER_INDEX = Bytes.toBytes("time");
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] items = line.split(" ", -1);
            long userId = Long.parseLong(items[0]);
            long timestamp = Long.parseLong(items[1]);
            byte[] userIdBytes = Bytes.toBytes(userId);
            byte[] timestampBytes = Bytes.toBytes(timestamp);
            ImmutableBytesWritable rowKey = new ImmutableBytesWritable(userIdBytes);
            Put put = new Put(rowKey.copyBytes());
            put.addColumn(FAMILY_BYTE, QUALIFIER_INDEX, timestampBytes);
            context.write(rowKey, put);
        }
    }
}