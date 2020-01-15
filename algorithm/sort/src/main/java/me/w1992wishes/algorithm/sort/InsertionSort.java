package me.w1992wishes.algorithm.sort;

import java.util.Arrays;

/**
 * 通常人们整理桥牌的方法是一张一张的来，将每一张牌插入到其他已经有序牌中的适当位置。
 * 在计算机的实现中，为了给要插入的元素腾出空间，我们需要将其余所有元素在插入之前都向右移动一位。这种算法就叫，插入排序。
 *
 * 对一个接近有序或有序的数组进行排序会比随机顺序或是逆序的数组进行排序要快的多。
 *
 * @author w1992wishes 2019/12/30 13:55
 */
public class InsertionSort {

    // 每步将一个待排序的记录，按其顺序码大小插入到前面已经排序的字序列的合适位置（从后向前找到合适位置后），直到全部插入排序完为止。

    /**
     * 一般来说，插入排序都采用in-place在数组上实现。具体算法描述如下：
     *
     * 1.从第一个元素开始，该元素可以认为已经被排序；
     * 2.取出下一个元素，在已经排序的元素序列中从后向前扫描；
     * 3.如果该元素（已排序）大于新元素，将该元素移到下一位置；
     * 4.重复步骤3，直到找到已排序的元素小于或者等于新元素的位置；
     * 5.将新元素插入到该位置后；
     * 6.重复步骤2~5。
     */
    private void insertSort(int[] array) {
        // 从第二个元素开始遍历
        for (int i = 1; i < array.length; i++) {
            // i 之前的元素可认为已经排序
            // 将 i 对应的元素插入已经排序的数中，可认为同已经排序元素比较，直到找到小于 i 对应的元素
            int perIndex = i - 1;
            int insert = array[i];
            // 如果要插入的元素小于第preIndex个元素，就将第preIndex个元素向后移动
            while (perIndex >= 0 && insert < array[perIndex]) {
                array[perIndex + 1] = array[perIndex];
                perIndex--;
            }
            // 直到要插入的元素不小于第preIndex个元素,将insertNote插入到数组中
            array[perIndex + 1] = insert;
        }
    }

    /**
     * 二分折半插入排序
     *
     * 虽然，折半插入改善了查找插入位置的比较次数，但是移动的时间耗费并没有得到改善，所以效率上优秀的量可观，时间复杂度仍然为O(n*n)。
     *
     * @param array array
     */
    private void halfInsertSort(int[] array) {
        // 从第二个元素开始遍历
        for (int i = 1; i < array.length; i++) {
            int insert = array[i];

            // 二分法找到应该插入的位置，默认 0 到 i-1 已经排序
            int low = 0;
            int high = i - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                if (array[mid] > insert) {
                    high = mid - 1;
                } else if (array[mid] < insert) {
                    low = mid + 1;
                } else {
                    break;
                }
            }

            // low的索引位置就是即将插入的位置
            // 找到插入的位置后，将后面的数都往后移动
            for (int j = i - 1; j >= low; j--) {
                array[j + 1] = array[j];
            }
            array[low] = insert;
        }
    }

    public static void main(String[] args) {
        int[] array = {9, 8, 10, 5, 2, 7, 6};

        InsertionSort sort = new InsertionSort();
        //sort.insertSort(array);
        sort.halfInsertSort(array);
        Arrays.stream(array).forEach(System.out::println);
    }

}
