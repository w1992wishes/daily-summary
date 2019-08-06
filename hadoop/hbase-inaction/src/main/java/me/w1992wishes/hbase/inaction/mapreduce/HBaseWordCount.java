package me.w1992wishes.hbase.inaction.mapreduce;
 
import java.io.IOException;

import me.w1992wishes.hbase.common.util.HBaseUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
 
public class HBaseWordCount {
    
    public static class HBaseMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
 
        private final static IntWritable ONE = new IntWritable(1);
        private Text word = new Text();
 
        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String[] words = value.toString().split(" ");
            for ( String w : words) {
                word.set(w);
                context.write(word, ONE);
            }
        }
    }
    
    public static class HBaseReducer extends TableReducer<Text, IntWritable, NullWritable> {
 
        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable value : values) {
                sum += value.get();
            }
            
            // Put实例化，每个词存一行
            Put put = new Put(key.getBytes());
            // 列族为content,列名为count,列值为单词的数目
            put.addColumn("content".getBytes(), "count".getBytes(), String.valueOf(sum).getBytes());
            
            context.write(NullWritable.get(), put);
        }
        
    }
    
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        
        if (args.length != 3) {
            System.out.println("args error");
            System.exit(0);
        }
        
        String input = args[0];
        String jobName = args[1];
        String tableName = args[2];
        
        // 创建数据表
        HBaseUtils.createTable(tableName, "content");
        
        // 配置MapReduce(或者将hadoop和hbase的相关配置文件引入项目)
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "192.168.199.128:9000");
        conf.set("mapred.job.tracker", "192.168.199.128:9001");
        conf.set("hbase.zookeeper.quorum", "192.168.199.128");
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        conf.set(TableOutputFormat.OUTPUT_TABLE, tableName);
        
        // 配置任务
        Job job = Job.getInstance(conf, jobName);
        job.setJarByClass(HBaseWordCount.class);
        job.setMapperClass(HBaseMapper.class);
        job.setReducerClass(HBaseReducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TableOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path(input));
        
        //执行MR任务
        boolean result = job.waitForCompletion(true);
        System.exit(result ? 0 : 1);
    }
 
}