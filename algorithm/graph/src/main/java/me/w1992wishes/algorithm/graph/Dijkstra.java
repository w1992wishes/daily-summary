package me.w1992wishes.algorithm.graph;

/**
 * 迪科斯彻算法
 * 基于广度优先搜索，使用优先队列存储检索信息
 * 伪代码：
 * 1  function Dijkstra(G, w, s)
 * 2     for each vertex v in V[G]    // 初始化
 * 3           d[v] := infinity       // 將各點的已知最短距離先設成無窮大
 * 4           previous[v] := undefined  // 各点的已知最短路径上的前趋都未知
 * // 因为出发点到出发点间不需移动任何距离，所以可以直接将s到s的最小距离设为0
 * 5     d[s] := 0
 * 6     S := empty set
 * 7     Q := set of all vertices
 * 8     while Q is not an empty set     // Dijkstra演算法主體
 * 9           u := Extract_Min(Q)
 * 10           S.append(u)
 * 11           for each edge outgoing from u as (u,v)
 * 12                  if d[v] > d[u] + w(u,v)    // 拓展边（u,v）。w(u,v)为从u到v的路径长度。
 * 13                        d[v] := d[u] + w(u,v)// 更新路径长度到更小的那个和值。
 * 14                        previous[v] := u     // 紀錄前趨頂點
 *
 * @author wly
 */
public class Dijkstra {

    private static int IMAX = 10000; //不连通状态
    public static int[][] adjMat = {
            {0, 10, 3, IMAX},
            {IMAX, 0, 4, 5},
            {IMAX, 4, 0, 10},
            {IMAX, IMAX, IMAX, 0}
    };

    public static void main(String[] args) {
        Dijkstra dijkstra = new Dijkstra();
        int start = 2;
        int end = 3;
        System.out.println("------测试------");
        System.out.println("\n从" + start + "到" + end
                + "的距离是:" + dijkstra.resolve(adjMat, start, end));

        System.out.println("------测试------");
        start = 0;
        end = 3;
        System.out.println("\n从" + start + "到" + end
                + "的距离是:" + dijkstra.resolve(adjMat, start, end));
    }

    /**
     * 在用邻接矩阵adjMat表示的图中，求解从点s到点t的最短路径
     *
     * @param adjMat 邻接矩阵
     * @param s      起点
     * @param t      终点
     */
    public int resolve(int[][] adjMat, int s, int t) {
        //判断参数是否正确
        if (s < 0 || t < 0 || s >= adjMat.length || t >= adjMat.length) {
            System.out.println("错误，顶点不在图中!");
            return IMAX;
        }

        //用来记录某个顶点是否已经完成遍历，即替代优先队列的"移出队列"动作
        boolean[] isVisited = new boolean[adjMat.length];
        //用于存储从s到各个点之间的最短路径长度
        int[] d = new int[adjMat.length];

        //初始化数据
        for (int i = 0; i < adjMat.length; i++) {
            isVisited[i] = false;
            d[i] = IMAX;
        }
        d[s] = 0; //s到s的距离是0
        isVisited[s] = true; //将s标记为已访问过的

        //尚未遍历的顶点数目，替代优先队列是否为空的判断即Queue.isEmpty()
        int unVisitedNum = adjMat.length;
        //用于表示当前所保存的子路径中权重最小的顶点的索引,对应优先队列中,默认是起点
        int index = s;
        while (unVisitedNum > 0 && index != t) {
            int min = IMAX;
            for (int i = 0; i < adjMat.length; i++) { //取到第i行的最小元素的索引
                if (min > d[i] && !isVisited[i]) {
                    min = d[i];
                    index = i;
                }
            }
            if (index == t || unVisitedNum == 0) {
                System.out.print(index); //打印最短路径
            } else {
                System.out.print(index + "=>"); //打印最短路径
            }
            for (int i = 0; i < adjMat.length; i++) {
                if (d[index] + adjMat[index][i] < d[i]) {
                    d[i] = d[index] + adjMat[index][i];
                }
            }
            unVisitedNum--;
            isVisited[index] = true;
        }
        return d[t];
    }
}
