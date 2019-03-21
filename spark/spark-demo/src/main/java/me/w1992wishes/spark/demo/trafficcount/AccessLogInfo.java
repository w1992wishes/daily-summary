package me.w1992wishes.spark.demo.trafficcount;

import scala.Serializable;

/**
 * @Author: w1992wishes
 * @Date: 2018/4/17 16:02
 */
public class AccessLogInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    //没有设备号，是因为这对象是站在设备角度看的，k-v形式<设备号,AccessLogInfo>

    private long timestamp;//时间戳
    private long upTraffic;//上行流量
    private long downTraffic;//下行流量

    public AccessLogInfo() {

    }

    public AccessLogInfo(long timestamp, long upTraffic, long downTraffic) {
        this.timestamp = timestamp;
        this.upTraffic = upTraffic;
        this.downTraffic = downTraffic;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getUpTraffic() {
        return upTraffic;
    }

    public void setUpTraffic(long upTraffic) {
        this.upTraffic = upTraffic;
    }

    public long getDownTraffic() {
        return downTraffic;
    }

    public void setDownTraffic(long downTraffic) {
        this.downTraffic = downTraffic;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }
}
