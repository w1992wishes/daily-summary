package me.w1992wishes.algorithm.cluster.jni;

public class DBSCANImpl implements DBSCAN {

    static {
        System.loadLibrary("dbscan_cpp");
    }

    @Override
    public native boolean initDatasFromFile(String fileName, int dataCounts) ;

    @Override
    public native void runDBSCAN(float eps, int minPts);

    @Override
    public native String saveDBSCAN();
}
