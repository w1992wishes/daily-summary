package me.w1992wishes.hbase.inaction.mapreduce;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * @author Administrator
 */
public class ReadHbaseDataToHDFS extends Configured implements Tool {

    public static void main(String[] args) throws Exception {

        int run = ToolRunner.run(new ReadHbaseDataToHDFS(), args);
        System.exit(run);

    }

    @Override
    public int run(String[] arg0) throws Exception {

        Configuration conf = HBaseConfiguration.create();
        conf.set("fs.defaultFS", "hdfs://192.168.11.73/");
        conf.set("hbase.zookeeper.quorum", "192.168.11.72:2181");
        System.setProperty("HADOOP_USER_NAME", "hadoop");
        FileSystem fs = FileSystem.get(conf);

        Job job = Job.getInstance(conf);

        job.setJarByClass(ReadHbaseDataToHDFS.class);

        // 取对业务有用的数据 info,age
        Scan scan = new Scan();
        scan.addColumn("info".getBytes(), "age".getBytes());

        TableMapReduceUtil.initTableMapperJob(
                "student".getBytes(), // 指定表名
                scan, // 指定扫描数据的条件
                HbaseToHDFSMapper.class, // 指定mapper class
                Text.class,     // outputKeyClass mapper阶段的输出的key的类型
                IntWritable.class, // outputValueClass mapper阶段的输出的value的类型
                job, // job对象
                false
        );


        job.setReducerClass(HbaseToHDFSReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);

        Path outputPath = new Path("/student/avg/");

        if (fs.exists(outputPath)) {
            fs.delete(outputPath, true);
        }

        FileOutputFormat.setOutputPath(job, outputPath);

        boolean isDone = job.waitForCompletion(true);

        return isDone ? 0 : 1;
    }

    public static class HbaseToHDFSMapper extends TableMapper<Text, IntWritable> {

        Text outKey = new Text("age");
        IntWritable outValue = new IntWritable();

        // key是hbase中的行键
        // value是hbase中的所行键的所有数据
        @Override
        protected void map(ImmutableBytesWritable key, Result value, Context context)
                throws IOException, InterruptedException {

            boolean isContainsColumn = value.containsColumn("info".getBytes(), "age".getBytes());

            if (isContainsColumn) {

                List<Cell> listCells = value.getColumnCells("info".getBytes(), "age".getBytes());
                System.out.println("listCells:\t" + listCells);
                Cell cell = listCells.get(0);
                System.out.println("cells:\t" + cell);

                byte[] cloneValue = CellUtil.cloneValue(cell);
                String ageValue = Bytes.toString(cloneValue);
                outValue.set(Integer.parseInt(ageValue));

                context.write(outKey, outValue);

            }

        }

    }

    public static class HbaseToHDFSReducer extends Reducer<Text, IntWritable, Text, DoubleWritable> {

        DoubleWritable outValue = new DoubleWritable();

        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {

            int count = 0;
            int sum = 0;
            for (IntWritable value : values) {
                count++;
                sum += value.get();
            }

            double avgAge = sum * 1.0 / count;
            outValue.set(avgAge);
            context.write(key, outValue);
        }

    }

}