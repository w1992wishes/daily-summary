package me.w1992wishes.algorithm.cluster.sample2;

/**
 * @Author: w1992wishes
 * @Date: 2018/5/12 14:03
 */
public class CudaTest {
    public static void main(String[] args) {
        CudaDBSCAN cudaDBSCAN = new CudaDBSCAN();
        // arg[0]->file.csv  arg[1]->dataCounts  args[2]->resultFile
        //cudaDBSCAN.runDBSCAN(args[0], 0.87f, 2, 20, 100);
        cudaDBSCAN.runDBSCAN(args[0], Integer.parseInt(args[1]), 0.87f, 2, 20, 100);
    }
}
