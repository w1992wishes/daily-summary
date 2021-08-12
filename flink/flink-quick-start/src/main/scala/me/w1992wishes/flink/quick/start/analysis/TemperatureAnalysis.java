package me.w1992wishes.flink.quick.start.analysis;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.accumulators.AverageAccumulator;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.functions.source.FromElementsFunction;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TemperatureAnalysis {

    // 作业名称
    private static final String JOB_NAME = TemperatureAnalysis.class.getSimpleName();
    // 解析字符串时间
    private static final ThreadLocal<SimpleDateFormat> FMT = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    // 输入的温度数据
    private static final List<String> DATA = Arrays.asList(
            "T1,2020-01-30 19:00:00,22",
            "T1,2020-01-30 19:00:01,25",
            "T1,2020-01-30 19:00:03,28",
            "T1,2020-01-30 19:00:06,26",
            "T1,2020-01-30 19:00:05,27",
            "T1,2020-01-30 19:00:12,31"
    );

    public static void main(String[] args) {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment()
                // 每隔5秒进行一次CheckPoint，事件传递语义为恰好一次
                .enableCheckpointing(5000, CheckpointingMode.EXACTLY_ONCE);
        // 使用事件时间
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        try {
            // 从集合中创建数据源
            env.addSource(new FromElementsFunction<>(Types.STRING.createSerializer(env.getConfig()), DATA), Types.STRING)
                    .map(TemperatureBean::of)// 将字符串时间转换为Temperature类型的POJO
                    .filter(Objects::nonNull)// 排除空数据
                    .assignTimestampsAndWatermarks(WatermarkStrategy.<TemperatureBean>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                            .withTimestampAssigner((TemperatureBean event, long timestamp) -> event.getTs(FMT.get())))// 提取Temperature类型事件的事件时间字段（cdt）作为注册水位线的时间戳
                    .keyBy(TemperatureBean::getKey)// 根据Key（ID_yyyy-MM-dd）分组，按照每个温度传感器每天的所有温度数据放到一个分区中
                    .timeWindow(Time.seconds(5))// 5秒的滚动窗口
                    .aggregate(new AggregateFunction<TemperatureBean, Acc, String>() {// 对每个窗口的温度数据进行聚合运算（平均）
                        @Override
                        public Acc createAccumulator() {
                            return new Acc();
                        }

                        @Override
                        public Acc add(TemperatureBean value, Acc acc) {
                            acc.setKey(value.getKey());
                            acc.add(value.getTemperature());
                            return acc;
                        }

                        @Override
                        public String getResult(Acc acc) {
                            return acc.toString();
                        }

                        @Override
                        public Acc merge(Acc a, Acc b) {
                            b.add(a.getLocalValue());
                            return b;
                        }
                    })
                    .addSink(new PrintSinkFunction<>());// 从控制台中打印
            env.execute(JOB_NAME);// 执行作业
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class TemperatureBean {
        private String sensorId;
        private String cdt;
        private double temperature;

        public static TemperatureBean of(String line) {
            return of(",", line);
        }

        public static TemperatureBean of(String delimiter, String line) {
            if (StringUtils.isNotEmpty(line)) {
                String[] values = line.split(delimiter, 3);
                return new TemperatureBean(values[0], values[1], Double.parseDouble(values[2]));
            }
            return null;
        }

        public TemperatureBean() {
        }

        public TemperatureBean(String sensorId, String cdt, double temperature) {
            this.sensorId = sensorId;
            this.cdt = cdt;
            this.temperature = temperature;
        }

        public String getKey() {
            return getSensorId() + "_" + getDay();
        }

        public long getTs(SimpleDateFormat sdf) {
            try {
                return sdf.parse(cdt).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return -1;
        }

        public String getDay() {
            return cdt.substring(0, 10);
        }

        public String getSensorId() {
            return sensorId;
        }

        public void setSensorId(String sensorId) {
            this.sensorId = sensorId;
        }

        public String getCdt() {
            return cdt;
        }

        public void setCdt(String cdt) {
            this.cdt = cdt;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        @Override
        public String toString() {
            return String.join(", ", sensorId, cdt, Double.toString(temperature));
        }
    }

    static class Acc extends AverageAccumulator {
        private String key;

        public void setKey(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return String.join(",", key, Double.toString(super.getLocalValue()));
        }
    }
}
