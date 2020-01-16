package me.w1992wishes.algorithm.search;

/**
 * @author w1992wishes 2020/1/16 10:13
 */
public class SequentialSearch {

    public static void main(String[] args) {
        System.out.println(new SequentialSearch().sequentialSearch(new int[]{1,2,3,4}, 3));
    }

    private int sequentialSearch(int[] a, int key) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == key) {
                return i;
            }
        }
        return -1;
    }
}
