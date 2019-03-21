package me.w1992wishes.study.cluster.algorithm.dbscan;

import java.util.List;

public class DataPoint {
    private String dataPointName; // 样本点名
    private List<Float> dimensioin; // 样本点的维度
    private boolean isKey; //是否是核心对象
    private boolean isVisited; // 是否已经被访问
    private int clusterId;

    public DataPoint() {

    }

    public DataPoint(List<Float> dimensioin, String dataPointName, boolean isKey) {
        this.dataPointName = dataPointName;
        this.dimensioin = dimensioin;
        this.isKey = isKey;
    }

    public String getDataPointName() {
        return dataPointName;
    }

    public void setDataPointName(String dataPointName) {
        this.dataPointName = dataPointName;
    }

    public List<Float> getDimensioin() {
        return dimensioin;
    }

    public void setDimensioin(List<Float> dimensioin) {
        this.dimensioin = dimensioin;
    }

    public boolean isKey() {
        return isKey;
    }

    public void setKey(boolean key) {
        isKey = key;
    }

    public boolean isVisited() {
        return isVisited;
    }

    public void setVisited(boolean visited) {
        isVisited = visited;
    }

    public int getClusterId() {
        return clusterId;
    }

    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }
}