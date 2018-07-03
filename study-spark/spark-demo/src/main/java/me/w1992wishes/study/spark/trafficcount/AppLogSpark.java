package me.w1992wishes.study.spark.trafficcount;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;

import java.util.List;

/**
 * 读取app-log.txt中的内容，然后按设备号统计出每个设备号总的上行流量/总的下行流量，
 * 然后挑出前10个上行流量最大的设备号,打印出来
 *
 * @Author: w1992wishes
 * @Date: 2018/4/17 16:11
 */
public class AppLogSpark {
    public static void main(String[] args) {
        //1.创建spark配置文件及上下文对象
        SparkConf conf = new SparkConf().setAppName("appLogSpark").setMaster("local");
        JavaSparkContext sc = new JavaSparkContext(conf);

        //2.读取日志文件，并创建一个RDD，使用sparkContext方法textFile()可以读取本地文件或
        //HDFS文件，创建的RDD会包含文件中的所有内容数据
        JavaRDD<String> appLogRDD = sc.textFile("e:\\app-log.txt");

        //3.将appLogRDD映射成k-v形式<设备号,AccessLogInfo>的RDD
        //可以这样理解为List<deviceId,AccessLogInfo>，deviceId是可以对应多个AcessLogInfo的
        JavaPairRDD<String, AccessLogInfo> appLogPairRDD = mapAppLogRDD2Pair(appLogRDD);

        //4.将appLogPairRDD根据key进行聚合，并统计
        //可以这样理解List<deviceId,AccessLogInfo>聚合后,deviceId只有对应一个AccessLogInfo了,
        //AccessLogInfo里面的数据就是某个deviceId的总上行流量/总下行流量了
        JavaPairRDD<String, AccessLogInfo> aggregateLogPairRDD = aggregateByDevice(appLogPairRDD);

        //5.排序，按上行流量、上行流量、时间戳排序
        //因为JavaPairRDD中排序只有sortByKey(XX),而我们这是根据value排序的，所以需要
        //将JavaPairRDD<String, AccessLogInfo>转为JavaPairRDD<AccessLogInfo, String>的RDD,
        //而AccessLogInfo只是个普通的POJO，为了便于比较排序，定义个AccessLogSortKey对象
        JavaPairRDD<AccessLogSortKey, String> logSortRDD = mapRDDKey2SortKey(aggregateLogPairRDD);

        //6.实现排序
        JavaPairRDD<AccessLogSortKey, String> sortRDD = logSortRDD.sortByKey(false);//降序

        //7.取前10个
        List<Tuple2<AccessLogSortKey, String>> list = sortRDD.take(10);

        //打印
        for (Tuple2<AccessLogSortKey, String> t : list) {
            System.out.println(t._2 + "\t" + t._1.getTimestamp() + "\t" + t._1.getUpTraffic() + "\t" + t._1.getDownTraffic());
        }

        //关闭
        sc.close();
    }

    /**
     * 将appLogRDD映射成key-value形式的JavaPairRDD<String, AccessLogInfo>的RDD
     *
     * @param appLogRDD
     * @return
     */
    private static JavaPairRDD<String, AccessLogInfo> mapAppLogRDD2Pair(JavaRDD<String> appLogRDD) {
        return appLogRDD.mapToPair(new PairFunction<String, String, AccessLogInfo>() {

            private static final long serialVersionUID = 1L;

            //accessLog是对应文件中的一行数据
            public Tuple2<String, AccessLogInfo> call(String accessLog) throws Exception {
                //根据\t对一行的数据进行分割
                String[] data = accessLog.split("\t");
                long timestamp = Long.parseLong(data[0]);
                String deviceId = data[1];
                long upTraffic = Long.parseLong(data[2]);
                long downTraffic = Long.parseLong(data[3]);

                AccessLogInfo info = new AccessLogInfo(timestamp, upTraffic, downTraffic);
                return new Tuple2<String, AccessLogInfo>(deviceId, info);
            }
        });
    }

    /**
     * 将JavaPairRDD<String, AccessLogInfo>进行聚合
     *
     * @param appLogPairRDD
     * @return
     */
    private static JavaPairRDD<String, AccessLogInfo> aggregateByDevice(JavaPairRDD<String, AccessLogInfo> appLogPairRDD) {
        return appLogPairRDD.reduceByKey(new Function2<AccessLogInfo, AccessLogInfo, AccessLogInfo>() {

            private static final long serialVersionUID = 1L;

            public AccessLogInfo call(AccessLogInfo v1, AccessLogInfo v2) throws Exception {
                //最早的访问时间
                long timestamp = v1.getTimestamp() < v2.getTimestamp() ? v1.getTimestamp() : v2.getTimestamp();
                long upTraffic = v1.getUpTraffic() + v2.getUpTraffic();//总上行流量
                long downTraffic = v1.getDownTraffic() + v2.getDownTraffic();//总下行流量
                return new AccessLogInfo(timestamp, upTraffic, downTraffic);
            }
        });
    }

    /**
     * 将JavaPairRDD<String, AccessLogInfo>转为JavaPairRDD<AccessLogSortKey, String >便于后面的排序
     */
    private static JavaPairRDD<AccessLogSortKey, String> mapRDDKey2SortKey(JavaPairRDD<String, AccessLogInfo> aggregateRDD) {
        return aggregateRDD.mapToPair(new PairFunction<Tuple2<String, AccessLogInfo>, AccessLogSortKey, String>() {
            private static final long serialVersionUID = 1L;

            public Tuple2<AccessLogSortKey, String> call(Tuple2<String, AccessLogInfo> t) throws Exception {
                String deviceId = t._1;
                AccessLogInfo info = t._2;
                AccessLogSortKey sortKey = new AccessLogSortKey(info.getTimestamp(), info.getUpTraffic(), info.getDownTraffic());
                return new Tuple2<AccessLogSortKey, String>(sortKey, deviceId);
            }
        });
    }
}