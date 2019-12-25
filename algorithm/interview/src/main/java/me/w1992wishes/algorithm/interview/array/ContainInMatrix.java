package me.w1992wishes.algorithm.interview.array;

/**
 *
 * 在一个二维数组中，每一行都按照从左到右递增的顺序排序，每一列都按照从上到下递增的顺序排序。
 *
 * 请完成一个函数，输入这样的一个二维数组和一个整数，判断数组中是否含有该整数。
 *
 * @author w1992wishes 2019/12/23 20:42
 */
public class ContainInMatrix {

    public static void main(String[] args) {

        int[][] matrix = new int[][]{{1,2,3}, {4,5,6}};
        int n = 5;

        System.out.println(contain(matrix, 2, 2, 5));
    }

    private static boolean contain(int[][] matrix, int rows, int cols, int n) {

        boolean found = false;

        if (matrix != null && rows > 0 && cols > 0) {
            int row = 0;
            int col = cols -1;

            while (row < rows && col >= 0) {
                if (matrix[row][col] == n) {
                    found = true;
                    break;
                } else if (matrix[row][col] > n) {
                    -- col;
                } else {
                    ++ row;
                }
            }
        }

        return found;
    }

}
