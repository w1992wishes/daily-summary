package me.w1992wishes.algorithm.interview.array;

/**
 * 把一个数组最开始的若干个元素搬到数组的末尾，我们称之为数组的旋转。 输入一个非减排序的数组的一个旋转，输出旋转数组的最小元素。
 * 例如数组{3,4,5,1,2}为{1,2,3,4,5}的一个旋转，该数组的最小值为1。
 * NOTE：给出的所有元素都大于0，若数组大小为0，请返回0。
 *
 * @author w1992wishes 2020/1/8 20:02
 */
public class MinInRotateArray {

    private int min(int[] array) {

        if (array.length == 0) {
            return 0;
        }

        //设置两个指针,一个指向前面,一个指向末尾
        int head = 0;
        int rear = array.length - 1;
        //设置中间指针,如果旋转0个数,那么数组就是直接有序的,不会进入循环,直接返回array[mid]
        int mid = head;

        // 当最左边值大于等于最右边时候
        while (array[head] >= array[rear]) {
            // 如果此时数组只剩下两个数值
            if (rear - head == 1) {
                // 最小的就是右边
                mid = rear;
                break;
            }

            // 如果数组长度是2个以上
            mid = (rear + head) / 2;

            // 假如最左边和中间以及最右边值都相等，只能进行顺序查找，如{1,1,1,0,1}
            if (array[head] == array[rear] && array[head] == array[mid]) {
                return findInOrder(array, head, rear);
            }

            // 如果最左边小于等于中间，说明最小值在后半部分，把mid位置标记为最左侧如{3,4,5,1,2}
            if (array[mid] >= array[head]) {
                head = mid;
            }
            // 如果最右侧大于等于中间值，说明最小值在前半部分，把mid位置标记为最右侧{4,5,1,2,3}
            else if (array[mid] <= array[rear]) {
                rear = mid;
            }

        }

        return array[mid];
    }

    private int findInOrder(int[] array, int head, int rear) {
        int result = array[head];
        for (int i = head + 1; i <= rear; i++) {
            if (result > array[i]) {
                result = array[i];
            }
        }
        return result;
    }

}
