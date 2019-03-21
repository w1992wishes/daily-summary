package me.w1992wishes.study.cluster.algorithm.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * @Author: w1992wishes
 * @Date: 2018/5/16 15:01
 */
public class CutCsvUtils {

    public static void cut(String in, String out, int num) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(in));
        BufferedWriter writer = new BufferedWriter(new FileWriter(out));
        String line = reader.readLine();
        int count = 1;
        while (line != null && count++<=num){
            writer.write(line);
            writer.newLine();
            line = reader.readLine();
        }
        writer.close();
        reader.close();
    }

    public static void main(String[] args) throws Exception {
        cut("data.csv", "30000.csv", 30000);
    }
}
