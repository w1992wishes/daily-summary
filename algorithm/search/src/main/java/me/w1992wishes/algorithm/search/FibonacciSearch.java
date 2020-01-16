package me.w1992wishes.algorithm.search;

import java.util.Arrays;

/**
 * 斐波那契查找与折半查找很相似，他是根据斐波那契序列的特点对有序表进行分割的。他要求开始表中记录的个数为某个斐波那契数小1，n=F(k)-1;
 *
 * 斐波那契查找就是在二分查找的基础上根据斐波那契数列进行分割的。在斐波那契数列找一个等于略大于查找表中元素个数的数F[n]，
 * 将原查找表扩展为长度为F[n](如果要补充元素，则补充重复最后一个元素，直到满足F[n]个元素)，完成后进行斐波那契分割，
 * 即F[n]个元素分割为前半部分F[n-1]个元素，后半部分F[n-2]个元素，找出要查找的元素在那一部分并递归，直到找到。
 *
 * @author w1992wishes 2020/1/16 10:55
 */
public class FibonacciSearch {

    // 斐波那契数组的长度
    private static final int MAXSIZE = 20;

    public static void main(String[] args) {
        int[] arr = new int[]{1, 2, 3, 4, 5, 5, 6, 6, 7};
        FibonacciSearch search = new FibonacciSearch();
        System.out.println(search.fibonacciSearch(arr, 3));
        System.out.println(search.fibonacciSearch(arr, 5));
    }

    private int fibonacciSearch(int[] arr, int key) {

        int low = 0;
        int high = arr.length - 1;

        // 获取斐波那契数列
        int[] f = fibonacci();
        // 斐波那契分割数值下标
        int k = 0;
        // 获取斐波那契分割数值下标
        while (arr.length > f[k] - 1) {
            k++;
        }

        // 创建临时数组
        // 因为f[k]这个值可能大于数组 arr 的长度，因此需要使用Arrays类，构造一个新的数组并指向a
        // 不足的部分会使用0填充
        int[] temp = Arrays.copyOf(arr, f[k]);

        // 序列补充至f[k]个元素
        // 补充的元素值为最后一个元素的值
        for (int i = arr.length; i < f[k] - 1; i++) {
            temp[i] = temp[high];
        }

        while (low <= high) {
            // 前半部分有f[k-1]个元素，由于下标从0开始
            // 则-1 获取 黄金分割位置元素的下标
            int mid = low + f[k - 1] - 1;
            if (key < temp[mid]) {
                // 查找前半部分
                high = mid - 1;
                // 全部元素= 前面的元素 + 后面的元素
                // f[k] = f[k-1] + f[k-2]
                // 因为前半部分有 f[k-1] 个元素，所以 k = k-1
                k = k - 1;
            } else if (key > arr[mid]) {
                // 查找后半部分
                low = mid + 1;
                // 全部元素 = 前半部分 + 后半部分
                // f[k] = f[k-1] + f[k-2]
                // 因为后半部分有f[k-2]个元素，所以 k = k-2
                k = k - 2;
            } else {
                // 如果为真则找到相应的位置
                if (mid <= high) {
                    return mid;
                } else {
                    // 出现这种情况是查找到补充的元素
                    // 而补充的元素与high位置的元素一样
                    return high;
                }
            }
        }

        return -1;

    }

    /**
     * 斐波那契数列
     */
    private int[] fibonacci() {
        int[] f = new int[MAXSIZE];
        int i = 0;
        f[0] = 1;
        f[1] = 1;
        for (i = 2; i < MAXSIZE; i++) {
            f[i] = f[i - 1] + f[i - 2];
        }
        return f;
    }


}
