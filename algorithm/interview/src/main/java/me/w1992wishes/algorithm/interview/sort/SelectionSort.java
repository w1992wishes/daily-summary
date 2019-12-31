package me.w1992wishes.algorithm.interview.sort;

/**
 *
 * 选择排序是另一个很容易理解和实现的简单排序算法。
 *
 * 学习它之前首先要知道它的两个很鲜明的特点。
 *
 * 1,运行时间和输入无关。
 * 为了找出最小的元素而扫描一遍数组并不能为下一遍扫描提供任何实质性帮助的信息。
 * 因此使用这种排序的我们会惊讶的发现，一个已经有序的数组或者数组内元素全部相等的数组和一个元素随机排列的数组所用的排序时间竟然一样长！
 * 而其他算法会更善于利用输入的初始状态，选择排序则不然。
 *
 * 2,数据移动是最少的。
 * 选择排序的交换次数和数组大小关系是线性关系。
 *
 * @author w1992wishes 2019/12/30 11:42
 */
public class SelectionSort extends AbstractSort {

    // 在要排序的一组数中，选出最小的一个数与第一个位置的数交换；
    // 然后在 剩下的数 当中再找最小的与第二个位置的数交换，如此循环到倒数第二个数和最后一个数比较为止。

    /**
     * n 个记录的直接选择排序可经过 n-1 趟直接选择排序得到有序结果。具体算法描述如下：
     *
     * 初始状态：无序区为R[1..n]，有序区为空；
     * 第i趟排序(i=1,2,3…n-1)开始时，当前有序区和无序区分别为R[1..i-1]和R(i..n）。
     * 该趟排序从当前无序区中-选出关键字最小的记录 R[k]，将它与无序区的第1个记录R交换，
     * 使R[1..i]和R[i+1..n)分别变为记录个数增加1个的新有序区和记录个数减少1个的新无序区；
     * n-1趟结束，数组有序化了。
     */
    @Override
    void sort(int[] numbers) {
        int size = numbers.length; // 数组长度
        int temp = 0; // 中间变量
        int tempIndex = 0; // 中间 index

        for (int i = 0; i < size - 1; i++) {
            // 选出最小的
            for (int j = i + 1; j < size; j++) {
                if (numbers[j] < numbers[i]) {
                    tempIndex = j;
                }
            }
            // 交换两个数
            temp = numbers[i];
            numbers[i] = numbers[tempIndex];
            numbers[tempIndex] = temp;
        }
    }

}
