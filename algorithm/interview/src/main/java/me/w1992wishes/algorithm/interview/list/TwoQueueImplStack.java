package me.w1992wishes.algorithm.interview.list;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * 两个队列添加元素，哪个队列为空，由于在输出元素时，要进行相应元素的移动（除去尾部元素），所以要在对应不为空的队列进行元素的添加；
 *
 * 在输出数据时，要进行两个队列的变相操作，不为空的队列要依次向为空的队列中添加元素，直到尾元素输出即可！
 *
 * @author w1992wishes 2019/12/26 17:48
 */
public class TwoQueueImplStack<T> {

    private Queue<T> queue1 = new ArrayDeque<>();
    private Queue<T> queue2 = new ArrayDeque<>();


    /**
     * 解决该题的关键主要在于两个关键：
     *
     * <1>入栈元素应在放在那个队列中？？？
     *
     * 当两个队列均为空的时候，任意插入一个队列，当再次插入元素时，选择非空的队列进行插入；
     *
     * <2>出栈时应该注意什么？？？
     *
     * 出栈时，将非空队列中的元素陆续出队并插入到空队列中直至非空队列只有队尾元素时；
     */



    /**
     * 向栈中压入数据
     */
    public void push(T element) {
        //两个队列为空时，优先考虑queue1
        if (queue1.isEmpty() && queue2.isEmpty()) {
            queue1.add(element);
            return;
        }

        //如果queue1为空，queue2有数据，直接放入queue2
        if (queue1.isEmpty()) {
            queue2.add(element);
            return;
        }

        //如果queue2为空，queue1有数据，直接放入queue1
        if (queue2.isEmpty()) {
            queue1.add(element);
            return;
        }
    }

    /**
     * 取出栈中的数据
     */
    public T poll() {
        //两个队列为空时，直接抛出异常
        if (queue1.isEmpty() && queue2.isEmpty()) {
            throw new RuntimeException("stack is empty");
        }

        //如果queue1为空，将queue2中的元素依次加入到 queue1, 弹出最后一个元素
        if (queue1.isEmpty()) {
            while(queue2.size() > 1) {
                queue1.add(queue2.poll());
            }
            return queue2.poll();
        }

        //如果queue2为空，将queue1中的元素依次加入到 queue2, 弹出最后一个元素
        if (queue2.isEmpty()) {
            while(queue1.size() > 1) {
                queue2.add(queue1.poll());
            }
            return queue1.poll();
        }
        return null;
    }

}
