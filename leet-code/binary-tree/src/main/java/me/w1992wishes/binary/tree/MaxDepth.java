package me.w1992wishes.binary.tree;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Given a binary tree, find its maximum depth.
 *
 * The maximum depth is the number of nodes along the longest path from the root node down to the farthest leaf node.
 *
 * Note: A leaf is a node with no children.
 *
 * Example:
 *
 * Given binary tree [3,9,20,null,null,15,7],
 * 3
 * / \
 * 9  20
 * /  \
 * 15   7
 * return its depth = 3.
 *
 * @author w1992wishes 2019/6/13 11:26
 */
public class MaxDepth {

    public int maxDepth(TreeNode root) {
        return recursiveMaxDepth(root);
    }

    /**
     * 深度优先搜索DFS，递归
     */
    private int recursiveMaxDepth(TreeNode treeNode) {
        if (treeNode == null) {
            return 0;
        }
        int leftDepth = recursiveMaxDepth(treeNode.left);
        int rightDepth = recursiveMaxDepth(treeNode.right);
        return Math.max(leftDepth, rightDepth) + 1;
    }

    /**
     * 也可以使用层序遍历二叉树，然后计数总层数，即为二叉树的最大深度
     */
    private int queueMaxDepth(TreeNode treeNode) {
        if (treeNode == null) {
            return 0;
        }
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(treeNode);
        int size = queue.size();
        int depth = 0;
        while (!queue.isEmpty()) {
            size--;
            TreeNode node = queue.poll();

            if (node.left != null) {
                queue.add(node.left);
            }
            if (node.right != null) {
                queue.add(node.right);
            }

            if (size == 0) {
                size = queue.size();
                depth++;
            }
        }
        return depth;
    }

}
