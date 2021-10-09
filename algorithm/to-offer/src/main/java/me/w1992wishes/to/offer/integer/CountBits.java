package me.w1992wishes.to.offer.integer;

/**
 * 给定一个非负整数 n ，请计算 0 到 n 之间的每个数字的二进制表示中 1 的个数，并输出一个数组。
 */
public class CountBits {

    /**
     * i >> 1会把最低位去掉，因此i >> 1是比i小的，同样是在前面的数组里算过。
     * <p>
     * 当 i 的最低位是0，则 i 中1的个数和i >> 1中1的个数相同；当i的最低位是1，i 中1的个数是i >> 1中1的个数再加1
     */
    public int[] countBits(int num) {
        int[] rs = new int[num + 1];
        for (int i = 0; i <= num; i++) {
            //注意i&1需要加括号
            rs[i] = rs[i >> 1] + (i & 1);
        }
        return rs;
    }

}
