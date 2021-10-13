package me.w1992wishes.to.offer.case50;

/**
 * 递归乘法。 写一个递归函数，不使用 * 运算符， 实现两个正整数的相乘。可以使用加号、减号、位移，但要吝啬一些。
 */
public class Multiply {

    public int multiply(int a, int b) {
        // 不能使用 * 法，考虑用左移右移操作
        // 两个数相乘，可以转换为 a/2 ， b*2
        // 比如： a=9, b=10, sum=0 : 当 a 是奇数时， sum = sum +b = 10, a=a/2=4, b=b*2=20;
        // 当 a=4是偶数时，a=a/2=2, b=b*2=40
        // 当 a=2是偶数时，a=a/2=1, b=b*2=80
        // 当 a=1是奇数时，sum = sum+b=10+80=90, a=a/2=0
        int sum = 0;
        while (a > 0) {
            if (isOdd(a)) {
                sum += b;
            }
            a = a >> 1;
            b = b << 1;
        }
        return sum;
    }

    private boolean isOdd(int n) {
        return (n & 1) != 0;
    }

    public static void main(String[] args) {
        Multiply multiply = new Multiply();
        System.out.println(multiply.multiply(99, 80));
        System.out.println(99 * 80);
    }
}
