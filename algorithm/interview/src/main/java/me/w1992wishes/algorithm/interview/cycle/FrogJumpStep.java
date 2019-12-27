package me.w1992wishes.algorithm.interview.cycle;

/**
 * 一只青蛙一次可以跳上1级台阶，也可以跳上2级。求该青蛙跳上一个n级的台阶总共有多少种跳法。
 *
 * @author w1992wishes 2019/12/27 17:49
 */
public class FrogJumpStep {

    /**
     * 该问题实质是斐波那契数列求和，递推公式为 f(n)=f(n-1)+f(n-2);
     *
     * 可以考虑，小青蛙每一步跳跃只有两种选择：一是再跳一级阶梯到达第 i 级阶梯，此时小青蛙处于第 i-1 级阶梯；或者再跳两级阶梯到达第 i 级阶梯，此时小青蛙处于第 i-2 级阶梯。
     *
     * 于是，i 级阶梯的跳法总和依赖于前 i-1 级阶梯的跳法总数f(i-1)和前 i-2 级阶梯的跳法总数f(i-2)。因为只有两种可能性，所以，f(i)=f(i-1)+f(i-2);
     */
    private int frogJump(int n) {

        int[] jump = {1, 2, 3};

        int stepOne = 1;
        int stepTwo = 2;
        int stepN = 0;

        if (n < 3) {
            return jump[n];
        }

        for (int i = 3; i < n; i++) {
            stepN = stepOne + stepTwo;
            stepOne = stepTwo;
            stepTwo = stepN;
        }

        return stepN;
    }

}
