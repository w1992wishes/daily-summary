package me.w1992wishes.algorithm.interview.dynamic;

import java.util.Arrays;

import static java.lang.Math.min;

/**
 * 题目：给你 k 种面值的硬币，面值分别为 c1, c2 ... ck，再给一个总金额 n，问你最少需要几枚硬币凑出这个金额，如果不可能凑出，则回答 -1 。
 * 比如说，k = 3，面值分别为 1，2，5，总金额 n = 11，那么最少需要 3 枚硬币，即 11 = 5 + 5 + 1 。
 *
 * @author w1992wishes 2020/1/17 14:28
 */
public class CoinChange {

    /**
     * f(0) = 0;  f(1) = 1; f(n) = 1 + min{(f(n - ci))}  1<=i<=k
     */

    /**
     * 暴力解法
     *
     * @param coins
     * @param amount
     * @return
     */
    private int coinChangeViolentSolution(int[] coins, int amount) {
        if (amount == 0) {
            return 0;
        }
        int ans = Integer.MAX_VALUE;
        for (int i = 0; i < coins.length; i++) {
            // 金额不可达
            if (amount - coins[i] < 0) {
                continue;
            }
            int subProb = coinChangeViolentSolution(coins, amount - coins[i]);
            // 子问题无解
            if (subProb == -1) {
                continue;
            }
            ans = min(ans, subProb + 1);
        }
        return ans == Integer.MAX_VALUE ? -1 : ans;
    }

    /**
     * 带备忘录的递归算法
     *
     * @param coins
     * @param amount
     * @return
     */
    int coinChangeMemo(int[] coins, int amount) {
        // 备忘录初始化为 -2
        int[] memo = new int[amount + 1];
        Arrays.fill(memo, -2);
        return helper(coins, amount, memo);
    }

    int helper(int[] coins, int amount, int[] memo) {
        if (amount == 0) {
            return 0;
        }
        if (memo[amount] != -2) {
            return memo[amount];
        }
        int ans = Integer.MAX_VALUE;
        for (int coin : coins) {
            // 金额不可达
            if (amount - coin < 0) {
                continue;
            }
            int subProb = helper(coins, amount - coin, memo);
            // 子问题无解
            if (subProb == -1) {
                continue;
            }
            ans = min(ans, subProb + 1);
        }
        // 记录本轮答案
        memo[amount] = (ans == Integer.MAX_VALUE) ? -1 : ans;
        return memo[amount];
    }

    /**
     * 动态规划
     *
     * @param coins
     * @param amount
     * @return
     */
    private int coinChangeDynamic(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1);
        dp[0] = 0;
        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (coin <= i) {
                    dp[i] = min(dp[i], dp[i - coin] + 1);
                }
            }
        }
        return dp[amount] > amount ? -1 : dp[amount];
    }

    public static void main(String[] args) {
        int[] coins = {1, 2, 5};
        int amount = 11;
        CoinChange coinChange = new CoinChange();
        System.out.println(coinChange.coinChangeViolentSolution(coins, amount));
        System.out.println(coinChange.coinChangeMemo(coins, amount));
        System.out.println(coinChange.coinChangeDynamic(coins, amount));
    }
}
