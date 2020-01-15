package me.w1992wishes.algorithm.sort;

import java.util.Arrays;

/**
 * @author w1992wishes 2020/1/15 11:12
 */
public class RadixSort {

    public static void main(String[] args) {
        int[] arr = {6, 9, 1, 4, 5, 8, 7, 0, 2, 3, 1, 4};

        System.out.println("排序前:  ");
        Arrays.stream(arr).forEach(System.out::println);

        new RadixSort().radixLSDSort(arr);

        System.out.println("排序后:  ");
        Arrays.stream(arr).forEach(System.out::println);
    }

    /**
     * 基数排序
     *
     * @param array 数组
     */
    private void radixLSDSort(int[] array) {

        if (array == null || array.length < 2) {
            return;
        }

        // 指数。当对数组按各位进行排序时，exp=1；按十位进行排序时，exp=10；...
        int exp;
        // 数组a中的最大值
        int max = getMax(array);

        // 从个位开始，对数组a按"指数"进行排序
        for (exp = 1; max / exp > 0; exp *= 10) {
            countSort(array, exp);
        }

    }

    /**
     * 对数组按照"某个位数"进行排序(桶排序)
     *
     * @param array -- 数组
     * @param exp   -- 指数。对数组a按照该指数进行排序。
     *
     *              例如，对于数组a={50, 3, 542, 745, 2014, 154, 63, 616}；
     *              (01) 当exp=1表示按照"个位"对数组a进行排序
     *              (02) 当exp=10表示按照"十位"对数组a进行排序
     *              (03) 当exp=100表示按照"百位"对数组a进行排序
     */
    private void countSort(int[] array, int exp) {
        // 存储"被排序数据"的临时数组
        int[] output = new int[array.length];
        int[] buckets = new int[10];

        // 将数据出现的次数存储在buckets[]中, (array[i] / exp) % 10 为 array 中元素在 bucket 数组的下标
        for (int i = 0; i < array.length; i++) {
            buckets[(array[i] / exp) % 10]++;
        }

        // 更改buckets[i]。目的是让更改后的buckets[i]的值，是该数据在output[]中的位置。
        for (int i = 1; i < 10; i++) {
            buckets[i] += buckets[i - 1];
        }

        // 将数据存储到临时数组output[]中
        for (int i = array.length - 1; i >= 0; i--) {
            output[buckets[(array[i] / exp) % 10] - 1] = array[i];
            buckets[(array[i] / exp) % 10]--;
        }

        // 将排序好的数据赋值给 a[]
        for (int i = 0; i < array.length; i++) {
            array[i] = output[i];
        }

        output = null;
        buckets = null;
    }

    /**
     * 位数
     */
    private int getDigit(int max) {

        // String.valueOf(max).length()

        int digit = 0;
        while (max > 0) {
            max /= 10;
            digit++;
        }
        return digit;
    }

    private int getMax(int[] array) {
        int max = array[0];
        for (int i = 0; i < array.length; ++i) {
            max = Math.max(max, array[i]);
        }
        return max;
    }
}
