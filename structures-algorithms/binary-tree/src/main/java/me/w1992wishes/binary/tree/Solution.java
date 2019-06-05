package me.w1992wishes.binary.tree;

import java.util.*;

/**
 * Definition for a binary tree node.
 * public class TreeNode {
 * int val;
 * TreeNode left;
 * TreeNode right;
 * TreeNode(int x) { val = x; }
 * }
 * <p>
 * Example:
 * Input: [1,null,2,3]
 * 1
 * \
 * 2
 * /
 * 3
 * <p>
 * Output: [1,2,3]
 */
class Solution {

    /**
     * 二叉树的前序遍历是:中->左->右。采用这个次序进行递归。
     */
    private void recursivePreorder(TreeNode root, List<Integer> nodes) {
        if (root == null) {
            return;
        }
        nodes.add(root.val);
        recursivePreorder(root.left, nodes);
        recursivePreorder(root.right, nodes);
    }

    /**
     * 递归的思想其实就是栈思想，因此非递归版本采用栈来实现。观察递归版本可知，函数先递归求解左子树，再求解右子树。
     * <p>
     * 因而采用栈（先进后出特性）时，为了先处理左子树，再处理右子树，只能先将右子树入栈，再将左子树入栈。
     */
    private void iterativePreorder(TreeNode root, List<Integer> nodes) {
        if (root == null) {
            return;
        }
        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            TreeNode t = stack.pop();
            nodes.add(t.val);
            if (t.right != null) {
                stack.push(t.right);
            }
            if (t.left != null) {
                stack.push(t.left);
            }
        }
    }

    /**
     * Given a binary tree, return the preorder traversal of its nodes' values.
     */
    public List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> nodes = new ArrayList<>();
        recursivePreorder(root, nodes);
        iterativePreorder(root, nodes);
        return nodes;
    }
}