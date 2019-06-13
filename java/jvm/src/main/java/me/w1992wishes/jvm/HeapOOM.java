package me.w1992wishes.jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * Vm args:-Xms20M -Xmx20M -XX:+HeapDumpOnOutOfMemoryError
 * 堆的最小值参数-Xms，堆的最大值参数-Xmx
 * -XX:+HeapDumpOnOutOfMemoryError表示让虚拟机在出现内存异常时Dump出当前的内存堆转储快照
 * -XX:HeapDumpPath:为快照文件位置
 * Java 堆内存溢出测试，深入理解java虚拟机 p51
 */
//-Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=E:/
public class HeapOOM {
    static class OOMObject {

    }

    public static void main(String[] args) {
        List<OOMObject> list = new ArrayList<OOMObject>();
        while (true) {
            list.add(new OOMObject());
        }
    }
}