package me.w1992wishes.study.spark.trafficcount;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * @Author: w1992wishes
 * @Date: 2018/4/17 16:04
 */
public class DataFileGenerator {

    public static void main(String[] args) {
        //1.生成100个设备号
        List<String> deviceIds = new ArrayList<String>();
        for(int i=0;i<100;i++){
            deviceIds.add(getDeviceId());
        }

        //2.生成1000个时间戳，该时间戳对应的设备号及上行流量、下行流量
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<1000;i++){
            long timestamp = System.currentTimeMillis() - random.nextInt(10000);
            //随机获取一个设备号
            String deviceId = deviceIds.get(random.nextInt(100));
            //上行流量
            long upTraffic = random.nextInt(10000);
            //下行流量
            long downTraffic = random.nextInt(10000);

            sb.append(timestamp).append("\t").append(deviceId).append("\t").append(upTraffic)
                    .append("\t").append(downTraffic).append("\r\n");
        }

        //将数据写到文件
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("e:\\app-log.txt")));
            pw.write(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            if(pw != null){
                pw.close();
            }
        }

    }

    private static String getDeviceId(){
        return UUID.randomUUID().toString().replace("-", "");
    }

}
