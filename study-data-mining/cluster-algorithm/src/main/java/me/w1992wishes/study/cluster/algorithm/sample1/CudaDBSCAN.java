package me.w1992wishes.study.cluster.algorithm.sample1;

import java.util.List;

/**
 * @Author: w1992wishes
 * @Date: 2018/5/11 17:02
 */
public class CudaDBSCAN {

    static {
        System.loadLibrary("CudaDBSCAN");
    }

    /**
     * 本地方法调用CUDA
     *
     * @param points 数据点，每个float[]保存一张图片的所有特征点
     * @param eps 邻域半径
     * @param minPoint 邻域内规定的最小点数
     */
    public native void runDBSCAN(List<float[]> points, float eps, int minPoint);

}
