package me.w1992wishes.to.offer.array;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 给定一个包含 n 个整数的数组nums，判断nums中是否存在三个元素a ，b ，c ，使得a + b + c = 0 ？请找出所有和为 0 且不重复的三元组。
 */
public class ThreeSum {
    public List<List<Integer>> threeSum(int[] nums) {
        if (nums == null || nums.length < 3)
            return new ArrayList<>();

        List<List<Integer>> res = new ArrayList<>();

        Arrays.sort(nums); // O(nlogn)

        // 利用双指针，外层套一个 for 循环
        int n = nums.length;
        for (int i = 0; i < n; i++) {
            // 优化1
            if (nums[i] > 0) {
                break;
            }
            // 优化2
            if (i > 0 && nums[i] == nums[i - 1]) {
                continue;
            }
            int left = i + 1;
            int right = n - 1;
            while (left < right) {
                int total = nums[i] + nums[left] + nums[right];
                if (total == 0) {
                    res.add(Arrays.asList(nums[i], nums[left], nums[right]));
                    while (left < right && nums[left] == nums[left + 1]) {
                        left++;
                    }
                    left++;
                } else if (total < 0) {
                    left++;
                } else {
                    right--;
                }
            }
        }
        return res;
    }
}
