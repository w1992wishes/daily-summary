package me.w1992wishes.to.offer.day1;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 用两个栈实现一个队列。队列的声明如下，请实现它的两个函数 appendTail 和 deleteHead ，
 * 分别完成在队列尾部插入整数和在队列头部删除整数的功能。(若队列中没有元素，deleteHead操作返回 -1 )
 */
class CQueue {

    //官方建议：使用栈尽量使用ArrayDeque：
    //Deque接口及其实现提供了LIFO堆栈操作的完整和更
    //Deque<Integer> stack=new ArrayDeque<Integer>();
    private Deque<Integer> first;
    private Deque<Integer> second;

    public CQueue() {
        first = new ArrayDeque<>();
        second = new ArrayDeque<>();
    }

    public void appendTail(int value) {
        first.push(value);
    }

    public int deleteHead() {
        if (second.isEmpty()) {
            while (!first.isEmpty()) {
                second.push(first.pop());
            }
        }
        if (!second.isEmpty()) {
            return second.pop();
        }
        return -1;
    }
}
