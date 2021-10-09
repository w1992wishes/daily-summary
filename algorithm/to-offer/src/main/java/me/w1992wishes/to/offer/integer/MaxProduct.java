package me.w1992wishes.to.offer.integer;

import java.util.Arrays;

/**
 * 单词长度的最大乘积
 */
public class MaxProduct {
    /**
     * 给定一个字符串数组 words，请计算当两个字符串 words[i] 和 words[j] 不包含相同字符时，它们长度的乘积的最大值。
     * 假设字符串中只包含英语的小写字母。如果没有不包含相同字符的一对字符串，返回 0。
     */
    public int maxProduct(String[] words) {
        // 难点在判断两个字符串相等上
        // 可以利用位运算，一个int 32 位，小写字母26位，可以用int 型的位来表示字符串某个字母是否出现过
        // 用二进制的一位表示某一个字母是否出现过，0表示没出现，1表示出现。
        // "abcd"二进制表示00000000 00000000 00000000 00001111、"bc"二进制表示00000000 00000000 00000000 00000110。
        // 当两个字符串没有相同的字母时，二进制数与的结果为0。
        if (words == null || words.length == 0) {
            return 0;
        }

        int[] bits = new int[words.length];
        Arrays.fill(bits, 0);
        // 将每个字符串转换为 int 的bit 位上
        for (int i = 0; i < words.length; i++) {
            String cur = words[i];
            for (int j = 0; j < cur.length(); j++) {
                // cur.charAt(j) - 'a' 表示某个字母出现在哪个 bit 位上
                bits[i] |= 1 << (cur.charAt(j) - 'a');
            }
        }

        // 判断是否有相同的字母，做与运算，没有相同的结果为0
        int result = 0;
        for (int i = 0; i < bits.length - 1; i++) {
            for (int j = i + 1; j < bits.length; j++) {
                if ((bits[i] & bits[j]) == 0) {
                    result = Math.max(result, words[i].length() * words[j].length());
                }
            }
        }
        return result;
    }
}
