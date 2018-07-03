#include<stdio.h>
#include<memory>
#include<math.h>
#include<algorithm>
#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>


using namespace std;

#define sqr(x) ((x)*(x))
double esp = 2;
int m = 4;

struct pt
{
	int x[3];
};

pt *tmp; //未排序的数据数组
pt *point; // 将temp按照x[0]元素大小排序后的数据数组
int *tag; // 每个数据所属簇id
int *list;
int *inside;

int *index;
int *reindex; // 排序后point对应的原下标
int *pre;
int *Next;
int n;

double dis(int x, int y)
{
	return sqrt(sqr(point[x].x[0] - point[y].x[0]) + sqr(point[x].x[1] - point[y].x[1]) + sqr(point[x].x[2] - point[y].x[2]));
}

int get_neighbor(int x)
{
	int cnt = 0; int idx = list[0];
	// 这里利用了前面的 重点1 处的代码，直接从可能是邻居点的位置开始遍历，到自身的位置结束
	for (int i = pre[x]; i <= x; ++i)
		if (dis(i, x) <= esp)
		{
			if (inside[i] == 0)
				list[idx++] = i;
			cnt++;
		}
	// 再反向从自身的位置开始，可能是邻居点的位置结束
	for (int i = x + 1; i <= Next[x]; ++i)
		if (dis(i, x) <= esp)
		{
			if (inside[i] == 0)
				list[idx++] = i;
			cnt++;
		}
	// 上面一正一反，就让每次遍历都压缩了绝大部分不可能是邻居点的点，提升了效率
	if (cnt >= m)
	{
		for (int i = list[0]; i<idx; ++i) inside[list[i]] = 1;
		list[0] = idx;
	}
	return cnt;
}

bool compare(int a, int b)
{
	return tmp[a].x[0]<tmp[b].x[0];
}

int main()
{
	freopen("data.txt", "r", stdin);
	freopen("ans.txt", "w", stdout);

	scanf("%d", &n);
	index = new int[n];
	tag = new int[n]();
	tmp = new pt[n];
	point = new pt[n];
	reindex = new int[n];
	pre = new int[n];
	Next = new int[n];
	list = new int[n]();
	inside = new int[n]();
	for (int i = 0; i<n; ++i)
	{
		scanf("%d %d %d", &tmp[i].x[0], &tmp[i].x[1], &tmp[i].x[2]);
		index[i] = i;
	}
	/* 将数组的下标按照tem[i].x[0]排序从小到大排序 */
	sort(index, index + n, compare);
	//sortByX(index, n);

	/* 按照新下标将数组从小到大赋值给point数组，得到一个顺序的数组，并记录数组中每个数据的原始下标 */
	for (int i = 0; i<n; ++i)
	{
		point[i] = tmp[index[i]];
		reindex[index[i]] = i;
	}

	/* 重点1：找到某个点最远可能是邻居的点(再远就不可能是邻居了)
	比如下标10000的这个点， 离下标62的这个点x轴距离为eps，显然61之前的所有点都不会成为10000这个点的邻居
	这样在找邻居点时就可以从下标62这个点开始，避免无谓的遍历，提升速度*/
	int flag = 0;
	for (int i = 0; i<n; ++i)
	{
		while (point[flag].x[0] + esp<point[i].x[0]) flag++;
		pre[i] = flag;
	}
	flag = n - 1;
	int maxlen = 0;
	for (int i = n - 1; i >= 0; --i)
	{
		while (point[flag].x[0] - esp>point[i].x[0]) flag--;
		Next[i] = flag;
		if (Next[i] - pre[i]>maxlen) maxlen = Next[i] - pre[i];
	}
	std::cerr << "flag_maxlen" + maxlen << "\n";
	int x = 0;
	int cor = 0;
	for (x = 0; x<n; ++x)
	{
		list[0] = 1;
		if (tag[x] == 0)
		{
			int size = get_neighbor(x);
			if (size<m) tag[x] = -1;
			else
			{
				cor++;
				tag[x] = cor;
				std::cerr << cor << " " << x << "\n";
				for (int i = 1; i<list[0]; ++i)
				{
					if (tag[list[i]] == -1)
						tag[list[i]] = cor;
					if (tag[list[i]] == 0)
					{
						tag[list[i]] = cor;
						get_neighbor(list[i]);
					}
				}
			}
		}
	}
	for (int i = 0; i<n; ++i)
		printf("%d %d %d %d\n", point[reindex[i]].x[0], point[reindex[i]].x[1], point[reindex[i]].x[2], tag[reindex[i]]);

	delete[]index;
	delete[]tag;
	delete[]tmp;
	delete[]point;
	delete[]reindex;
	delete[]pre;
	delete[]Next;
	delete[]list;
	delete[]inside;
	return 0;
}
