package me.w1992wishes.to.offer.case50;

/**
 * 求 1+2+…+n ，要求不能使用乘除法、for、while、if、else、switch、case等关键字及条件判断语句（A?B:C）。
 */
public class SumOneToN {

    /**
     * 不能使用乘除法，只能累加，又不能使用循环，这时想到用递归，但递需要根据条件跳出，怎么办？
     *
     * 可以使用逻辑运算 && 的短路操作
     */
    private int sum(int n){
        // 递归调用时当 n <= 0 时即可跳出
        boolean bo = n > 0 && (n += sum(n-1)) > 0;
        return n;
    }

    public static void main(String[] args) {
        SumOneToN sumOneToN = new SumOneToN();
        System.out.println(sumOneToN.sum(20));
    }

}
