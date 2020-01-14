package me.w1992wishes.algorithm.sort;

import java.util.Arrays;

/**
 * 计数排序不是基于比较的排序算法，其核心在于将输入的数据值转化为键存储在额外开辟的数组空间中。
 *
 * 作为一种线性时间复杂度的排序，计数排序要求输入的数据必须是有确定范围的整数。
 *
 * 计数排序需要占用大量空间，它仅适用于数据比较集中的情况。比如 [0~100]，[10000~19999] 这样的数据。
 *
 * @author w1992wishes 2020/1/14 20:00
 */
public class CountingSort {

    public static void main(String[] args) {
        int[] arr = {6, 9, 1, 4, 5, 8, 7, 0, 2, 3};

        System.out.println("排序前:  ");
        Arrays.stream(arr).forEach(System.out::println);

        new CountingSort().countingSort(arr);

        System.out.println("排序后:  ");
        Arrays.stream(arr).forEach(System.out::println);
    }

    /**
     * 找出待排序的数组中最大和最小的元素；
     * 统计数组中每个值为i的元素出现的次数，存入数组C的第i项；
     * 对所有的计数累加（从C中的第一个元素开始，每一项和前一项相加）；
     * 反向填充目标数组：将每个元素i放在新数组的第C(i)项，每放一个元素就将C(i)减去1。
     *
     * @param array array
     */
    private void countingSort(int[] array) {

        if (array == null || array.length < 2) {
            return;
        }

        // 找出最大值与最小值
        int min = array[0], max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (min > array[i]) {
                min = array[i];
            } else if (max < array[i]) {
                max = array[i];
            }
        }

        // 初始化计数数组
        // 找出每个数字出现的次数
        int[] counter = new int[max - min + 1];
        for (int i = 0; i < array.length; i++) {
            counter[array[i] - min]++;
        }

        // 排序
        int index = 0;
        for (int i = 0; i < counter.length; i++) {
            while (counter[i]-- > 0) {
                array[index++] = i + min;
            }
        }
    }

}
