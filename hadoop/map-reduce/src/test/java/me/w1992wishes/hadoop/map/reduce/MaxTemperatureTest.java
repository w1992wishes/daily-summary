package me.w1992wishes.hadoop.map.reduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mrunit.mapreduce.MapDriver;
import org.apache.hadoop.mrunit.mapreduce.ReduceDriver;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;


/**
 * @author w1992wishes 2019/6/3 10:11
 */

public class MaxTemperatureTest {

    @Test
    public void processesValidRecord() {
        Text value = new Text("2014010114");
        new MapDriver<LongWritable, Text, Text, IntWritable>()
                .withMapper(new MaxTemperature.TemperatureMapper())
                .withInput(new LongWritable(0), value)
                .withOutput(new Text("2014"), new IntWritable(14));

    }

    @Test
    public void returnsMaximumInValues() {
        new ReduceDriver<Text, IntWritable, Text, IntWritable>()
                .withReducer(new MaxTemperature.TemperatureReducer())
                .withInputKey(new Text("2014"))
                .withInputValues(Arrays.asList(new IntWritable(10), new IntWritable(14)))
                .withOutput(new Text("2014"), new IntWritable(14))
                .runTest();
    }
}

