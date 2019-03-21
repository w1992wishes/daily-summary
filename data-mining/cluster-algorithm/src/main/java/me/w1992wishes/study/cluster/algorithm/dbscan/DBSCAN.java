package me.w1992wishes.study.cluster.algorithm.dbscan;

import java.io.*;
import java.util.*;

/**
 * 在DBSCANClustering2之上对mergeCluster方法进行改良，DBSCANClustering2方法可能存在误差
 *
 * Created by wanqinfeng on 2018/4/22.
 */
public class DBSCAN {
    //半径
    private static double Epislon = 0.87;
    //密度、最小点个数
    private static int MinPts = 2;

    private List<DataPoint> points = new ArrayList<>();

    private void initData(String fileName, int length) {
        InputStream in = null;
        BufferedReader br = null;
        try {
            in = this.getClass().getClassLoader().getResourceAsStream(fileName);
            br = new BufferedReader(new InputStreamReader(in));
            String line = br.readLine();
            // 读取每一行
            int num=1;
            while (line != null && !line.equals("")) {
                StringTokenizer tokenizer = new StringTokenizer(line, ",");

                // 第一列是要聚类的多维数据，先将数据读取出来进行转换，每一个维度的数据都放入list中
                String pointsStr =  tokenizer.nextToken();
                List<Float> dimensioin = new ArrayList<>();
                if (pointsStr != null && !pointsStr.equals("")){
                    for (String pointStr : pointsStr.split("_")){
                        dimensioin.add(Float.parseFloat(pointStr));
                    }
                }

                String imgUrl = tokenizer.nextToken();

                points.add(new DataPoint(dimensioin, imgUrl, false));
                // 读取指定行数
                if (length == num++){
                    break;
                }
                line = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //计算两点之间的欧氏距离
    private double euclideanDistance(DataPoint a, DataPoint b) {
        List<Float> dim1 = a.getDimensioin();
        List<Float> dim2 = b.getDimensioin();
        double distance = 0.0;
        if (dim1.size() == dim2.size()) {
            for (int i = 0; i < dim1.size(); i++) {
                double temp = Math.pow((Double.parseDouble(String.valueOf(dim1.get(i))) -  Double.parseDouble(String.valueOf(dim2.get(i)))), 2);
                distance = distance + temp;
            }
            distance = Math.pow(distance, 0.5);
        }
        return distance;
    }

    //获取当前点的邻居
    private List<DataPoint> obtainNeighbors(DataPoint current, List<DataPoint> points) {
        List<DataPoint> neighbors = new LinkedList<>();
        for (DataPoint point : points) {
            double distance = euclideanDistance(current, point);
            if (distance <= Epislon) {
                neighbors.add(point);
            }
        }
        return neighbors;
    }

    private void mergeCluster(DataPoint point, List<DataPoint> neighbors,
                              int clusterId, List<DataPoint> points) {
        point.setClusterId(clusterId);

        while (!neighbors.isEmpty()){
            DataPoint neighbor = neighbors.get(0);
            if (!neighbor.isVisited()){
                neighbor.setVisited(true);
                List<DataPoint> nneighbors = obtainNeighbors(neighbor, points);
                if (nneighbors.size() > MinPts) {
                    for (DataPoint nneighbor : nneighbors){
                        if (!neighbors.contains(nneighbor)){
                            neighbors.add(nneighbor);
                        }
                    }
                }
            }
            //未被聚类的点归入当前聚类中
            if (neighbor.getClusterId() <= 0) {
                neighbor.setClusterId(clusterId);
            }
            neighbors.remove(neighbor);
        }
    }

    private void cluster(List<DataPoint> points) {
        System.out.println("points counts " + points.size());
        // clusterId初始为0表示未分类,分类后设置为一个正数,如果设置为-1表示噪声
        int clusterId = 0;
        // 遍历所有的点
        for (DataPoint point : points) {
            // 遍历过就跳过
            if (point.isVisited()) {
                continue;
            }
            point.setVisited(true);
            // 找到其邻域所有点
            List<DataPoint> neighbors = obtainNeighbors(point, points);
            // 邻域中的点大于MinPts，则为核心点
            if (neighbors.size() >= MinPts) {
                // 满足核心对象条件的点创建一个新簇
                clusterId = point.getClusterId() <= 0 ? (++clusterId) : point.getClusterId();
                // 处理该簇内所有未被标记为已访问的点，对簇进行扩展
                mergeCluster(point, neighbors, clusterId, points);
            } else {
                // 未满足核心对象条件的点暂时当作噪声处理
                if (point.getClusterId() <= 0) {
                    point.setClusterId(-1);
                }
            }
        }
    }

    private void saveHtml(List<DataPoint> points, String resultFile) throws IOException {
        Collections.sort(points, new Comparator<DataPoint>() {
            @Override
            public int compare(DataPoint o1, DataPoint o2) {
                return Integer.valueOf(o1.getClusterId()).compareTo(o2.getClusterId());
            }
        });
        BufferedWriter resultHtml = new BufferedWriter(new FileWriter(resultFile));
        resultHtml.write("div{width:350px; height:350px; border: 1px solid #ff0000;}");
        for (DataPoint point : points) {
            resultHtml.write("<div style='float:left'>"
                    + "<img src='" + point.getDataPointName() + "'/>"
                    + "<p>" + point.getClusterId() + "</p></div>");
        }
        resultHtml.close();
    }

    private void runDBSCAN(List<DataPoint> points) throws IOException {
        long start = System.currentTimeMillis();
        cluster(points);
        System.out.println("speed time: " + (System.currentTimeMillis() - start));
    }

    public static void main(String[] args) throws IOException {
        DBSCAN builder = new DBSCAN();
        // arg[0]->file.csv  arg[1]->dataCounts  args[2]->resultFile
        builder.initData(args[0],Integer.parseInt(args[1]));
        System.out.print("dbscan cluster ");
        builder.runDBSCAN(builder.points);
        builder.saveHtml(builder.points, args[2]);
    }
}
