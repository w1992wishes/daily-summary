package me.w1992wishes.binary.tree;

import java.util.*;

/**
 * Given a binary tree, return the inorder traversal of its nodes' values.
 * <p>
 * In-order traversal is to traverse the left subtree first. Then visit the root. Finally, traverse the right subtree.
 *
 * @author w1992wishes 2019/6/11 10:56
 */
public class InorderTraversal {

    public List<Integer> inorderTraversal(TreeNode root) {
        List<Integer> nodes = new ArrayList<>();
        recursiveInorder(root, nodes);
        stackInorder(root, nodes);
        return nodes;
    }


    /**
     * 递归中序实现，左->中->右
     */
    private void recursiveInorder(TreeNode root, List<Integer> nodes) {
        if (root == null) {
            return;
        }
        recursiveInorder(root.left, nodes);
        nodes.add(root.val);
        recursiveInorder(root.right, nodes);
    }

    /**
     * 栈的非递归中序遍历，左->中->右
     */
    private void stackInorder(TreeNode root, List<Integer> nodes) {
        if (root == null) {
            return;
        }
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode node = root;
        while (node != null || !stack.isEmpty()) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
            node = stack.pop();
            nodes.add(node.val);
            node = node.right;
        }
    }

}