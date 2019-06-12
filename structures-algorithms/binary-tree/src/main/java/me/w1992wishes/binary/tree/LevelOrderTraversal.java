package me.w1992wishes.binary.tree;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Given a binary tree, return the level order queueTraversal of its nodes' values. (ie, from left to right, level by level).
 *
 * For example:
 * Given binary tree [3,9,20,null,null,15,7],
 *     3
 *    / \
 *   9  20
 *     /  \
 *    15   7
 * return its level order queueTraversal as:
 * [
 *   [3],
 *   [9,20],
 *   [15,7]
 * ]
 *
 * @author w1992wishes 2019/6/12 14:07
 */
public class LevelOrderTraversal {

    public List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> res = new ArrayList<>();
        queueTraversal(root, res);
        return res;
    }

    /**
     * 使用队列辅助实现
     *
     * 树为空时，return null
     *
     * 遍历树--> root入队，通过队列遍历树，当父层节点出队时，子层节点入队。每次将一层遍历完，将其层（list)存到结果res
     *
     * return res
     */
    private void queueTraversal(TreeNode treeNode, List<List<Integer>> res) {
        if (treeNode == null) {
            return;
        }
        List<Integer> level = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(treeNode);
        int size = queue.size();
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            level.add(node.val);
            if (node.left != null) {
                queue.add(node.left);
            }
            if (node.right != null) {
                queue.add(node.right);
            }

            size--;
            // 遍历完一层后需重新构建 list，并重新计算下一层的 size
            if (size == 0) {
                res.add(level);
                level = new ArrayList<>();
                size = queue.size();
            }
        }
    }

    public List<List<Integer>> levelOrder1(TreeNode root) {
        List<List<Integer>> res = new ArrayList<>();
        recursiveTraversal(root, 0, res);
        return res;
    }

    /**
     * 递归实现
     *
     * 判断数是否为空，为空则return
     *
     * 判断当前遍历的层次level，result中是否有记录了（是否第一次遍历该层）
     *
     * 根据对应层次添加节点 result.get(level).add
     */
    private void recursiveTraversal(TreeNode treeNode, int level, List<List<Integer>> res) {
        if (treeNode == null) {
            return;
        }
        if (level == res.size()) {
            res.add(new ArrayList<>());
        }
        res.get(level).add(treeNode.val);
        recursiveTraversal(treeNode.left, level + 1, res);
        recursiveTraversal(treeNode.right, level + 1, res);
    }

}
