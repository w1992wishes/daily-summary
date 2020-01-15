package me.w1992wishes.algorithm.sort;

import java.util.Arrays;

/**
 * 归并即将两个有序的数组归并并成一个更大的有序数组。
 *
 * 人们很快根据这个思路发明了一种简单的递归排序算法：归并排序。
 *
 * 要将一个数组排序，可以先（递归地）将它分成两半分别排序，然后将结果归并起来。
 *
 * 归并排序最吸引人的性质是它能保证任意长度为N的数组排序所需时间和NlogN成正比；
 *
 * 它的主要缺点也显而易见就是它所需的额外空间和N成正比。
 *
 * @author w1992wishes 2019/12/30 15:18
 */
public class MergeSort {

    public static void main(String[] args) {
        int[] arr = {9, 8, 7, 11, 5, 4, 3, 2, 1};
        sort(arr);
        System.out.println(Arrays.toString(arr));
    }

    // 归并（Merge）排序法是将两个（或两个以上）有序表合并成一个新的有序表，即把待排序序列分为若干个子序列，每个子序列是有序的。
    // 然后再把有序子序列合并为整体有序序列。

    /**
     * 把长度为n的输入序列分成两个长度为n/2的子序列；
     * 对这两个子序列分别采用归并排序；
     * 将两个排序好的子序列合并成一个最终的排序序列。
     */
    public static void sort(int[] arr) {
        //在排序前，先建好一个长度等于原数组长度的临时数组，避免递归中频繁开辟空间
        int[] temp = new int[arr.length];
        sort(arr, 0, arr.length - 1, temp);
    }

    /**
     * 归并排序 简介:将两个（或两个以上）有序表合并成一个新的有序表
     * 即把待排序序列分为若干个子序列，每个子序列是有序的。然后再把有序子序列合并为整体有序序列 时间复杂度为O(nlogn) 稳定排序方式
     */
    private static void sort(int[] arr, int left, int right, int[] temp) {
        if (left < right) {
            int mid = (left + right) / 2;
            //左边归并排序，使得左子序列有序
            sort(arr, left, mid, temp);
            //右边归并排序，使得右子序列有序
            sort(arr, mid + 1, right, temp);
            //将两个有序子数组合并操作
            merge(arr, left, mid, right, temp);
        }
    }

    /**
     * 将数组中low到high位置的数进行排序
     *
     * @param arr 待排序数组
     * @param left     待排的开始位置
     * @param mid     待排中间位置
     * @param right    待排结束位置
     */
    private static void merge(int[] arr, int left, int mid, int right, int[] temp) {
        //左序列指针
        int i = left;
        //右序列指针
        int j = mid + 1;
        //临时数组指针
        int t = 0;
        while (i <= mid && j <= right) {
            if (arr[i] <= arr[j]) {
                temp[t++] = arr[i++];
            } else {
                temp[t++] = arr[j++];
            }
        }
        //将左边剩余元素填充进temp中
        while (i <= mid) {
            temp[t++] = arr[i++];
        }
        //将右序列剩余元素填充进temp中
        while (j <= right) {
            temp[t++] = arr[j++];
        }
        t = 0;
        //将temp中的元素全部拷贝到原数组中
        while (left <= right) {
            arr[left++] = temp[t++];
        }
    }

}
