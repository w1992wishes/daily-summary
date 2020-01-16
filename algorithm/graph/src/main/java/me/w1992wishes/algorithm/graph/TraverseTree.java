package me.w1992wishes.algorithm.graph;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Stack;

/**
 * @author w1992wishes 2020/1/16 18:47
 */
public class TraverseTree {

    public static void main(String[] args) {
        TreeNode node1 = new TreeNode(1);
        TreeNode node2 = new TreeNode(2);
        TreeNode node3 = new TreeNode(3);
        TreeNode node4 = new TreeNode(4);
        TreeNode node5 = new TreeNode(5);
        TreeNode node6 = new TreeNode(6);
        TreeNode node7 = new TreeNode(7);
        TreeNode node8 = new TreeNode(8);
        TreeNode node9 = new TreeNode(9);

        node1.leftNode = node2;
        node1.rightNode = node3;
        node2.leftNode = node4;
        node2.rightNode = node5;
        node3.leftNode = node6;
        node3.rightNode = node7;
        node5.rightNode = node8;
        node7.leftNode = node9;

        TraverseTree traverseTree = new TraverseTree();
        traverseTree.depthFirstSearch(node1);
        System.out.println();
        traverseTree.broadFirstSearch(node1);
    }

    /**
     * 二叉树数据结构
     */
    private static class TreeNode {
        int data;
        TreeNode leftNode;
        TreeNode rightNode;

        public TreeNode() {
        }

        public TreeNode(int d) {
            data = d;
        }

        public TreeNode(TreeNode leftNode, TreeNode rightNode, int d) {
            leftNode = leftNode;
            rightNode = rightNode;
            data = d;
        }

    }

    private void depthFirstSearch(TreeNode nodeHead) {
        if (nodeHead == null) {
            return;
        }
        Stack<TreeNode> stack = new Stack<>();
        stack.push(nodeHead);
        while (!stack.isEmpty()) {
            // 弹出栈顶元素
            TreeNode node = stack.pop();
            System.out.print(node.data + " ");
            // 深度优先遍历，先遍历左边，后遍历右边,栈先进后出
            if (node.rightNode != null) {
                stack.push(node.rightNode);
            }
            if (node.leftNode != null) {
                stack.push(node.leftNode);
            }
        }
    }

    private void broadFirstSearch(TreeNode nodeHead) {
        if (nodeHead == null) {
            return;
        }

        Queue<TreeNode> queue = new ArrayDeque<>();
        queue.offer(nodeHead);

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            System.out.print(node.data + " ");
            // 广度优先遍历，在这里采用每一行从左到右遍历
            if (node.leftNode != null) {
                queue.offer(node.leftNode);
            }
            if (node.rightNode != null) {
                queue.offer(node.rightNode);
            }
        }
    }
}
