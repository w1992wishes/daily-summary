package me.w1992wishes.algorithm.sort;

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
public class HeapSort {

    private static int[] waitSortArray = new int[]{1, 0, 10, 20, 3, 5, 6, 4, 9, 8, 12, 17, 34, 11};

    public static void main(String[] args) {
        HeapSort heapSort = new HeapSort();
        heapSort.buildMaxHeapify(waitSortArray);
        heapSort.heapSort(waitSortArray);
        heapSort.print(waitSortArray);
    }

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
    private void heapSort(int[] array) {
        // 末尾与头交换，交换后调整最大堆
        for (int i = array.length - 1; i > 0; i--) {
//            print(array);
            // array[0] 为最大值，同末尾交换
            int temp = array[0];
            array[0] = array[i];
            array[i] = temp;
            // 交换后重新调整堆
            maxHeapify(array, i, 0);
        }
    }

    private void print(int[] data) {
        int pre = -2;
        for (int i = 0; i < data.length; i++) {
            if (pre < (int) getLog(i + 1)) {
                pre = (int) getLog(i + 1);
                System.out.println();
            }
            System.out.print(data[i] + "|");
        }
    }

    /**
     * 以2为底的对数
     *
     * @param param param
     * @return 以2为底的对数
     */
    private static double getLog(double param) {
        return Math.log(param) / Math.log(2);
    }

    /**
     * 父节点位置
     *
     * @param current 节点下标
     * @return 父节点位置
     */
    private static int parentIndex(int current) {
        // (current - 1) / 2
        return (current - 1) >> 1;
    }

    /**
     * 返回父节点下的左子节点下标
     *
     * @param i 父节点下标
     * @return 左子节点下标
     */
    private int left(int i) {
        return i * 2 + 1;
    }

    /**
     * 返回父节点下的右子节点下标
     *
     * @param i 父节点下标
     * @return 右子节点下标
     */
    private int right(int i) {
        return i * 2 + 2;
    }

    /**
     * 循环选择非叶子节点，由下往上调整最大堆
     *
     * @param array 数组
     */
    private void buildMaxHeapify(int[] array) {
        int lastIndex = array.length - 1;
        // beginIndex = 第一个非叶子节点。
        int beginIndex = parentIndex(lastIndex);
        for (int i = beginIndex; i >= 0; i--) {
            maxHeapify(array, array.length, i);
        }
    }

    /**
     * 调整最大堆
     *
     * @param array    数组
     * @param heapSize 需要创建最大堆的大小，一般在sort的时候用到，因为最多值放在末尾，末尾就不再归入最大堆了
     * @param i        当前需要创建最大堆的位置
     */
    private void maxHeapify(int[] array, int heapSize, int i) {
        // 先根据父节点 index 找到子节点 index
        int leftIndex = left(i);
        int rightIndex = right(i);
        int largestIndex = i;

        // 将父节点同两个子节点比较，找出最大的那一个
        if (leftIndex < heapSize && array[leftIndex] > array[i]) {
            largestIndex = leftIndex;
        }

        if (rightIndex < heapSize && array[rightIndex] > array[largestIndex]) {
            largestIndex = rightIndex;
        }

        // 如果最大不为父节点，则交换，如果交换了，其子节点可能就不是最大堆了，需要重新调整最大堆
        if (largestIndex != i) {
            swap(array, i, largestIndex);
            maxHeapify(array, heapSize, largestIndex);
        }
    }

    /**
     * 交换
     *
     * @param array 数组
     * @param i     i
     * @param j     j
     */
    private void swap(int[] array, int i, int j) {
        int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

}
