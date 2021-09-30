package me.w1992wishes.algorithm.dynamic;

import java.util.Arrays;

/**
 * 购买长钢条，将其切割为短钢条出售，不同长度的钢条对应不同的价格，希望得到最佳的切割方案使利润最大化。
 * <p>
 * 长度 i	1	2	3	4	5	6	7	8	9	10
 * 价格 p	1	5	8	9	10	17	17	20	24	30
 */
public class RodCut {

    /**
     * 递归解法
     *
     * @param p 价格
     * @param n 长度
     * @return 利润最大值
     */
    public static int cut(int[] p, int n) {
        // 最优解由最优子解法组成
        if (n == 0) {
            return 0;
        }
        int q = -1;
        for (int i = 1; i <= n; i++) {
            q = Math.max(q, p[i - 1] + cut(p, n - i));
        }
        return q;
    }

    /**
     * 带备忘录迭代，记忆已经计算过的值
     *
     * @param p 价格
     * @param n 长度
     * @return 利润最大值
     */
    public static int cutMemo(int[] p, int n) {
        int[] mem = new int[n + 1];
        Arrays.fill(mem, -1);
        return cutMemo(p, n, mem);
    }

    private static int cutMemo(int[] p, int n, int[] mem) {
        if (mem[n] > 0) {
            return mem[n];
        }
        if (n == 0) {
            return 0;
        }
        int q = -1;
        for (int i = 1; i <= n; i++) {
            q = Math.max(q, p[i - 1] + cutMemo(p, n - i, mem));
        }
        mem[n] = q;
        return q;
    }

    /**
     * 自底向上的动态规划
     *
     * @param p 价格
     * @param n 长度
     * @return 利润最大值
     */
    public static int buttonUpCut(int[] p, int n) {
        int[] mem = new int[n + 1];
        for (int i = 1; i <= n; i++) {
            int q = -1;
            for (int j = 1; j <= i; j++)
                q = Math.max(q, p[j - 1] + mem[i - j]);
            mem[i] = q;
        }
        return mem[n];
    }

    public static void main(String[] args) {
        int[] p = new int[]{1, 5, 8, 9, 10, 17, 17, 20, 24, 30};
        int n = 10;
        System.out.println(cut(p, n));
        System.out.println(cutMemo(p, n));
        System.out.println(buttonUpCut(p, n));
    }
}
