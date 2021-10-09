package me.w1992wishes.to.offer.integer;

/**
 * 整数除法
 * <p>
 * 给定两个整数 a 和 b ，求它们的除法的商 a/b ，要求不得使用乘号 '*'、除号 '/' 以及求余符号 '%' 。
 */
public class Divide {

//    public int divide(int dividend, int divisor) {
//        // 对于32位整数而言，最小的负数是-2^31,将其转化为正数是2^31，导致溢出。将正数转化为负数不会导致溢出。
//        // 特殊情况，可能溢出的情况讨论，由于是整数除法，除数不为0，商的值一定小于等于被除数的绝对值，因此，int型溢出只有一种情况，(-2^31)/(-1) = 2^31。
//
//        //特殊处理 -2^31/ -1 = 2^31 溢出
//        if(dividend == 0x80000000 && divisor == -1){
//            return 0x7FFFFFFF;
//        }
//
//        int negative = 2;//用于记录正数个数
//        //由于负数转为正数 -2^31 -> 2^31 越界，所以采用正数转为负数
//        if(dividend > 0){
//            negative --;
//            dividend = -dividend;
//        }
//
//        if(divisor > 0){
//            negative --;
//            divisor = -divisor;
//        }
//
//        //计算两个负数相除
//        int result = 0;
//        while(dividend <= divisor){
//            dividend -= divisor;
//            result++;
//        }
//
//        return negative == 1 ? -result : result;
//
//    }

    /**
     * 举个例子，300/7
     * 不断右移300，直到找到第一个大于等于7的
     * 300 = 7x32 + 7x8 + 7x2 + 6
     * 第一次找到32 ans += 32;
     * 然后300->300-32x7=76
     * 继续重复上一流程
     * 76 = 7x8 + 7x2 + 6
     * 找到8，ans+=8,然后76->76-7x8=20
     * 20 = 7x2 + 6
     * 找到2，ans+=2,然后20->20-2x7=6
     * 此时6<7，由于不计算小数部分，此时返回答案ans即可，为防止溢出这里全部用的long
     * 然后以及a=0及a == Integer.MIN_VALUE && b == -1的特殊情况处理
     * 然后就是正负号的判断
     */
    public int divide(int a, int b) {

        if (a == 0) return 0;

        /* 对于32位整数而言，最小的负数是-2^31,将其转化为正数是2^31，导致溢出。将正数转化为负数不会导致溢出。
         * int型溢出只有一种情况，(-2^31)/(-1) = 2^31
         */
        if (a == Integer.MIN_VALUE && b == -1) {
            return Integer.MAX_VALUE;
        }

        /*
         * 判断是否同号，同号则 结果为正数
         */
        boolean f = (a > 0 && b > 0) || (a < 0 && b < 0);

        /*
         * 取绝对值
         */
        long x = Math.abs((long) a), y = Math.abs((long) b);

        long ans = 0;
        for (int i = 31; i >= 0; i--) {
            if ((x >> i) >= y) {
                ans += ((long) 1 << i);
                x -= (y << i);
            }
        }
        return f ? (int) ans : (int) (-ans);
    }
}
