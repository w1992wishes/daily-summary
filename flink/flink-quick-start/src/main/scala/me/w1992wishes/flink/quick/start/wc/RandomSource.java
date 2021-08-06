package me.w1992wishes.flink.quick.start.wc;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;

import java.util.Random;

// 数据源产生器
public class RandomSource extends RichSourceFunction<Integer> {
    private Random random;
    private volatile Boolean running = false;
 
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        random = new Random();
        running = true;
    }
 
    @Override
    public void close() throws Exception {
        super.close();
        random = null;
    }
 
    @Override
    public void run(SourceContext<Integer> sourceContext) throws Exception {
        while (running) {
            Integer out = random.nextInt(10);
            sourceContext.collect(out);
            Thread.sleep(1000);
        }
    }
 
    @Override
    public void cancel() {
        running = false;
    }
}