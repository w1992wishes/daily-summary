package me.w1992wishes.algorithm.cluster.sample1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @Author: w1992wishes
 * @Date: 2018/5/11 20:33
 */
public class CudaTest {
    private static List<float[]> points;

    private static void initDatas() throws IOException {
        points = new LinkedList<>();
        InputStream in = null;
        BufferedReader br = null;
        in = CudaTest.class.getClassLoader().getResourceAsStream("f2w.csv");
        br = new BufferedReader(new InputStreamReader(in));
        String line = br.readLine();
        // 读取每一行
        while (line != null && !"".equals(line)) {
            StringTokenizer tokenizer = new StringTokenizer(line, ",");

            int num = Integer.parseInt(tokenizer.nextToken());

            // f2w.csw中的第二列是要聚类的多维数据，先将数据读取出来进行转换，每一个维度的数据都放入list中
            String pointsStr = tokenizer.nextToken();
            int size = pointsStr.split("_").length;
            float[] dimensioins = new float[size];
            for (int i = 0; i < size; i++) {
                dimensioins[i] = (Float.parseFloat(pointsStr.split("_")[i]));
            }

            String imgUrl = tokenizer.nextToken();

            points.add(dimensioins);
            line = br.readLine();
        }
    }

    public static void main(String[] args) throws IOException {
        initDatas();
        CudaDBSCAN dbscan = new CudaDBSCAN();
        System.out.println("start run dbscan");
        dbscan.runDBSCAN(points,0.87f, 2);
    }
}
