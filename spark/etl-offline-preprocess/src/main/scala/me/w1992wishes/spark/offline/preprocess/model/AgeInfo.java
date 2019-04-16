package me.w1992wishes.spark.offline.preprocess.model;

/**
 * @author w1992wishes 2019/2/28 11:52
 */
public class AgeInfo {

    private Integer value;

    private Float confidence;

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }
}
