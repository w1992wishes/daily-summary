package me.w1992wishes.algorithm.interview.list;

import java.util.Stack;

/**
 * 第一个栈负责添加元素，弹出元素时，首先判断第二个栈是否为空，若为空就直接将其第一个栈中的数据全部压入第二个栈中，然后输出栈顶元素，即可实现队列效果；
 *
 * 若第二个栈中有数据，直接将其数据压入第一个栈中，输出时直接输出第二个栈顶的元素即可！
 *
 * 需要等第二个栈中没有元素了，才能往第二个栈添加第一个栈 pop 出的元素
 *
 * @author Administrator
 */
public class TwoStackImplQueue<T> {
    private Stack<T> stack1 = new Stack<T>();
    private Stack<T> stack2 = new Stack<T>();

    private void appendTail(T t) {
        // stack1只负责压入队列元素
        stack1.push(t);
    }

    private T deleteHead() {
        //若stack2为空，将 stack1 中的元素压入 stack2
        if (stack2.isEmpty()) {
            while (!stack1.isEmpty()) {
                stack2.push(stack1.pop());
            }
        }
        return stack2.pop();
    }
}



