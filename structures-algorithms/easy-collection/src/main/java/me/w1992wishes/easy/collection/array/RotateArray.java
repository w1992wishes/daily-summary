package me.w1992wishes.easy.collection.array;

/**
 * @author w1992wishes 2019/6/10 11:38
 */
public class RotateArray {

    /**
     * Given an array, rotate the array to the right by k steps, where k is non-negative.
     * <p>
     * Example 1:
     * <p>
     * Input: [1,2,3,4,5,6,7] and k = 3
     * Output: [5,6,7,1,2,3,4]
     * Explanation:
     * rotate 1 steps to the right: [7,1,2,3,4,5,6]
     * rotate 2 steps to the right: [6,7,1,2,3,4,5]
     * rotate 3 steps to the right: [5,6,7,1,2,3,4]
     */
    public void rotate(int[] nums, int k) {
        rotate1(nums, k);
    }

    /**
     * 效率较低，易理解
     */
    private void rotate1(int[] nums, int k) {
        k = k % nums.length;

        int temp;
        int n = nums.length;
        for (int step = 0; step < k; step++) {
            temp = nums[n - 1];
            for (int i = n - 1; i > 0; --i) {
                nums[i] = nums[i - 1];
            }
            nums[0] = temp;
        }
    }

    /**
     * 看一种O(n)的空间复杂度的方法，复制一个和 nums 一样的数组，然后利用映射关系i -> (i+k)%n来交换数字
     */
    private void rotate2(int[] nums, int k) {
        k =  k % nums.length;

        int[] t = nums.clone();

        for (int i = 0; i < nums.length; i++) {
            nums[(i + k) % nums.length] = t[i];
        }
    }
}
