package me.w1992wishes.binary.tree;

import java.util.*;

/**
 * Given a binary tree, return the postorder traversal of its nodes' values.
 *
 * Example:
 *
 * Input: [1,null,2,3]
 * 1
 * \
 * 2
 * /
 * 3
 *
 * Output: [3,2,1]
 *
 * Follow up: Recursive solution is trivial, could you do it iteratively?
 *
 * @author w1992wishes 2019/6/11 11:01
 */
public class PostorderTraversal {

    public List<Integer> postorderTraversal(TreeNode root) {
        LinkedList<Integer> nodes = new LinkedList<>();
        recursivePostorder(root, nodes);
        stackPostorder(root, nodes);
        return nodes;
    }

    /**
     * 递归后序实现，左->右->中
     */
    private void recursivePostorder(TreeNode root, List<Integer> nodes) {
        if (root == null) {
            return;
        }
        recursivePostorder(root.left, nodes);
        recursivePostorder(root.right, nodes);
        nodes.add(root.val);
    }

    /**
     * 前序非递归: 中->左->右  入栈顺序:  右左
     *
     * 可以实现 : 中->右->左  入栈顺序:  左右(1)
     *
     * 后续非递归: 左->右->中
     *
     * 遍历的时候是中->右->左，但不遍历，将这个压入另一个栈，最后逆序回来，就能得到左->右->中；
     */
    private void stackPostorder(TreeNode root, LinkedList<Integer> nodes) {
        if (root == null) {
            return;
        }
        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(root);
        TreeNode cur;
        while (!stack.isEmpty()) {
            cur = stack.pop();
            //下面这一步就是 reverse(print(root), visit(right), visit(left))
            nodes.addFirst(cur.val);//代替了另一个栈
            if (cur.left != null) {
                stack.push(cur.left);
            }
            if (cur.right != null) {
                stack.push(cur.right);
            }
        }
    }

    /**
     * 对于任一结点p，先将其入栈。若p不存在左孩子和右孩子，则可以直接访问它；或者p存在左孩子或者右孩子，但是左孩子和右孩子都已经被访问过了，也可以直接访问该结点。
     *
     * 若非上述两种情况，则将右孩子和左孩子依次入栈。这样可以保证每次取栈顶元素时，左孩子在右孩子前面被访问，根结点在左孩子和右孩子访问之后被访问。
     */
    public void postorderTraversal1(TreeNode root) {
        List<Integer> res = new ArrayList<>();
        if (root == null) {
            return;
        }
        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(root);
        TreeNode cur, pre = null;
        while (!stack.isEmpty()) {
            cur = stack.peek();   // 不能直接pop
            if ((cur.left == null && cur.right == null) || ((pre != null) && (pre == cur.left || pre == cur.right))) {
                res.add(cur.val);
                pre = cur;
                stack.pop();
            } else {               // otherwise, can't visis directly
                if (cur.right != null) {
                    stack.push(cur.right);
                }
                if (cur.left != null) {
                    stack.push(cur.left);
                }
            }
        }
    }
}
