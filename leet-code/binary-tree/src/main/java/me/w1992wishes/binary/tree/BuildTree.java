package me.w1992wishes.binary.tree;

import java.util.Arrays;

/**
 * Given inorder and postorder traversal of a tree, construct the binary tree.
 *
 * Note:
 * You may assume that duplicates do not exist in the tree.
 *
 * For example, given
 *
 * inorder = [9,3,15,20,7]
 * postorder = [9,15,7,20,3]
 * Return the following binary tree:
 *
 * ```
 *     3
 *    / \
 *   9  20
 *     /  \
 *    15   7
 * ```
 *
 * @author w1992wishes 2019/6/26 16:53
 */
public class BuildTree {

    /**
     * 后序遍历的最后一个元素一定是根节点，在中序遍历中找出此根节点的位置序号。
     * 中序遍历序号左边的是左孩子，右边的是右孩子。再根据左孩子和右孩子的长度对后序遍历进行切片即可。
     *
     * @param inorder 左中右
     * @param postorder 左右中
     * @return tree
     */
    public TreeNode buildTree(int[] inorder, int[] postorder) {
        int inStart = 0;
        int inEnd = inorder.length - 1;
        int postStart = 0;
        int postEnd = postorder.length - 1;

        return buildTree(inorder, inStart, inEnd, postorder, postStart, postEnd);
    }

    public TreeNode buildTree(int[] inorder, int inStart, int inEnd,
                              int[] postorder, int postStart, int postEnd) {
        if (inStart > inEnd || postStart > postEnd) {
            return null;
        }

        int rootValue = postorder[postEnd];
        TreeNode root = new TreeNode(rootValue);

        int k = Arrays.asList(inorder).indexOf(rootValue);

        root.left = buildTree(inorder, inStart, k - 1, postorder, postStart,
                postStart + k - (inStart + 1));
        // Becuase k is not the length, it it need to -(inStart+1) to get the length
        root.right = buildTree(inorder, k + 1, inEnd, postorder, postStart + k- inStart, postEnd - 1);
        // postStart+k-inStart = postStart+k-(inStart+1) +1

        return root;
    }

}
