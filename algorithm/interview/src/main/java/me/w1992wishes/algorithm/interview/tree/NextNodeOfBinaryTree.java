package me.w1992wishes.algorithm.interview.tree;

/**
 * 给定一个二叉树和其中的一个结点，请找出中序遍历顺序的下一个结点并且返回。
 *
 * 注意，树中的结点不仅包含左右子结点，同时包含指向父结点的指针。
 *
 * @author w1992wishes 2019/12/25 17:48
 */
public class NextNodeOfBinaryTree {

    private static class BinaryTreeNode {
        int value;
        BinaryTreeNode left;
        BinaryTreeNode right;
        BinaryTreeNode parent;

        BinaryTreeNode(int value) {
            this.value = value;
        }
    }

    /**
     * 首先这道题给出的是中序遍历这个二叉树，那么就是左根右。我们在求一个结点的下一个结点，那么这个时候我们需要分情况讨论：
     *
     * 1、如果该结点有右子树，则该结点的下一个结点为该结点的右子树的最左结点。
     *
     * 2、如果该结点没有右子树，则又分两种情况讨论：
     *
     * 情况一：它是父节点的左子节点，那么它的下一个节点就是它的父节点。
     * 情况二：它是父节点的右子节点，可以沿着指向父节点的指针一直向上遍历，直到其中的一个父结点是其父结点的左孩子，则该父结点的父结点为下一个结点。
     */
    private static BinaryTreeNode nextNode(BinaryTreeNode node) {

        if (node == null) {
            return null;
        }

        // 如果该结点有右子树，则该结点的下一个结点为该结点的右子树的最左结点。
        if(node.right!=null){
            node = node.right;
            while(node.left!=null){
                node=node.left;
            }
            return node;
        }

        // 如果该结点没有右子树，则又分两种情况讨论：
        // 它是父节点的左子节点，那么它的下一个节点就是它的父节点。
        // 它是父节点的右子节点，可以沿着指向父节点的指针一直向上遍历，直到其中的一个父结点是其父结点的左孩子，则该父结点的父结点为下一个结点。
        while (node.parent != null) {
            BinaryTreeNode parent = node.parent;
            if (parent.left == node) {
                return parent;
            }
            node = parent;
        }

        return null;

    }

}
