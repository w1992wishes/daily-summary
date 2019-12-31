package me.w1992wishes.common.domain;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 实体信息
 *
 * @author w1992wishes 2018/11/9 9:59
 */
public class EventFace implements Serializable {

    /**
     * 源系统 code
     */
    private String sysCode;

    /**
     * 小图唯一标识
     */
    private String thumbnailId;

    /**
     * 小图 url
     */
    private String thumbnailUrl;

    /**
     * 人脸小图对应大图唯一标识
     */
    private String imageId;

    /**
     * 大图 url
     */
    private String imageUrl;

    /**
     * 特征值
     */
    private byte[] featureInfo;

    /**
     * 算法版本
     */
    private Integer algoVersion;

    /**
     * 性别信息
     */
    private String genderInfo;

    /**
     * 年龄信息
     */
    private String ageInfo;

    /**
     * 发型信息
     */
    private String hairstyleInfo;

    /**
     * 帽子信息
     */
    private String hatInfo;

    /**
     * 眼镜信息
     */
    private String glassesInfo;

    /**
     * 族别信息
     */
    private String raceInfo;

    /**
     * 口罩信息
     */
    private String maskInfo;

    /**
     * 皮肤信息
     */
    private String skinInfo;

    /**
     * 角度信息
     */
    private String poseInfo;

    /**
     * 质量信息
     */
    private Float qualityInfo;

    /**
     * 人脸区域
     */
    private String targetRect;

    /**
     * 检测区域的浮点
     */
    private String targetRectFloat;

    /**
     * 人脸小图基于背景大图的坐标
     */
    private String targetThumbnailRect;

    /**
     * 一些脸部轮廓信息
     */
    private String landMarkInfo;

    /**
     * 采集源id
     */
    private Long sourceId;

    /**
     * 采集源类型
     */
    private Integer sourceType;

    /**
     * 地点
     */
    private String site;

    /**
     * 抓拍时间
     */
    private Timestamp time;

    /**
     * 事件创建时间
     */
    private Timestamp createTime;

    /**
     * 特征质量
     */
    private Float featureQuality;

    /**
     * 数据保存书剑
     */
    private Timestamp saveTime;

    /**
     * 扩展字段
     */
    private String column1;

    /**
     * 扩展字段
     */
    private String column2;

    /**
     * 扩展字段
     */
    private String column3;

    /**
     * 来源 sequence
     */
    private Long fromSequence;

    public String getSysCode() {
        return sysCode;
    }

    public void setSysCode(String sysCode) {
        this.sysCode = sysCode;
    }

    public String getThumbnailId() {
        return thumbnailId;
    }

    public void setThumbnailId(String thumbnailId) {
        this.thumbnailId = thumbnailId;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public byte[] getFeatureInfo() {
        return featureInfo;
    }

    public void setFeatureInfo(byte[] featureInfo) {
        this.featureInfo = featureInfo;
    }

    public Integer getAlgoVersion() {
        return algoVersion;
    }

    public void setAlgoVersion(Integer algoVersion) {
        this.algoVersion = algoVersion;
    }

    public String getGenderInfo() {
        return genderInfo;
    }

    public void setGenderInfo(String genderInfo) {
        this.genderInfo = genderInfo;
    }

    public String getAgeInfo() {
        return ageInfo;
    }

    public void setAgeInfo(String ageInfo) {
        this.ageInfo = ageInfo;
    }

    public String getHairstyleInfo() {
        return hairstyleInfo;
    }

    public void setHairstyleInfo(String hairstyleInfo) {
        this.hairstyleInfo = hairstyleInfo;
    }

    public String getHatInfo() {
        return hatInfo;
    }

    public void setHatInfo(String hatInfo) {
        this.hatInfo = hatInfo;
    }

    public String getGlassesInfo() {
        return glassesInfo;
    }

    public void setGlassesInfo(String glassesInfo) {
        this.glassesInfo = glassesInfo;
    }

    public String getRaceInfo() {
        return raceInfo;
    }

    public void setRaceInfo(String raceInfo) {
        this.raceInfo = raceInfo;
    }

    public String getMaskInfo() {
        return maskInfo;
    }

    public void setMaskInfo(String maskInfo) {
        this.maskInfo = maskInfo;
    }

    public String getSkinInfo() {
        return skinInfo;
    }

    public void setSkinInfo(String skinInfo) {
        this.skinInfo = skinInfo;
    }

    public String getPoseInfo() {
        return poseInfo;
    }

    public void setPoseInfo(String poseInfo) {
        this.poseInfo = poseInfo;
    }

    public Float getQualityInfo() {
        return qualityInfo;
    }

    public void setQualityInfo(Float qualityInfo) {
        this.qualityInfo = qualityInfo;
    }

    public String getTargetRect() {
        return targetRect;
    }

    public void setTargetRect(String targetRect) {
        this.targetRect = targetRect;
    }

    public String getTargetRectFloat() {
        return targetRectFloat;
    }

    public void setTargetRectFloat(String targetRectFloat) {
        this.targetRectFloat = targetRectFloat;
    }

    public String getLandMarkInfo() {
        return landMarkInfo;
    }

    public void setLandMarkInfo(String landMarkInfo) {
        this.landMarkInfo = landMarkInfo;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Integer getSourceType() {
        return sourceType;
    }

    public void setSourceType(Integer sourceType) {
        this.sourceType = sourceType;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public String getColumn1() {
        return column1;
    }

    public void setColumn1(String column1) {
        this.column1 = column1;
    }

    public String getColumn2() {
        return column2;
    }

    public void setColumn2(String column2) {
        this.column2 = column2;
    }

    public String getColumn3() {
        return column3;
    }

    public void setColumn3(String column3) {
        this.column3 = column3;
    }

    public Long getFromSequence() {
        return fromSequence;
    }

    public void setFromSequence(Long fromSequence) {
        this.fromSequence = fromSequence;
    }

    public Float getFeatureQuality() {
        return featureQuality;
    }

    public void setFeatureQuality(Float featureQuality) {
        this.featureQuality = featureQuality;
    }

    public Timestamp getSaveTime() {
        return saveTime;
    }

    public void setSaveTime(Timestamp saveTime) {
        this.saveTime = saveTime;
    }

    public String getTargetThumbnailRect() {
        return targetThumbnailRect;
    }

    public void setTargetThumbnailRect(String targetThumbnailRect) {
        this.targetThumbnailRect = targetThumbnailRect;
    }
}
