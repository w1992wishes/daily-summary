package me.w1992wishes.jvm;

/**
 * -XX:+PrintGCDetails
 */
public class GCTest {

    public static void main(String[] args) {
        byte[] allocation1, allocation2;
        allocation1 = new byte[59800 * 1024];
        allocation2 = new byte[900*1024];
    }
}
