package me.w1992wishes.algorithm.sort;

import java.util.Arrays;

/**
 * @author w1992wishes 2020/1/14 19:10
 */
public class ShellSort {

    private void shellSort(int[] array) {
        //step:步长，步长缩小到0的时候就退出循环
        for (int step = array.length / 2; step > 0; step = step / 2) {
            // 对一个步长区间进行比较 [step,arr.length)，直接插入排序
            for (int i = step; i < array.length; i++) {
                int insert = array[i];
                int j;
                // 每一个段内进行插入排序
                // 如想从小到大排只需修改这里
                for (j = i - step; j >= 0 && array[j] > insert; j -= step) {
                    // 把元素往后挪
                    array[j + step] = array[j];
                }
                //此时step为一个负数，[j + step]为左区间上的初始交换值
                array[j + step] = insert;
            }

        }
    }

    public static void main(String[] args) {
        int[] arr = {6, 9, 1, 4, 5, 8, 7, 0, 2, 3};

        System.out.println("排序前:  ");
        Arrays.stream(arr).forEach(System.out::println);

        new ShellSort().shellSort(arr);

        System.out.println("排序后:  ");
        Arrays.stream(arr).forEach(System.out::println);
    }

}
