package me.w1992wishes.study.cluster.algorithm.kmeans;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @Author: w1992wishes
 * @Date: 2018/4/20 16:31
 */
public class KMeans {

    private Random random;
    private List<Point> dataset; //数据集
    private List<Point> centers; //中心质点集

    public KMeans(int k) throws IOException {
        initDataSet();
        initCenters(k);
    }

    /**
     * 初始化数据集
     *
     * @throws IOException
     */
    private void initDataSet() throws IOException {
        dataset = new ArrayList<Point>();
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader( this.getClass().getClassLoader().getResourceAsStream("data.txt")));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            String[] s = line.split(" ");
            Point point = new Point();
            point.setX(Double.parseDouble(s[0]));
            point.setY(Double.parseDouble(s[1]));
            point.setName(s[2]);
            dataset.add(point);
        }
    }

    /**
     * 初始化k个质心
     *
     * @param k
     */
    public void initCenters(int k) {
        if (dataset == null) {
            try {
                initDataSet();
            } catch (IOException e) {
                throw new RuntimeException("init dataSet failure");
            }
        }
        centers = new ArrayList<Point>();
        // 随机从样本集合中选取k个样本点作为聚簇中心
        for (int i = 0; i < k; i++) {
            random = new Random();
            int index = random.nextInt(dataset.size());
            centers.add(dataset.get(index));
        }
    }

    /**
     * 把数据集中的每个点归到离它最近的那个质心
     *
     * @return
     */
    public Map<Point, List<Point>> kcluster() {

        //上一次的聚簇中心 和 新的聚簇中心
        Map<Point, List<Point>> lastClusterCenterMap = null;
        Map<Point, List<Point>> nowClusterCenterMap = new HashMap<>();

        for (Point center : centers) {
            nowClusterCenterMap.put(center, new ArrayList<>());
        }

        // 这块是给每个Point找到离最近的质点
        while (true) {
            for (Point point : dataset) {
                double shortest = Double.MAX_VALUE;
                Point center = null;
                for (Point clusteringCenter : nowClusterCenterMap.keySet()) {
                    double distance = distance(point, clusteringCenter);
                    if (distance < shortest) {
                        shortest = distance;
                        center = clusteringCenter;
                    }
                }
                nowClusterCenterMap.get(center).add(point);
            }

            //如果结果与上一次相同，则整个过程结束
            if (isEqualCenter(lastClusterCenterMap, nowClusterCenterMap)) {
                break;
            }

            lastClusterCenterMap = nowClusterCenterMap;
            nowClusterCenterMap = new HashMap<>();
            //把中心点移到其所有成员的平均位置处,并构建新的聚簇中心
            for (Map.Entry<Point, List<Point>> entry : lastClusterCenterMap.entrySet()) {
                nowClusterCenterMap.put(getNewCenterPoint(entry.getValue()), new ArrayList<>());
            }
        }
        return nowClusterCenterMap;
    }

    /**
     * 使用欧氏距离计算两点之间距离
     *
     * @param point1
     * @param point2
     * @return
     */
    private double distance(Point point1, Point point2) {
        double distance = Math.pow((point1.getX() - point2.getX()), 2)
                + Math.pow((point1.getY() - point2.getY()), 2);
        distance = Math.sqrt(distance);
        return distance;
    }

    /**
     * 判断前后两次是否是相同的聚簇中心，若是则程序结束，否则继续,知道相同
     *
     * @param lastClusterCenterMap
     * @param nowClusterCenterMap
     * @return
     */
    private boolean isEqualCenter(Map<Point, List<Point>> lastClusterCenterMap,
                                  Map<Point, List<Point>> nowClusterCenterMap) {
        if (lastClusterCenterMap == null) {
            return false;
        } else {
            for (Point lastCenter : lastClusterCenterMap.keySet()) {
                if (!nowClusterCenterMap.containsKey(lastCenter)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 计算新的中心
     *
     * @param value
     * @return
     */
    private Point getNewCenterPoint(List<Point> value) {
        double sumX = 0.0, sumY = 0.0;
        for (Point point : value) {
            sumX += point.getX();
            sumY += point.getY();
        }
        Point point = new Point();
        point.setX(sumX / value.size());
        point.setY(sumY / value.size());
        return point;
    }

    public static void main(String[] args) throws IOException {
        KMeans kmeansClustering = new KMeans(4);

        Map<Point, List<Point>> result = kmeansClustering.kcluster();

        for (Map.Entry<Point, List<Point>> entry : result.entrySet()) {
            System.out.println("===============center：" + entry.getKey() + "================");
            for (Point point : entry.getValue()) {
                System.out.println(point.getName());
            }
        }
    }

    static class Point {
        private double X;
        private double Y;
        private String name;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public double getX() {
            return X;
        }
        public void setX(double x) {
            X = x;
        }
        public double getY() {
            return Y;
        }
        public void setY(double y) {
            Y = y;
        }

        @Override
        public boolean equals(Object obj) {
            Point point = (Point) obj;
            if (this == point){
                return true;
            }
            if (this.getX() == point.getX() && this.getY() == point.getY() && this.getName() == point.getName()) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "(" + X + "," + Y + ")";
        }

        @Override
        public int hashCode() {
            return (int) (X+Y);
        }
    }
}
