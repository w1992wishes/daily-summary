package me.w1992wishes.spark.streaming.common.domain;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.io.Serializable;

/**
 * @author w1992wishes 2019/11/22 11:25
 */
@Data
public class MultiData implements Serializable {

    private String type;

    private String operation;

    private String operator;

    private String time;

    private JSONObject[] datas;

}
