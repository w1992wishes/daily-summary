package me.w1992wishes.algorithm.test;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author w1992wishes 2020/1/9 17:10
 */
public class SortTest {

    private int[] array = {9, 8, 10, 5, 2, 7, 6};

    @Test
    public void quickSortTest() {
        quickSort(array, 0, array.length - 1);
        Assert.assertArrayEquals(array, new int[]{2, 5, 6, 7, 8, 9, 10});
    }

    private void quickSort(int[] array, int low, int high) {
        // 跳出条件
        if (low >= high) {
            return;
        }

        int l = low, r = high;
        // 取第一个为参考值
        int t_d = array[l];

        while (l < r) {
            // 此时 l 位置留了一个坑，所以从 r 位置向前比较
            while (l < r && t_d <= array[r]) {
                r--;
            }
            if (l < r) {
                array[l++] = array[r];
            }

            // 此时 r 位置留了一个坑，所以从 l 位置向后比较
            while (l < r && array[l] < t_d) {
                l++;
            }
            if (l < r) {
                array[r--] = array[l];
            }
        }

        // 最后 l 位置留了一个坑，填入参考值
        array[l] = t_d;
        quickSort(array, low, l - 1);
        quickSort(array, l + 1, high);
    }

    @Test
    public void mergeSortTest() {
        // 在排序前，先建好一个长度等于原数组长度的临时数组，避免递归中频繁开辟空间
        int tmp[] = new int[array.length];
        mergeSort(array, 0, array.length - 1, tmp);
        Assert.assertArrayEquals(array, new int[]{2, 5, 6, 7, 8, 9, 10});
    }

    private void mergeSort(int[] array, int low, int high, int[] tmp) {
        if (low >= high) {
            return;
        }
        int mid = (low + high) / 2;
        // 左部分
        mergeSort(array, low, mid, tmp);
        // 右部分
        mergeSort(array, mid + 1, high, tmp);
        // 左右归并
        merge(array, low, mid, high, tmp);
    }


    /**
     * 将数组中low到high位置的数进行排序
     *
     * @param array 待排序数组
     * @param low   待排的开始位置
     * @param mid   待排中间位置
     * @param high  待排结束位置
     */
    private void merge(int[] array, int low, int mid, int high, int[] tmp) {
        // 右序列指针
        int right = mid + 1;
        // 左序列指针
        int left = low;

        // 临时数组指针
        int i = 0;

        while (left <= mid && right <= high) {
            // tmp 填充一个后移动一位
            if (array[left] <= array[right]) {
                tmp[i++] = array[left++];
            } else {
                tmp[i++] = array[right++];
            }
        }

        // 将左边剩余元素填充进temp中
        while (left <= mid) {
            tmp[i++] = array[left++];
        }

        // 将右序列剩余元素填充进temp中
        while (right <= high) {
            tmp[i++] = array[right++];
        }

        // 将temp中的元素全部拷贝到原数组中
        i = 0;
        while (low <= high) {
            array[low++] = tmp[i++];
        }

    }

    @Test
    public void heapSortTest() {
        // 先构建最大堆
        buildMaxHeapify(array);
        // 排序
        heapSort(array);
        Assert.assertArrayEquals(array, new int[]{2, 5, 6, 7, 8, 9, 10});
    }

    /**
     * 排序，最大值放在末尾，data虽然是最大堆，在排序后就成了递增的
     *
     * @param array
     */
    private void heapSort(int[] array) {
        // 末尾与头交换，交换后调整最大堆
        for (int i = array.length - 1; i > 0; i--) {
            swap(array, i, 0);
            maxHeapify(array, i, 0);
        }
    }

    private void buildMaxHeapify(int[] array) {
        // 没有子节点的才需要创建最大堆，从最后一个的父节点开始
        int startIndex = getParentIndex(array.length - 1);
        // 从尾端开始创建最大堆，每次都是正确的堆
        for (int i = startIndex; i >= 0; i--) {
            maxHeapify(array, array.length, i);
        }
    }

    /**
     * @param array
     * @param heapSize 需要创建最大堆的大小，一般在sort的时候用到，因为最大值放在末尾，末尾就不再归入最大堆了
     * @param index    当前需要创建最大堆的位置
     */
    private void maxHeapify(int[] array, int heapSize, int index) {
        int l = leftIndex(index);
        int r = rightIndex(index);

        int largest = index;
        if (l < heapSize && array[largest] < array[l]) {
            largest = l;
        }
        if (r < heapSize && array[largest] < array[r]) {
            largest = r;
        }

        // 得到最大值后可能需要交换，如果交换了，其子节点可能就不是最大堆了，需要重新调整
        if (largest != index) {
            swap(array, index, largest);
            maxHeapify(array, heapSize, largest);
        }
    }

    private void swap(int[] array, int index, int largest) {
        int temp = array[index];
        array[index] = array[largest];
        array[largest] = temp;
    }

    private int leftIndex(int parentIndex) {
        return (parentIndex << 1) + 1;
    }

    private int rightIndex(int parentIndex) {
        return (parentIndex << 1) + 2;
    }

    private int getParentIndex(int current) {
        return (current - 1) >> 1;
    }

    @Test
    public void bubbleSortTest() {
        bubbleSort(array);
        Assert.assertArrayEquals(array, new int[]{2, 5, 6, 7, 8, 9, 10});
    }

    private void bubbleSort(int[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            for (int j = 0; j < array.length - 1 - i; j++) {
                if (array[j] > array[j + 1]) {
                    int tmp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = tmp;
                }
            }
        }
    }

    @Test
    public void optimizeBubbleSortTest() {
        optimizeBubbleSort(array);
        Assert.assertArrayEquals(array, new int[]{2, 5, 6, 7, 8, 9, 10});
    }

    private void optimizeBubbleSort(int[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            boolean flag = true;
            for (int j = 0; j < array.length - 1 - i; j++) {
                if (array[j] > array[j + 1]) {
                    flag = false;
                    int tmp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = tmp;
                }
            }
            if (flag) {
                break;
            }
        }
    }

    @Test
    public void insertSortTest() {
        insertSort(array);
        Assert.assertArrayEquals(array, new int[]{2, 5, 6, 7, 8, 9, 10});
    }

    private void insertSort(int[] array) {
        // 从第二个元素开始遍历
        for (int i = 1; i < array.length; i++) {
            // i 之前的元素可认为已经排序
            // 将 i 对应的元素插入已经排序的数中，可认为同已经排序元素比较，直到找到小于 i 对应的元素
            int insert = array[i];
            int perIndex = i - 1;
            // 如果要插入的元素小于第preIndex个元素，就将第preIndex个元素向后移动
            while (perIndex >= 0 && insert < array[perIndex]) {
                array[perIndex + 1] = array[perIndex];
                perIndex--;
            }
            // 直到要插入的元素不小于第preIndex个元素,将insertNote插入到数组中
            array[perIndex + 1] = insert;
        }
    }

    @Test
    public void selectionSortTest() {
        selectionSort(array);
        Assert.assertArrayEquals(array, new int[]{2, 5, 6, 7, 8, 9, 10});
    }

    private void selectionSort(int[] array) {
        // 每次从数组（i-n）中找出最小的插入已经排序的（0-i）数中
        for (int i = 0; i < array.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < array.length; j++) {
                if (array[min] > array[j]) {
                    min = j;
                }
            }
            int tmp = array[i];
            array[i] = array[min];
            array[min] = tmp;
        }
    }
}
