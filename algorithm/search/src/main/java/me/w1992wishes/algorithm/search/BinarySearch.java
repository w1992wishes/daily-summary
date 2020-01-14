package me.w1992wishes.algorithm.search;

/**
 * @author w1992wishes 2020/1/14 16:18
 */
public class BinarySearch {

    private int binarySearch(int[] array, int value) {
        int low = 0;
        int high = array.length - 1;

        while (low <= high) {

            int mid = (low + high) >>> 1;

            if (array[mid] < value) {
                low = mid + 1;
            } else if (array[mid] > value) {
                high = mid - 1;
            } else {
                return mid;
            }

        }
        return -1;
    }

    /**
     * 查找第一个等于指定值的元素索引
     *
     * 原二分查找在找到一个元素后，检查与上一个元素是否相等
     * 如果不相等，当前元素就是第一个
     * 如果相等，继续往前找，mid - 1
     *
     * @param array array
     * @param value value
     * @return index
     */
    private int binarySearchFirst(int[] array, int value) {
        int low = 0;
        int high = array.length - 1;

        while (low <= high) {

            int mid = (low + high) >>> 1;

            if (array[mid] < value) {
                low = mid + 1;
            } else if (array[mid] > value) {
                high = mid - 1;
            } else {
                /*while (mid > 0 && array[mid - 1] == value) {
                    mid = mid - 1;
                }
                return mid;*/

                if (mid == 0 || array[mid - 1] != value) {
                    return mid;
                } else {
                    // 如果前面还有相等的就继续二分查找
                    high = mid - 1;
                }
            }

        }
        return -1;
    }

    /**
     * 查找最后一个等于指定值的元素索引
     *
     * @param array array
     * @param value value
     * @return index
     */
    private int binarySearchLast(int[] array, int value) {
        int low = 0;
        int high = array.length - 1;

        while (low <= high) {

            int mid = (low + high) >>> 1;

            if (array[mid] < value) {
                low = mid + 1;
            } else if (array[mid] > value) {
                high = mid - 1;
            } else {
               /* while (mid < array.length - 1 && array[mid + 1] == value) {
                    mid = mid + 1;
                }
                return mid;*/
                if (mid == array.length - 1 || array[mid + 1] != value) {
                    return mid;
                } else {
                    // 如果前面还有相等的就继续二分查找
                    low = mid + 1;
                }
            }
        }
        return -1;
    }

    /**
     * 查找最后一个小于等于给定值的元素索引
     *
     * @param array array
     * @param value value
     * @return index
     */
    private int binarySearchLastLess(int[] array, int value) {
        int low = 0;
        int high = array.length - 1;

        while (low <= high) {

            int mid = (low + high) >>> 1;
            if (array[mid] <= value) {
                if (mid == array.length - 1 || array[mid + 1] > value) {
                    return mid;
                } else {
                    low = mid + 1;
                }
            } else {
                high = mid - 1;
            }

        }
        return -1;
    }

    /**
     * 查找第一个大于等于指定值的元素索引
     *
     * @param array array
     * @param value value
     * @return index
     */
    public int binarySearchFistMore(int[] array, int value) {
        int low = 0;
        int high = array.length - 1;

        while (low <= high) {

            int mid = low + ((high - low) >> 1);

            if (array[mid] >= value) {
                if (mid == 0 || array[mid - 1] < value) {
                    return mid;
                } else {
                    high = mid - 1;
                }
            } else {
                low = mid + 1;
            }
        }
        return -1;
    }

}
