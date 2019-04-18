package me.w1992wishes.common.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author w1992wishes 2019/4/18 15:00
 */
public class ListUtils {

    public static <T> List<List<T>> divideList(List<T> list, int segments) {
        List<List<T>> lists = new ArrayList<>();
        if (segments == 0) {
            lists.add(list);
        } else {
            int remainder = list.size() % segments;
            int number = list.size() / segments;
            int offset = 0;
            // 拆分成多份
            for (int i = 0; i < segments; i++) {
                int start = i * number + offset;
                int end;
                if (remainder > 0) {
                    end = (i + 1) * number + offset + 1;
                    remainder--;
                    offset++;
                } else {
                    end = (i + 1) * number + offset;
                }
                List<T> subList = list.subList(start, end);
                lists.add(subList);
            }
        }
        return lists;
    }

}
