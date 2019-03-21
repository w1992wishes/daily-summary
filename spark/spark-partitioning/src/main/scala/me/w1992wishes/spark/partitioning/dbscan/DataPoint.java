package me.w1992wishes.spark.partitioning.dbscan;

import java.io.Serializable;
import java.util.Arrays;

public class DataPoint implements Serializable {
	private static final long serialVersionUID = -1804416488338257597L;
	
	private String dataPointName; // 样本点名
    private double dimensioin[]; // 样本点的维度
    private boolean isKey; //是否是核心对象
    private boolean isVisited; // 是否已经被访问
    private int clusterId;

    public DataPoint() {

    }

    public DataPoint(double[] dimensioin, String dataPointName, boolean isKey) {
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

    public double[] getDimensioin() {
        return dimensioin;
    }

    public void setDimensioin(double[] dimensioin) {
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

	@Override
	public String toString() {
		return "DataPoint [dataPointName=" + dataPointName + ", dimensioin=" + Arrays.toString(dimensioin) + ", isKey="
				+ isKey + ", isVisited=" + isVisited + ", clusterId=" + clusterId + "]";
	}
	
	public String toSimpleString() {
		return dataPointName + "=" + clusterId;
	}
    
}