package me.w1992wishes.algorithm.cluster.jni;

/**
 * @Author: w1992wishes
 * @Date: 2018/5/18 10:18
 */
public interface DBSCAN {

    boolean initDatasFromFile(String fileName, int dataCounts);

    void runDBSCAN(float eps, int minPts);

    String saveDBSCAN();
}
