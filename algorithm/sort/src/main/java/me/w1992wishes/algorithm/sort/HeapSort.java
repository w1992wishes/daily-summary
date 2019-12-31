package me.w1992wishes.algorithm.sort;

/**
 * @author w1992wishes 2019/12/31 15:40
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
     * 排序，最大值放在末尾，data虽然是最大堆，在排序后就成了递增的
     *
     * @param array 数组
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
