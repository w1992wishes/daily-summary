package me.w1992wishes.easy.collection.array;

import java.util.HashSet;
import java.util.Set;

/**
 * Given an array of integers, find if the array contains any duplicates.
 * <p>
 * Your function should return true if any value appears at least twice in the array, and it should return false if every element is distinct.
 * <p>
 * Example 1:
 * <p>
 * Input: [1,2,3,1]
 * Output: true
 *
 * @author w1992wishes 2019/6/11 9:40
 */
public class ContainsDuplicateArray {

    /**
     * 使用两层for循环，外层控制当前元素，内层控制剩下的元素，依次比较，发现重复元素即可返回false。
     * <p>
     * 此解法的时间复杂度是O(n^2)，空间复杂度是O(1)。
     */
    public boolean containsDuplicate(int[] nums) {
        int pre = 0;
        int cur = 0;
        int n = nums.length;
        while (pre < n) {
            while (cur < n) {
                if (nums[pre] == nums[cur++]) {
                    return true;
                }
            }
            pre++;
        }
        return false;
    }

    /**
     * 使用HashSet，借助其add方法，如果当前元素已经存在则返回false，说明存在重复元素。
     * <p>
     * 此解法的时间复杂度是O(n)，空间复杂度是O(n)。
     */
    public boolean containsDuplicate1(int[] nums) {
        Set<Integer> set = new HashSet<>();
        for (int i=0; i<nums.length; i++) {
            if (!set.add(nums[i])) {
                return true;
            }
        }
        return false;
    }

}
