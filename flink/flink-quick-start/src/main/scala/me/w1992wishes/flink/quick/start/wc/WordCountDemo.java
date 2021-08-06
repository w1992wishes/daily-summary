package me.w1992wishes.flink.quick.start.wc;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.AkkaOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.HeartbeatManagerOptions;
import org.apache.flink.configuration.WebOptions;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;

// wordcount 计数demo代码
public class WordCountDemo {
    public static void main(String[] args) throws Exception {
 
        // conf设置，避免调试的时候心跳超时
        Configuration configuration = new Configuration();
        configuration.setLong(HeartbeatManagerOptions.HEARTBEAT_INTERVAL, 10 * 60 * 1000);
        configuration.setLong(HeartbeatManagerOptions.HEARTBEAT_TIMEOUT, 20 * 60 * 1000);
        configuration.setString(AkkaOptions.ASK_TIMEOUT, "600 s");
        configuration.setString(AkkaOptions.WATCH_HEARTBEAT_INTERVAL, "600 s");
        configuration.setString(AkkaOptions.WATCH_HEARTBEAT_PAUSE, "1000 s");
        configuration.setString(AkkaOptions.TCP_TIMEOUT, "600 s");
        configuration.setString(AkkaOptions.TRANSPORT_HEARTBEAT_INTERVAL, "600 s");
        configuration.setString(AkkaOptions.LOOKUP_TIMEOUT, "600 s");
        configuration.setString(AkkaOptions.CLIENT_TIMEOUT, "600 s");
        configuration.setLong(WebOptions.TIMEOUT, 10 * 60 * 1000L);
 
        // local环境初始化
        StreamExecutionEnvironment streamExecutionEnvironment = StreamExecutionEnvironment.createLocalEnvironment(1, configuration);
 
        // 添加自定义的数据源(每秒随机产生一个范围在(0-10)的数字)
        DataStream<Integer> dataStream = streamExecutionEnvironment.addSource(new RandomSource()).returns(Integer.class);
        dataStream.map(new MapFunction<Integer, Tuple2<String, Integer>>() { // 生成 k-v对
            @Override
            public Tuple2<String, Integer> map(Integer value) throws Exception {
                return new Tuple2<>("i" + value, 1);
            }
        }).keyBy(0).timeWindow(Time.seconds(5)).reduce(new ReduceFunction<Tuple2<String, Integer>>() { // 每5s按照key进行统计计数
            @Override
            public Tuple2<String, Integer> reduce(Tuple2<String, Integer> value1, Tuple2<String, Integer> value2) throws Exception {
                value2.f1 = value1.f1 + value2.f1;
                return value2;
            }
        }).filter(new FilterFunction<Tuple2<String, Integer>>() {
            @Override
            public boolean filter(Tuple2<String, Integer> value) throws Exception {
                return value.f0.compareTo("i5") != 0;
            }
        }).print();
        streamExecutionEnvironment.execute("word count run");
    }
}