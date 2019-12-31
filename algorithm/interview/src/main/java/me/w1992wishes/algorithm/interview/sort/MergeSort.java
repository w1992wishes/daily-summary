package me.w1992wishes.algorithm.interview.sort;

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
public class MergeSort extends AbstractSort {

    // 归并（Merge）排序法是将两个（或两个以上）有序表合并成一个新的有序表，即把待排序序列分为若干个子序列，每个子序列是有序的。
    // 然后再把有序子序列合并为整体有序序列。

    /**
     * 把长度为n的输入序列分成两个长度为n/2的子序列；
     * 对这两个子序列分别采用归并排序；
     * 将两个排序好的子序列合并成一个最终的排序序列。
     */
    @Override
    void sort(int[] numbers) {
        mergeSort(numbers, 0, numbers.length - 1);
    }

    /**
     * 归并排序 简介:将两个（或两个以上）有序表合并成一个新的有序表
     * 即把待排序序列分为若干个子序列，每个子序列是有序的。然后再把有序子序列合并为整体有序序列 时间复杂度为O(nlogn) 稳定排序方式
     */
    private void mergeSort(int[] numbers, int low, int high) {
        int mid = (low + high) / 2;
        if (low < high) {
            // 左边
            mergeSort(numbers, low, mid);
            // 右边
            mergeSort(numbers, mid + 1, high);
            // 左右归并
            merge(numbers, low, mid, high);
        }
    }

    /**
     * 将数组中low到high位置的数进行排序
     *
     * @param numbers 待排序数组
     * @param low     待排的开始位置
     * @param mid     待排中间位置
     * @param high    待排结束位置
     */
    public void merge(int[] numbers, int low, int mid, int high) {
        int[] temp = new int[high - low + 1];
        int i = low;// 左指针
        int j = mid + 1;// 右指针
        int k = 0;// 临时数组指针

        // 把较小的数先移到新数组中
        while (i <= mid && j <= high) {
            if (numbers[i] < numbers[j]) {
                temp[k++] = numbers[i++];
            } else {
                temp[k++] = numbers[j++];
            }
        }

        // 把左边剩余的数移入数组
        while (i <= mid) {
            temp[k++] = numbers[i++];
        }

        // 把右边边剩余的数移入数组
        while (j <= high) {
            temp[k++] = numbers[j++];
        }

/*        // 把新数组中的数覆盖numbers数组
        for (int k2 = 0; k2 < temp.length; k2++) {
            numbers[k2 + low] = temp[k2];
        }*/

        k = 0;
        //将temp中的元素全部拷贝到原数组中
        while (low <= high) {
            numbers[low++] = temp[k++];
        }
    }
}
