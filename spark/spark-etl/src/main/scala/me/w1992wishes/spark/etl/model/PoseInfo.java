package me.w1992wishes.spark.etl.model;

/**
 * @author w1992wishes 2019/2/28 13:47
 */
public class PoseInfo {
    /**
     * Pitch，上下俯仰角度
     */
    private Float pitch;

    /**
     * 人脸平面旋转
     */
    private Float roll;

    /**
     * Yaw，左右偏离角度
     */
    private Float yaw;

    public Float getPitch() {
        return pitch;
    }

    public void setPitch(Float pitch) {
        this.pitch = pitch;
    }

    public Float getRoll() {
        return roll;
    }

    public void setRoll(Float roll) {
        this.roll = roll;
    }

    public Float getYaw() {
        return yaw;
    }

    public void setYaw(Float yaw) {
        this.yaw = yaw;
    }
}
