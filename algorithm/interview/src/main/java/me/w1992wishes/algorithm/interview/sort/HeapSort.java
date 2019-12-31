package me.w1992wishes.algorithm.interview.sort;

import java.util.Arrays;

/**
 * 堆排序(Heapsort)是指利用堆积树（堆）这种数据结构所设计的一种排序算法，它是是对简单选择排序的改进。
 *
 * 可以利用数组的特点快速定位指定索引的元素。堆分为大根堆和小根堆，是完全二叉树。
 *
 * 大根堆的要求是每个节点的值都不大于其父节点的值，即A[PARENT[i]] >= A[i]。
 *
 * 在数组的非降序排序中，需要使用的就是大根堆，因为根据大根堆的要求可知，最大的值一定在堆顶。
 *
 * @author w1992wishes 2019/12/30 15:44
 */
public class HeapSort extends AbstractSort {

    /**
     * 堆节点的访问
     *
     * 通常堆是通过一维数组来实现的。在数组起始位置为0的情形中：
     *
     * 父节点i的左子节点在位置2i+1;
     * 父节点i的右子节点在位置 2i+2;
     * 子节点i的父节点在位置floor((i-1/2)
     *
     *
     * 堆的操作
     * 在堆的数据结构中，堆中的最大值总是位于根节点(在优先队列中使用堆的话堆中的最小值位于根节点)。堆中定义以下几种操作：
     *
     * 最大堆调整（Max_Heapify）：将堆的末端子节点作调整，使得子节点永远小于父节点
     * 创建最大堆（Build_Max_Heap）：将堆所有数据重新排序
     * 堆排序（HeapSort）：移除位在第一个数据的根节点，并做最大堆调整的递归运算
     */

    /**
     * 将待排序的序列构造成一个大顶堆。
     *
     * 此时，整个序列的最大值就是堆顶的根节点。
     *
     * 将它移走(其实就是将其与堆数组的末尾元素交换，此时末尾元素就是最大值)，然后将剩余的n-1个序列重新构造成一个堆，这样就会得到n个元素中的次最大值。
     *
     * 如此反复执行，就能得到一个有序序列了。
     */
    @Override
    void sort(int[] numbers) {
        /*
         *  第一步：将数组堆化
         *  beginIndex = 第一个非叶子节点。
         *  从第一个非叶子节点开始即可。无需从最后一个叶子节点开始。
         *  叶子节点可以看作已符合堆要求的节点，根节点就是它自己且自己以下值为最大。
         */
        int len = numbers.length - 1;
        int beginIndex = (len - 1) >> 1;
        for (int i = beginIndex; i >= 0; i--) {
            maxHeapify(i, len, numbers);
        }

        /*
         * 第二步：对堆化数据排序
         * 每次都是移出最顶层的根节点A[0]，与最尾部节点位置调换，同时遍历长度 - 1。
         * 然后从新整理被换到根节点的末尾元素，使其符合堆的特性。
         * 直至未排序的堆长度为 0。
         */
        for (int i = len; i > 0; i--) {
            swap(0, i, numbers);
            maxHeapify(0, i - 1, numbers);
        }
    }

    private void swap(int i, int j, int[] numbers) {
        int temp = numbers[i];
        numbers[i] = numbers[j];
        numbers[j] = temp;
    }

    /**
     * 调整索引为 index 处的数据，使其符合堆的特性。
     *
     * @param index 需要堆化处理的数据的索引
     * @param len   未排序的堆（数组）的长度
     */
    private void maxHeapify(int index, int len, int[] numbers) {
        int li = (index << 1) + 1; // 左子节点索引
        int ri = li + 1;           // 右子节点索引
        int cMax = li;             // 子节点值最大索引，默认左子节点。

        if (li > len) {
            return;       // 左子节点索引超出计算范围，直接返回。
        }
        // 先判断左右子节点，哪个较大。
        if (ri <= len && numbers[ri] > numbers[li]) {
            cMax = ri;
        }
        if (numbers[cMax] > numbers[index]) {
            swap(cMax, index, numbers);      // 如果父节点被子节点调换，
            maxHeapify(cMax, len, numbers);  // 则需要继续判断换下后的父节点是否符合堆的特性。
        }
    }

    /**
     * 测试用例
     *
     * 输出：
     * [0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9]
     */
    public static void main(String[] args) {
        int[] arr = new int[]{3, 5, 3, 0, 8, 6, 1, 5, 8, 6, 2, 4, 9, 4, 7, 0, 1, 8, 9, 7, 3, 1, 2, 5, 9, 7, 4, 0, 2, 6};
        new HeapSort().sort(arr);
        System.out.println(Arrays.toString(arr));
    }

}
