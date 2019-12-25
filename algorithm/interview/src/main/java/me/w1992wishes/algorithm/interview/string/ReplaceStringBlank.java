package me.w1992wishes.algorithm.interview.string;

/**
 *
 * 请实现一个函数，把字符串中的每个空格替换成"%20"。例如输入“We are happy.”，则输出“We%20are%20happy.”。
 *
 * @author w1992wishes 2019/12/25 11:27
 */
public class ReplaceStringBlank {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        String s = "we are happy";
        StringBuffer str = new StringBuffer(s);
        System.out.println(s);
        System.out.println(replaceSpace1(str));
        System.out.println(replaceSpace2(str));
    }

    /**
     * 寻常直接替换，需要多次挪动字符串，时间复杂度为 O(n2)
     *
     * 改用 buffer，负责度为 O(n)
     *
     */
    private static String replaceSpace1(StringBuffer str) {
        if (str == null || str.length() == 0) {
            return str.toString();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            sb.append(ch == ' ' ? "%20" : ch);
        }
        return sb.toString();
    }

    public static String replaceSpace2(StringBuffer str){
        if (str == null || str.length() == 0) {
            return str.toString();
        }

        int len = str.length();
        for (int i = 0; i < len; ++i) {
            if (str.charAt(i) == ' ') {
                // append 两个空格
                str.append("  ");
            }
        }

        // p 指向原字符串末尾
        int p = len - 1;

        // q 指向现字符串末尾
        int q = str.length() - 1;

        while (p >= 0) {
            char ch = str.charAt(p--);
            if (ch == ' ') {
                str.setCharAt(q--, '0');
                str.setCharAt(q--, '2');
                str.setCharAt(q--, '%');
            } else {
                str.setCharAt(q--, ch);
            }
        }
        return str.toString();
    }

}
