package me.w1992wishes.to.offer.array;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 给定一个包含 n 个整数的数组nums，判断nums中是否存在三个元素a ，b ，c ，使得a + b + c = 0 ？请找出所有和为 0 且不重复的三元组。
 */
public class ThreeSum {
    public List<List<Integer>> threeSum(int[] nums) {
        List<List<Integer>> res = new ArrayList<>();
        if (nums == null || nums.length == 0)
            return res;
        int len = nums.length;
        Arrays.sort(nums);  //第一步：先排序

        //依次遍历每一个元素，把它作为a，在后续的元素里找b+c=-a;
        for (int i = 0; i < len - 2; i++) {
            if (i > 0 && nums[i] == nums[i - 1])
                continue;     //重复元素直接跳过
            //找两数之和为-a，双指针法
            int low = i + 1, high = len - 1;
            while (low < high) {
                if (nums[low] + nums[high] == -nums[i]) { //找到了一个解
                    res.add(Arrays.asList(nums[i], nums[low], nums[high]));
                    //已经找到了，重复的去掉
                    while (low < high && nums[low + 1] == nums[low])
                        low++;
                    while (low < high && nums[high - 1] == nums[high])
                        high--;
                    low++;
                    high--;
                } else if (nums[low] + nums[high] < -nums[i])
                    low++;
                else
                    high--;
            }
        }
        return res;
    }
}
