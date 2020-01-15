package me.w1992wishes.algorithm.sort;

/**
 * 冒泡排序（Bubble Sort），是一种计算机科学领域的较简单的排序算法。
 * 它重复地走访过要排序的数列，一次比较两个元素，如果他们的顺序错误就把他们交换过来。走访数列的工作是重复地进行直到没有再需要交换，也就是说该数列已经排序完成。
 * 这个算法的名字由来是因为越大的元素会经由交换慢慢“浮”到数列的顶端，故名“冒泡排序”。
 *
 * @author w1992wishes 2019/12/30 11:25
 */
public class BubbleSort {

    /**
     * 1.比较相邻的元素。如果第一个比第二个大，就交换它们两个；
     * 2.对每一对相邻元素作同样的工作，从开始第一对到结尾的最后一对，这样在最后的元素应该会是最大的数；
     * 3.针对所有的元素重复以上的步骤，除了最后一个；
     * 4.重复步骤1~3，直到排序完成。
     *
     */
    void sort(int[] numbers) {
        int temp = 0;
        int size = numbers.length;
        for (int i = 0; i < size - 1; i++) {
            for (int j = 0; j < size - 1 - i; j++) {
                // 交换两数位置
                if (numbers[j] > numbers[j + 1]) {
                    temp = numbers[j];
                    numbers[j] = numbers[j + 1];
                    numbers[j + 1] = temp;
                }
            }
        }
    }

    // 冒泡排序是最容易理解的一个排序算法。
    // 其效率并不是很高。当序列已经有序的时候。会进行一些多余的比较。
    // 根据其两两比较的特性，可以推测出，如果一趟比较中连一次元素的交换操作都没发生，那么整个序列肯定是已经有序的了。
    // 据此给出优化版冒泡排序算法。
    private void optimizeSort(int[] numbers) {
        // 初始化为无序状态
        boolean sorted = false;
        int temp = 0;
        int size = numbers.length;
        for (int i = 0; i < size - 1 && !sorted; i++) {
            // 假设已经有序，若没有发生交换，则sorted维持为true，下次循环将直接退出。
            sorted = true;
            for (int j = 0; j < size - 1 - i; j++) {
                // 交换两数位置
                if (numbers[j] > numbers[j + 1]) {
                    temp = numbers[j];
                    numbers[j] = numbers[j + 1];
                    numbers[j + 1] = temp;
                    // 数组无序
                    sorted = false;
                }
            }
        }
    }
}
