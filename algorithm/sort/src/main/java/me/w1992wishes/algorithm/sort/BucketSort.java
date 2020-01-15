package me.w1992wishes.algorithm.sort;

import java.util.Arrays;

/**
 * @author w1992wishes 2020/1/15 9:49
 */
public class BucketSort {

    public static void main(String[] args) {
        int[] arr = {6, 9, 1, 4, 5, 8, 7, 0, 2, 3};

        System.out.println("排序前:  ");
        Arrays.stream(arr).forEach(System.out::println);

        new BucketSort().bucketSort(arr);

        System.out.println("排序后:  ");
        Arrays.stream(arr).forEach(System.out::println);
    }


    private void bucketSort(int[] array) {

        if (array == null || array.length < 2) {
            return;
        }

        int min = array[0], max = array[0];
        // 先找出最大最小值
        for (int i = 1; i < array.length; i++) {
            min = Math.min(min, array[i]);
            max = Math.max(max, array[i]);
        }

        int len = array.length;
        // 根据原始序列的长度，设置桶的数量。这里假设每个桶放平均放4个元素
        int bucketCount = len / 4;
        int[][] buckets = new int[bucketCount][];

        // 每个桶的数值范围
        int range = (max - min + 1) / bucketCount;

        // 遍历原始序列
        for (int i = 0; i < len; i++) {
            int val = array[i];
            // 计算当前值属于哪个桶
            int bucketIndex = (int) Math.floor((val - min) / range);
            // 向桶中添加元素
            buckets[bucketIndex] = appendItem(buckets[bucketIndex], val);
        }

        // 最后合并所有的桶
        int k = 0;
        for (int[] b : buckets) {
            if (b != null) {
                for (int i = 0; i < b.length; i++) {
                    array[k++] = b[i];
                }
            }
        }

    }

    private static int[] appendItem(int[] bucketArr, int val) {
        if (bucketArr == null || bucketArr.length == 0) {
            return new int[]{val};
        }
        // 拷贝一下原来桶的序列，并增加一位
        int[] arr = Arrays.copyOf(bucketArr, bucketArr.length + 1);
        // 这里使用插入排序，将新的值val插入到序列中
        int i;
        for (i = bucketArr.length - 1; i >= 0; i--) {
            // 从新序列arr的倒数第二位开始向前遍历（倒数第一位是新增加的空位，还没有值）
            // 如果当前序列值大于val，那么向后移位
            if (arr[i] > val) {
                arr[i + 1] = arr[i];
            } else {
                break;
            }
        }
        arr[i + 1] = val;
        return arr;
    }

}
