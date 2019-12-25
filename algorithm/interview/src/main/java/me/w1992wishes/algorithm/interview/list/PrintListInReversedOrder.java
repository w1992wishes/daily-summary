package me.w1992wishes.algorithm.interview.list;

import java.util.Stack;

/**
 * 输入一个链表的头结点，从尾到头反过来打印出每个结点的值。
 *
 * @author w1992wishes 2019/12/25 17:00
 */
public class PrintListInReversedOrder {

    public static void main(String[] args) {

    }

    private static class ListNode {
        int val;
        ListNode next;
    }

    // 采用栈
    public static void printListReversingly_Iteratively(ListNode node) {
        Stack<ListNode> stack = new Stack<ListNode>();
        while (node != null) {
            stack.push(node);
            node = node.next;
        }
        while (!stack.empty()) {
            System.out.println(stack.pop().val);
        }
    }

    //采用递归
    public static void printListReversingly_Recursively(ListNode node) {
        if (node != null) {
            printListReversingly_Recursively(node.next);
            System.out.println(node.val);
        } else {
            return;
        }

    }

}
