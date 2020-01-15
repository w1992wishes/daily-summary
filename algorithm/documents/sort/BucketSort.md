# 【Sort】桶排序

[TOC]

## 一、思想

一句话总结：划分多个范围相同的区间，每个自区间自排序，最后合并。

桶排序是计数排序的扩展版本，计数排序可以看成每个桶只存储相同元素，而桶排序每个桶存储一定范围的元素，通过映射函数，将待排序数组中的元素映射到各个对应的桶中，对每个桶中的元素进行排序，最后将非空桶中的元素逐个放入原序列中。

桶排序需要尽量保证元素分散均匀，否则当所有数据集中在同一个桶中时，桶排序失效。

## 二、图解过程

![](../../../images/algorithm/sort/bucket-sort-1.png)

## 三、核心代码

```java
public class BucketSort {

    public static void main(String[] args) {
        int[] arr = {6, 9, 1, 4, 5, 8, 7, 0, 2, 3};

        System.out.println("排序前:  ");
        Arrays.stream(arr).forEach(System.out::println);

        new BucketSort().bucketSort(arr);

        System.out.println("排序后:  ");
        Arrays.stream(arr).forEach(System.out::println);
    }


    private void bucketSort(int[] array) {

        if (array == null || array.length < 2) {
            return;
        }

        int min = array[0], max = array[0];
        // 先找出最大最小值
        for (int i = 1; i < array.length; i++) {
            min = Math.min(min, array[i]);
            max = Math.max(max, array[i]);
        }

        int len = array.length;
        // 根据原始序列的长度，设置桶的数量。这里假设每个桶放平均放4个元素
        int bucketCount = len / 4;
        int[][] buckets = new int[bucketCount][];

        // 每个桶的数值范围
        int range = (max - min + 1) / bucketCount;

        // 遍历原始序列
        for (int i = 0; i < len; i++) {
            int val = array[i];
            // 计算当前值属于哪个桶
            int bucketIndex = (int) Math.floor((val - min) / range);
            // 向桶中添加元素
            buckets[bucketIndex] = appendItem(buckets[bucketIndex], val);
        }

        // 最后合并所有的桶
        int k = 0;
        for (int[] b : buckets) {
            if (b != null) {
                for (int i = 0; i < b.length; i++) {
                    array[k++] = b[i];
                }
            }
        }

    }

    private static int[] appendItem(int[] bucketArr, int val) {
        if (bucketArr == null || bucketArr.length == 0) {
            return new int[]{val};
        }
        // 拷贝一下原来桶的序列，并增加一位
        int[] arr = Arrays.copyOf(bucketArr, bucketArr.length + 1);
        // 这里使用插入排序，将新的值val插入到序列中
        int i;
        for (i = bucketArr.length - 1; i >= 0; i--) {
            // 从新序列arr的倒数第二位开始向前遍历（倒数第一位是新增加的空位，还没有值）
            // 如果当前序列值大于val，那么向后移位
            if (arr[i] > val) {
                arr[i + 1] = arr[i];
            } else {
                break;
            }
        }
        arr[i + 1] = val;
        return arr;
    }

}
```

## 四、复杂度分析

### 4.1、时间复杂度：O(N + C)

对于待排序序列大小为 N，共分为 M 个桶，主要步骤有：

* N 次循环，将每个元素装入对应的桶中
* M 次循环，对每个桶中的数据进行排序（平均每个桶有 N/M 个元素）

一般使用较为快速的排序算法，时间复杂度为 O(NlogN)，实际的桶排序过程是以链表形式插入的。

整个桶排序的时间复杂度为：

O(N)+O(M∗(N/M∗log(N/M)))=O(N∗(log(N/M)+1))

当 N = M 时，复杂度为 O(N)

### 4.2、额外空间复杂度：

O(N + M)

## 五、稳定性分析

桶排序的稳定性取决于桶内排序使用的算法。