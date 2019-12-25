package me.w1992wishes.algorithm.interview.tree;

import java.util.Arrays;

/**
 * 输入某二叉树的前序遍历和中序遍历的结果，请重建出该二叉树。
 *
 * 假设输入的前序遍历和中序遍历的结果中都不含重复的数字。
 *
 * 例如输入前序遍历序列{1,2,4,7,3,5,6,8}和中序遍历序列{4,7,2,1,5,3,8,6}，则重建二叉树并返回。
 *
 * @author w1992wishes 2019/12/25 17:11
 */
public class RebuildBinaryTree {

    public static void main(String[] args) {

    }

    private static class BinaryTreeNode {
        int value;
        BinaryTreeNode left;
        BinaryTreeNode right;

        BinaryTreeNode(int value) {
            this.value = value;
        }
    }

    /**
     * 1、前序遍历序列中的第一个元素为根节点
     * 2、找到该根节点在中序遍历序列中的位置，左侧即为左树的遍历序列，右侧为右树的遍历序列
     * 3、根据左树遍历序列的数组长度len，找出前序遍历序列的第2到第len+1个元素，第一个元素即为左树中的根节点，右树的根节点是前序遍历序列的最后一个节点
     * 4、递归进行上述过程，构建树
     */
    private static BinaryTreeNode build(int[] preorder, int preStart, int preEnd, int[] inorder, int inStart, int inEnd) {
        if (preStart > preEnd || inStart > inEnd) {
            return null;
        }
        // 1、根节点的值
        int rootVal = preorder[preStart];

        // 2.寻找根节点在中序序列的位置
        int rootIdxInOrder = Arrays.asList(inorder).indexOf(rootVal);

        // 没找到，参数不合法，返回null
        if (rootIdxInOrder < 0) {
            return null;
        }

        // 3、生成头节点
        BinaryTreeNode root = new BinaryTreeNode(rootVal);

        // 只有一个元素，直接返回，减少递归层次
        if (preStart == preEnd && inStart == inEnd) {
            return root;
        }

        // 4、左子树长度
        int leftTreeLen = rootIdxInOrder - inStart;

        // 5、前序遍历序列中，左子树遍历序列结束索引为 preStart + 1 + leftTreeLen - 1
        int preIdxLeftTreeEnd = preStart + leftTreeLen;

        // 可以计算出中序序列的左右子树序列为:左：inStart ~ k-1，右：k+1 ~ inEnd
        // 前序序列的左右子树：左：preStart+1 ~ preStart+k-inStart， 右：preStart+k-inStart+1 ~ preEnd
        root.left = build(preorder, preStart + 1, preIdxLeftTreeEnd, inorder, inStart, rootIdxInOrder - 1);
        root.right = build(preorder, preStart + preIdxLeftTreeEnd + 1, preEnd, inorder, rootIdxInOrder + 1, inEnd);
        return root;
    }
}
