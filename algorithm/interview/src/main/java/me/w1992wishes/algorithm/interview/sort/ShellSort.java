package me.w1992wishes.algorithm.interview.sort;

/**
 * 对于大规模乱序的数组插入排序很慢，因为它只会交换相邻的元素，因此元素只能一点一点地从数组的一端移动到另一端。
 *
 * 如果最小的元素刚好在数组的尽头的话，那么要将它移动到正确的位置要N-1次移动。
 *
 * 1959年Shell发明，第一个突破O(n2)的排序算法，是简单插入排序的改进版。它与插入排序的不同之处在于，它会优先比较距离较远的元素。希尔排序又叫缩小增量排序。
 * 希尔排序交换不相邻的元素以对数组的局部进行排序，并最终用插入排序将局部有序的数组排序。
 *
 * 希尔排序，也称递减增量排序算法，是插入排序的一种更高效的改进版本。但希尔排序是非稳定排序算法。
 *
 * @author w1992wishes 2019/12/30 14:36
 */
public class ShellSort extends AbstractSort {

    /**
     * 先将整个待排序的记录序列分割成为若干子序列分别进行直接插入排序，待整个序列中的记录“基本有序”时，再对全体记录进行依次直接插入排序。具体算法描述：
     *
     * 选择一个增量序列 t1，t2，……，tk，其中 ti > tj, tk = 1；
     * 按增量序列个数 k，对序列进行 k 趟排序；
     * 每趟排序，根据对应的增量 ti，将待排序列分割成若干长度为 m 的子序列，分别对各子表进行直接插入排序。
     * 仅增量因子为 1 时，整个序列作为一个表来处理，表长度即为整个序列的长度。
     */
    @Override
    void sort(int[] numbers) {
        int j = 0;
        int temp = 0;
        //希尔排序
        // 每次将步长缩短为原来的一半
        for (int increment = numbers.length / 2; increment > 0; increment /= 2) {
            // 从第 increment 个元素，逐个对其所在组进行直接插入排序操作
            for (int i = increment; i < numbers.length; i++) {
                temp = numbers[i];
                for (j = i; j >= increment; j -= increment) {
                    // 如想从小到大排只需修改这里
                    if (temp < numbers[j - increment]) {
                        numbers[j] = numbers[j - increment];
                    } else {
                        break;
                    }

                }
                numbers[j] = temp;
            }
        }
    }
}
