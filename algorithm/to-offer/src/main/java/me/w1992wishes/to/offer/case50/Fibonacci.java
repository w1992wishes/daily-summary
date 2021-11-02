package me.w1992wishes.to.offer.case50;

public class Fibonacci {

    // F(0)=0，F(1)=1
    // F(n)=F(n−1)+F(n−2),(1<n≤30)
    // 给你 n ，请计算 F(n)
    public long fibonacci(int n) {
        if (n < 2) {
            return n;
        }
        if (n == 2) {
            return 1;
        }
        long f1 = 1, f2 = 1, c = 0;
        //此处要减去两项
        for (int i = 3; i <= n; i++) {
            c = f1 + f2;
            f1 = f2;
            f2 = c;
        }
        return c;
    }

    public static void main(String[] args) {
        Fibonacci fibonacci = new Fibonacci();
        for (int i = 0; i < 20; i++) {
            System.out.println("n->" + i + "->" + fibonacci.fibonacci(i));
        }
    }
}
