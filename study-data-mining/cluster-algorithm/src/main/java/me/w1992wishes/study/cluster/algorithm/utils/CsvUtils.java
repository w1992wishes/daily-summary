package me.w1992wishes.study.cluster.algorithm.utils;

import java.io.*;
import java.util.StringTokenizer;

/**
 * @Author: w1992wishes
 * @Date: 2018/5/14 19:22
 */
public class CsvUtils {
    private static void changeCsvFormat(String srcFile, String destFile){
        try (BufferedReader reader = new BufferedReader(new FileReader(srcFile))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line, ",");
                writeLine(destFile, tokenizer.nextToken(), lineNum++);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeLine(String destFile, String datas, int lineNum){
        File csv = new File(destFile); // CSV数据文件
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(csv, true)); // 附加
            String[] line = datas.split(" ");
            // 添加新的数据行
            StringBuilder newLine = new StringBuilder();
            newLine.append(lineNum + ",");
            for (int i=1; i<line.length; i++){
                newLine.append(line[i]);
                if (i != line.length -1){
                    newLine.append("_");
                }else {
                    newLine.append(",");
                }
            }
            newLine.append(line[0]);
            bw.write(newLine.toString());
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        changeCsvFormat(args[0], args[1]);
    }
}
