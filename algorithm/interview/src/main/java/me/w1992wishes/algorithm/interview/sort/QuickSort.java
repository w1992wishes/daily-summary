package me.w1992wishes.algorithm.interview.sort;

/**
 * 快速排序的基本思想：通过一趟排序将待排记录分隔成独立的两部分，其中一部分记录的关键字均比另一部分的关键字小，则可分别对这两部分记录继续进行排序，以达到整个序列有序。
 *
 * @author w1992wishes 2019/12/30 15:31
 */
public class QuickSort extends AbstractSort {

    /**
     * 快速排序使用分治法来把一个串（list）分为两个子串（sub-lists）。具体算法描述如下：
     *
     * 从数列中挑出一个元素，称为 “基准”（pivot）；
     * 重新排序数列，所有元素比基准值小的摆放在基准前面，所有元素比基准值大的摆在基准的后面（相同的数可以到任一边）。在这个分区退出之后，该基准就处于数列的中间位置。这个称为分区（partition）操作；
     * 递归地（recursive）把小于基准值元素的子数列和大于基准值元素的子数列排序。
     */
    @Override
    void sort(int[] numbers) {
        quickSort(numbers);
    }

    /**
     * 查找出中轴（默认是最低位low）的在numbers数组排序后所在位置
     *
     * @param numbers 带查找数组
     * @param low     开始位置
     * @param high    结束位置
     * @return 中轴所在位置
     */
    public static int getMiddle(int[] numbers, int low, int high) {
        // 数组的第一个作为中轴
        int temp = numbers[low];
        while (low < high) {
            while (low < high && numbers[high] > temp) {
                high--;
            }
            // 比中轴小的记录移到低端
            numbers[low] = numbers[high];

            while (low < high && numbers[low] < temp) {
                low++;
            }
            // 比中轴大的记录移到高端
            numbers[high] = numbers[low];
        }
        numbers[low] = temp; // 中轴记录到尾
        return low; // 返回中轴的位置
    }

    /**
     * @param numbers 带排序数组
     * @param low     开始位置
     * @param high    结束位置
     */
    public static void quick(int[] numbers, int low, int high) {
        if (low < high) {
            int middle = getMiddle(numbers, low, high); // 将numbers数组进行一分为二
            quick(numbers, low, middle - 1); // 对低字段表进行递归排序
            quick(numbers, middle + 1, high); // 对高字段表进行递归排序
        }

    }

    /**
     * 快速排序
     * 快速排序提供方法调用
     *
     * @param numbers 带排序数组
     */
    public static void quickSort(int[] numbers) {
        // 查看数组是否为空
        if (numbers.length > 0) {
            quick(numbers, 0, numbers.length - 1);
        }
    }
}
