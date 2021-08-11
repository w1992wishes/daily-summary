package me.w1992wishes.to.offer.integer;

/**
 * 给定两个整数 a 和 b ，求它们的除法的商 a/b ，要求不得使用乘号 '*'、除号 '/' 以及求余符号 '%' 。
 */
public class Divide {

    public int divide(int dividend, int divisor) {
        // 对于32位整数而言，最小的负数是-2^31,将其转化为正数是2^31，导致溢出。将正数转化为负数不会导致溢出。
        // 特殊情况，可能溢出的情况讨论，由于是整数除法，除数不为0，商的值一定小于等于被除数的绝对值，因此，int型溢出只有一种情况，(-2^31)/(-1) = 2^31。

        //特殊处理 -2^31/ -1 = 2^31 溢出
        if(dividend == 0x80000000 && divisor == -1){
            return 0x7FFFFFFF;
        }

        int negative = 2;//用于记录正数个数
        //由于负数转为正数 -2^31 -> 2^31 越界，所以采用正数转为负数
        if(dividend > 0){
            negative --;
            dividend = -dividend;
        }

        if(divisor > 0){
            negative --;
            divisor = -divisor;
        }

        //计算两个负数相除
        int result = 0;
        while(dividend <= divisor){
            dividend -= divisor;
            result++;
        }

        return negative == 1 ? -result : result;

    }
}
