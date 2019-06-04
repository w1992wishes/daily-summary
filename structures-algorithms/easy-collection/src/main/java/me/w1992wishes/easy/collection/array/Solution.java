package me.w1992wishes.easy.collection.array;


/**
 *
 * @author w1992wishes 2019/6/4 14:04
 */
public class Solution {

    /**
     * Q: Given a sorted array nums, remove the duplicates in-place such that each element appear only once and return the new length.
     *
     * Do not allocate extra space for another array, you must do this by modifying the input array in-place with O(1) extra memory.
     *
     * S: 使用快慢指针来记录遍历的坐标，最开始时两个指针都指向第一个数字，如果两个指针指的数字相同，则快指针向前走一步，
     *
     * 如果不同，则两个指针都向前走一步，这样当快指针走完整个数组后，慢指针当前的坐标加1就是数组中不同数字的个数
     */
    public int removeDuplicates(int[] nums) {
        if (nums == null || nums.length == 0) {
            return 0;
        }

        int pre = 0; int cur = 0; int n = nums.length;
        while (cur < n) {
            if (nums[pre] != nums[cur]) {
                nums[++pre] = nums[cur];
            }
            cur++;
        }
        return pre + 1;
    }

}
