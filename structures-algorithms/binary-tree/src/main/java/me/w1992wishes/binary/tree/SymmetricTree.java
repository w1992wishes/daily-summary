package me.w1992wishes.binary.tree;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Given a binary tree, check whether it is a mirror of itself (ie, symmetric around its center).
 *
 * For example, this binary tree [1,2,2,3,4,4,3] is symmetric:
 *
 *     1
 *    / \
 *   2   2
 *  / \ / \
 * 3  4 4  3
 *
 *
 * But the following [1,2,2,null,3,null,3] is not:
 *
 *     1
 *    / \
 *   2   2
 *    \   \
 *    3    3
 *
 * Note:
 * Bonus points if you could solve it both recursively and iteratively.
 *
 * @author w1992wishes 2019/6/14 10:39
 */
public class SymmetricTree {

    public boolean isSymmetric(TreeNode root) {
        if (root == null) {
            return true;
        }
        return isSymmetricRecursive(root.left, root.right);
    }

    /**
     * 递归解法：由于对称性，每次把一个节点的左子树和它兄弟节点的右子树进行比较，然后递归处理即可。
     */
    private boolean isSymmetricRecursive(TreeNode left, TreeNode right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return (left.val == right.val) &&
                isSymmetricRecursive(left.left, right.right) &&
                isSymmetricRecursive(left.right, right.left);
    }

    /**
     * 迭代方法：思路和递归一样，此处使用两个队列进行处理.
     *
     * 把左子树的孩子放入第一个队列，右子树的放入第二个队列，每次取队首元素进行判断，速度比递归快了几倍
     */
    private boolean isSymmetricQueue(TreeNode root) {
        if (root == null || (root.left == null && root.right == null)) {
            return true;
        }
        Queue<TreeNode> leftQueue = new LinkedList<>();
        Queue<TreeNode> rightQueue = new LinkedList<>();
        leftQueue.add(root.left);
        rightQueue.add(root.right);
        while (!leftQueue.isEmpty() && !rightQueue.isEmpty()) {
            TreeNode left = leftQueue.poll();
            TreeNode right = rightQueue.poll();
            // 都为空
            if (left == null && right == null) {
                continue;
            }
            // 如果有一个为 null，即返回 false
            if (left == null || right == null) {
                return false;
            }
            // 都不为空
            if (left.val != right.val) {
                return false;
            }
            // 第一个队列先进左子树
            leftQueue.add(left.left);
            leftQueue.add(left.right);
            // 第二个队列先进右子树
            rightQueue.add(right.right);
            rightQueue.add(right.left);
        }
        if (!leftQueue.isEmpty() || !rightQueue.isEmpty()) {
            return false;
        }
        return true;
    }

}
