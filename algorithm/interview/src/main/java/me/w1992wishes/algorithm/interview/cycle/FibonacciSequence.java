package me.w1992wishes.algorithm.interview.cycle;

/**
 *
 * Fibonacci数列中的每个数都是其两个直接前项的和。 0,1,1,2,3,5,8,13,21,......
 *
 * F(0)=0，F(1)=1, F(n)=F(n-1)+F(n-2)（n>=3，n∈N*）
 *
 * @author w1992wishes 2019/12/27 17:26
 */
public class FibonacciSequence {

    /**
     * 循环求法。我们可以从第1项开始，一直求到第n项，即可，一个循环可以做到，时间复杂度为O(n).
     */
    private int fibonacci(int n) {

        int[] result = {0, 1};

        int finNMinusOne = 1;
        int finNMinusTwo = 0;
        int finN = 0;

        if (n < 2) {
            return result[n];
        }

        for (int i = 2; i < n; i++) {
            finN = finNMinusOne + finNMinusTwo;

            finNMinusTwo = finNMinusOne;
            finNMinusOne = finN;
        }

        return finN;
    }

}
