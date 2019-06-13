package me.w1992wishes.jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * VM args:-XX:PermSize=10M -XX:MaxPermSize=10M
 * PermSize 方法区大小
 */
public class RuntimeContantPoolOOM {
    public static void main(String[] args) {
        //使用List保存对常量池字符串的应用，避免Full GC回收常量池的行为
        List<String> list = new ArrayList<String>();
        //10M的PermSize在int的范围足够产生OutOfMemoryError
        int i = 0;
        while (true) {
            list.add(String.valueOf(i++).intern());
        }
    }
}