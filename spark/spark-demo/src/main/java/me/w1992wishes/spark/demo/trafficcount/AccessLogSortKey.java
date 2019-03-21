package me.w1992wishes.spark.demo.trafficcount;

import scala.Serializable;
import scala.math.Ordered;

/**
 *
 * 因为有排序要求，而上面的AccessLogInfo.java是没有定义排序比较的，所以定义个AccessLogSortKey.java
 *
 * @Author: w1992wishes
 * @Date: 2018/4/17 16:05
 */
public class AccessLogSortKey implements Ordered<AccessLogSortKey>, Serializable {

    private static final long serialVersionUID = 1L;

    private long timestamp;//时间戳
    private long upTraffic;//上行流量
    private long downTraffic;//下行流量

    public AccessLogSortKey(){

    }

    public AccessLogSortKey(long timestamp, long upTraffic, long downTraffic) {
        this.timestamp = timestamp;
        this.upTraffic = upTraffic;
        this.downTraffic = downTraffic;
    }

    @Override
    public boolean $less(AccessLogSortKey other) {
        if(upTraffic < other.upTraffic){
            return true;
        }else if(upTraffic == other.upTraffic && downTraffic < other.downTraffic	){
            return true;
        }else if(upTraffic == other.upTraffic && downTraffic == other.downTraffic && timestamp < other.timestamp	){
            return true;
        }
        return false;
    }

    @Override
    public boolean $less$eq(AccessLogSortKey other) {
        if($less(other)){
            return true;
        }else if(upTraffic == other.upTraffic && downTraffic == other.downTraffic && timestamp == other.timestamp){
            return true;
        }
        return false;
    }

    @Override
    public boolean $greater(AccessLogSortKey other) {
        if(upTraffic > other.upTraffic){
            return true;
        }else if(upTraffic == other.upTraffic && downTraffic > other.downTraffic	){
            return true;
        }else if(upTraffic == other.upTraffic && downTraffic == other.downTraffic && timestamp > other.timestamp	){
            return true;
        }
        return false;
    }

    @Override
    public boolean $greater$eq(AccessLogSortKey other) {
        if($greater(other)){
            return true;
        }else if(upTraffic == other.upTraffic && downTraffic == other.downTraffic && timestamp == other.timestamp){
            return true;
        }
        return false;
    }

    @Override
    public int compare(AccessLogSortKey other) {
        if(upTraffic - other.upTraffic != 0){
            return (int)(upTraffic - other.upTraffic);
        }
        if(downTraffic - other.downTraffic != 0){
            return (int)(downTraffic - other.downTraffic);
        }
        if(timestamp - other.timestamp != 0){
            return (int)(timestamp - other.timestamp);
        }
        return 0;
    }

    @Override
    public int compareTo(AccessLogSortKey other) {
        if(upTraffic - other.upTraffic != 0){
            return (int)(upTraffic - other.upTraffic);
        }
        if(downTraffic - other.downTraffic != 0){
            return (int)(downTraffic - other.downTraffic);
        }
        if(timestamp - other.timestamp != 0){
            return (int)(timestamp - other.timestamp);
        }
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (downTraffic ^ (downTraffic >>> 32));
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        result = prime * result + (int) (upTraffic ^ (upTraffic >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AccessLogSortKey other = (AccessLogSortKey) obj;
        if (downTraffic != other.downTraffic)
            return false;
        if (timestamp != other.timestamp)
            return false;
        if (upTraffic != other.upTraffic)
            return false;
        return true;
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
