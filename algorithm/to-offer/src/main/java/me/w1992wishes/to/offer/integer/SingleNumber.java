package me.w1992wishes.to.offer.integer;

import java.util.Arrays;

/**
 * 只出现一次的数字
 * <p>
 * 给你一个整数数组 nums ，除某个元素仅出现 一次 外，其余每个元素都恰出现 三次 。请你找出并返回那个只出现了一次的元素。
 */
public class SingleNumber {
    public int singleNumber(int[] nums) {
        // 比较法 : 先对数组进行排序，然后对 nums[i] 和 nums[i + 2]进行比较，如相等，i += 3，继续下一组比较，直到取到不相等的一组。
        // 先排序  (0, 3, 3, 3, 5, 5, 5), (0, 0, 0, 3, 5, 5, 5), (0, 0, 0, 3, 3, 3, 5)
        Arrays.sort(nums);
        int size = nums.length;
        for (int step = 0; step < size; step += 3) {
            // 找到不相等的一组，直接返回
            if (step + 2 >= size || nums[step] != nums[step + 2]) {
                return nums[step];
            }
        }
        return -1;
    }
}
