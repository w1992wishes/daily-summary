package me.w1992wishes.binary.tree;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Given a binary tree and a sum, determine if the tree has a root-to-leaf path such that adding up all the values along the path equals the given sum.
 *
 * Note: A leaf is a node with no children.
 *
 * Example:
 *
 * Given the below binary tree and sum = 22,
 *
 * ```
 *       5
 *      / \
 *     4   8
 *    /   / \
 *   11  13  4
 *  /  \      \
 * 7    2      1
 * ```
 *
 * return true, as there exist a root-to-leaf path 5->4->11->2 which sum is 22.
 *
 * @author w1992wishes 2019/6/19 19:11
 */
public class PathSum {

    /**
     * 递归的方式
     */
    private boolean hasPathSum(TreeNode root, int sum) {
        if (root == null) {
            return false;
        }
        if (root.left == null && root.right == null && root.val == sum) {
            return true;
        }
        return hasPathSum(root.left, sum - root.val) || hasPathSum(root.right, sum - root.val);
    }

    /**
     * 迭代的方式，利用前序遍历
     */
    private boolean hasPathSum1(TreeNode root, int sum) {
        if (root == null) {
            return false;
        }
        LinkedList<TreeNode> nodes = new LinkedList<>();
        LinkedList<Integer> values = new LinkedList<>();

        nodes.add(root);
        values.add(root.val);

        while(!nodes.isEmpty()){
            TreeNode curr = nodes.poll();
            int sumValue = values.poll();

            if(curr.left == null && curr.right == null && sumValue==sum){
                return true;
            }

            if(curr.left != null){
                nodes.add(curr.left);
                values.add(sumValue+curr.left.val);
            }

            if(curr.right != null){
                nodes.add(curr.right);
                values.add(sumValue+curr.right.val);
            }
        }
        return false;
    }
}
