package me.w1992wishes.algorithm.interview.sort;

/**
 * 通常人们整理桥牌的方法是一张一张的来，将每一张牌插入到其他已经有序牌中的适当位置。
 * 在计算机的实现中，为了给要插入的元素腾出空间，我们需要将其余所有元素在插入之前都向右移动一位。这种算法就叫，插入排序。
 *
 * 对一个接近有序或有序的数组进行排序会比随机顺序或是逆序的数组进行排序要快的多。
 *
 * @author w1992wishes 2019/12/30 13:55
 */
public class InsertionSort extends AbstractSort {

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
    @Override
    void sort(int[] numbers) {
        int size = numbers.length;
        int insertNote;// 要插入的数据;

        // 从数组的第二个元素开始循环将数组中的元素插入
        for (int i = 1; i < size; i++) {
            // 设置数组中的第2个元素为第一次循环要插入的数据
            insertNote = numbers[i];
            int preIndex = i - 1;
            while (preIndex >= 0 && insertNote < numbers[preIndex]) {
                // 如果要插入的元素小于第preIndex个元素，就将第preIndex个元素向后移动
                numbers[preIndex + 1] = numbers[preIndex];
                preIndex--;
            }
            // 直到要插入的元素不小于第preIndex个元素,将insertNote插入到数组中
            numbers[preIndex + 1] = insertNote;
        }
    }

}
