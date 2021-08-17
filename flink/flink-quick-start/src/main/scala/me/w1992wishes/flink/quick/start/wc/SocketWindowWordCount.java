package me.w1992wishes.flink.quick.start.wc;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

public class SocketWindowWordCount {
    public static void main(String[] args) throws Exception {
        /** 需要连接的主机名和端口 */
        final String hostname;
        final int port;
        try {
            final ParameterTool params = ParameterTool.fromArgs(args);
            hostname = params.get("hostname");
            port = params.getInt("port");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Please run 'SocketWindowWordCount --host <host> --port <port>'");
            return;
        }

        /** 获取{@link StreamExecutionEnvironment}的具体实现的实例 */
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        /** 通过连接给定地址和端口, 获取数据流的数据源 */
        DataStream<String> text = env.socketTextStream(hostname, port);

        /** 对数据流中的数据进行解析、分组、窗口、以及聚合 */
        DataStream<WordWithCount> windowCounts = text
                .flatMap(new FlatMapFunction<String, WordWithCount>() {
                    @Override
                    public void flatMap(String value, Collector<WordWithCount> out) {
                        for (String word : value.split("\\s")) {
                            out.collect(new WordWithCount(word, 1L));
                        }
                    }
                })
                .keyBy("word")
                .timeWindow(Time.seconds(5), Time.seconds(1))
                .reduce(new ReduceFunction<WordWithCount>() {
                    @Override
                    public WordWithCount reduce(WordWithCount a, WordWithCount b) {
                        return new WordWithCount(a.word, a.count + b.count);
                    }
                });

        /** 打印出分析结果 */
        windowCounts.print();

        /** 懒加载，执行处理程序 */
        env.execute("Socket Window WordCount");
    }

    /**
     * 单词和统计次数的数据结构
     */
    public static class WordWithCount {
        public String word;
        public long count;

        public WordWithCount(String word, long count) {
            this.word = word;
            this.count = count;
        }

        @Override
        public String toString() {
            return word + " : " + count;
        }
    }
}