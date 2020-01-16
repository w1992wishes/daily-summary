package me.w1992wishes.algorithm.search;

/**
 * 插值查找(Interpolation Search)是根据要查找的关键字 key 与查找表中最大最小记录的关键字比较后的查找方法，其核心就在于插值的计算公式(key - a[low])/(a[high] - a[low])。
 *
 * mid=low+(high-low)*(key-a[low])/(a[high]-a[low]) //(1/2)换为(key-a[low])/(a[high]-a[low])
 *
 * 也就是将二分法的比例参数 1/2 改进为自适应的，根据关键字在整个有序表中所处的位置，让mid值的变化更靠近关键字key，这样也就间接地减少了比较次数。
 *
 * @author w1992wishes 2020/1/16 10:27
 */
public class InterpolationSearch {

    public static void main(String[] args) {
        int[] arr = new int[]{1, 2, 3, 4, 5, 5, 6, 6, 7};
        InterpolationSearch search = new InterpolationSearch();
        System.out.println(search.interpolationSearch(arr, 3));
        System.out.println(search.interpolationSearch(arr, 5));
    }

    private int interpolationSearch(int[] arr, int key) {
        if (arr == null) {
            return -1;
        }

        int low = 0;
        int high = arr.length - 1;
        while (low <= high) {
            int mid = low + (high - low) * (key - arr[low]) / (arr[high] - arr[low]);
            if (key > arr[mid]) {
                low = mid + 1;
            } else if (key < arr[mid]) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

}
