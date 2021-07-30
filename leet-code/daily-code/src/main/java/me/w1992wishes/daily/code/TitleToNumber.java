package me.w1992wishes.daily.code;

import java.util.HashMap;
import java.util.Map;

/**
 * 给你一个字符串 columnTitle ，表示 Excel 表格中的列名称。返回该列名称对应的列序号。
 */
public class TitleToNumber {
    public int titleToNumber(String columnTitle) {
        /**
         * 因为有 26 个字母，所以相当于 26 进制，每 26 个数则向前进一位
         * 所以每遍历一位则ans = ans * 26 + num
         * 以 ZY 为例，Z 的值为 26，Y 的值为 25，则结果为 26 * 26 + 25=701
         */
        int ans = 0;
        for (int i = 0; i < columnTitle.length(); i++) {
            int num = columnTitle.charAt(i) - 'A' + 1;
            ans = ans * 26 + num;
        }
        return ans;
    }
}
