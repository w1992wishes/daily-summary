package me.w1992wishes.hbase.common.util;

import org.apache.hadoop.hbase.util.Bytes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author wanqinfeng
 * @date 2019/7/7 15:03.
 */
public class Md5Utils {

    public static final int MD5_LENGTH = 16;

    /**
     * 信息摘要是安全的单向哈希函数，它接收随意大小的数据，输出固定长度的哈希值。
     *
     * @param s 数据
     * @return 固定长度的哈希值
     */
    public static byte[] md5sum(String s) {
        MessageDigest d;
        try {
            d = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available!", e);
        }

        return d.digest(Bytes.toBytes(s));
    }

}
