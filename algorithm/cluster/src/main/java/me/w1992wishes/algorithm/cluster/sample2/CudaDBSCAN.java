package me.w1992wishes.algorithm.cluster.sample2;

/**
 * @Author: w1992wishes
 * @Date: 2018/5/12 14:03
 */
public class CudaDBSCAN {
    static {
        System.loadLibrary("CudaDBSCAN");
    }

    /**
     * 本地调用cuda dbscan
     *
     * @param fileName 获取数据的文件名
     * @param count 取聚类数量
     * @param eps 邻域半径
     * @param minPoint 邻域内规定的最小点数
     * @param block_num GPU block数
     * @param thread_num GPU每个block线程数
     */
    public native void runDBSCAN(String fileName, int count, float eps, int minPoint, int block_num, int thread_num);
}
