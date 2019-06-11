package me.w1992wishes.easy.collection.array;

/**
 * Say you have an array for which the ith element is the price of a given stock on day i.
 * <p>
 * Design an algorithm to find the maximum profit. You may complete as many transactions as you like (i.e., buy one and sell one share of the stock multiple times).
 * <p>
 * Example 1:
 * <p>
 * Input: [7,1,5,3,6,4]
 * Output: 7
 * Explanation: Buy on day 2 (price = 1) and sell on day 3 (price = 5), profit = 5-1 = 4.
 * Then buy on day 4 (price = 3) and sell on day 5 (price = 6), profit = 6-3 = 3.
 *
 * @author w1992wishes 2019/6/4 14:04
 */
public class MaxProfitArray {

    /**
     * 简单的方法就是一旦第二天的价格比前一天的高，就在前一天买入第二天卖出，但是这个会违反“不能同一天买卖的规则”
     */
    public int maxProfit1(int[] prices) {
        int total = 0;
        for (int i = 0; i < prices.length - 1; ++i) {
            if (prices[i] < prices[i + 1]) {
                total += prices[i + 1] - prices[i];
            }
        }
        return total;
    }

    /**
     * 低谷买，高峰卖就可以获得最大利润
     */
    public int maxProfit2(int[] prices) {
        int len = prices.length;
        if (len <= 1) {
            return 0;
        }

        int i = 0;
        int total = 0;
        while (i < len - 1) {
            int buy, sell;
            //寻找递减区间的最后一个值（局部最小点）
            while (i + 1 < len && prices[i + 1] < prices[i]) {
                i++;
            }
            //局部最小点作为买入点
            buy = i;

            //找下一个点(卖出点至少为下一个点）
            i++;
            //不满足。。继续往下找递增区间的最后一个值（局部最高点）
            while (i < len && prices[i] >= prices[i - 1]) {
                i++;
            }
            //设置卖出点
            sell = i - 1;
            //计算总和
            total += prices[sell] - prices[buy];
        }
        return total;
    }

}
