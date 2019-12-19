package me.w1992wishes.spark.streaming.example.intellif.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author w1992wishes 2019/11/22 11:12
 */
@Data
public class TrackEventInfo implements Serializable {

    private String dataType;

    private String id;

    private String aid;

    private String bizCode;

    private Date time;

    /**
     * time 中天时间抽取格式: 2019-11-02
     */
    private String dt;

    private Date createTime;

    private String location;

    private String props;

    private String geoHash;

}
