#include "intellif_minning_dbscan_impl_DBSCANImpl.h"
#include<stdio.h>
#include<memory>
#include<math.h>
#include<algorithm>
#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>
#include <ctime>

using namespace std;

#define sqr(x) ((x)*(x))
float eps;
int m;

struct pt
{
	float x[128];
	string img;
};

pt *tmp; //未排序的数据数组
pt *point; // 将temp按照x[0]元素大小排序后的数据数组
int *tag; // 每个数据所属簇id
int *list;
int *inside;

int *sub;
int *resub; // 排序后point对应的原下标
int *pre;
int *Next;
int n;

// 计算欧氏距离
float dis(int x, int y)
{
	float temp = 0.0f;
	for (int i = 0; i < 128; i++)
	{
		temp += sqr(point[x].x[i] - point[y].x[i]);

	}
	return sqrt(temp);
}

// 返回邻居点个数
int get_neighbor(int x)
{
	int cnt = 0; int idx = list[0];
	// 这里利用了前面的 重点1 处的代码，直接从可能是邻居点的位置开始遍历，到自身的位置结束
	for (int i = pre[x]; i <= x; ++i)
		if (dis(i, x) <= eps)
		{
			if (inside[i] == 0)
				list[idx++] = i;
			cnt++;
		}
	// 再反向从自身的位置开始，可能是邻居点的位置结束
	for (int i = x + 1; i <= Next[x]; ++i)
		if (dis(i, x) <= eps)
		{
			if (inside[i] == 0)
				list[idx++] = i;
			cnt++;
		}
	// 上面一正一反，就让每次遍历都压缩了绝大部分不可能是邻居点的点，提升了效率
	if (cnt >= m)
	{
		for (int i = list[0]; i < idx; ++i) inside[list[i]] = 1;
		list[0] = idx;
	}
	return cnt;
}

bool compare(int a, int b)
{
	return tmp[a].x[0] < tmp[b].x[0];
}

//删除字符串中空格，制表符tab等无效字符
string Trim(string& str)
{
	//str.find_first_not_of(" \t\r\n"),在字符串str中从索引0开始，返回首次不匹配"\t\r\n"的位置
	str.erase(0, str.find_first_not_of(" \t\r\n"));
	str.erase(str.find_last_not_of(" \t\r\n") + 1);
	return str;
}

// 动态分配数组
void allocaArrays(int n) {
	sub = new int[n];
	tag = new int[n]();
	tmp = new pt[n];
	point = new pt[n];
	resub = new int[n];
	pre = new int[n];
	Next = new int[n];
	list = new int[n]();
	inside = new int[n]();
}

// 释放数组内存
void freeArrays() {
	delete[]sub;
	delete[]tag;
	delete[]tmp;
	delete[]point;
	delete[]resub;
	delete[]pre;
	delete[]Next;
	delete[]list;
	delete[]inside;
	sub = NULL;
	tag = NULL;
	tmp = NULL;
	point = NULL;
	resub = NULL;
	pre = NULL;
	Next = NULL;
	list = NULL;
	inside = NULL;
}

// 预处理，减少后续遍历
void preHandle() {
	/* 重点1：找到某个点最远可能是邻居的点(再远就不可能是邻居了)
	比如下标10000的这个点， 离下标62的这个点x轴距离为eps，显然61之前的所有点都不会成为10000这个点的邻居
	这样在找邻居点时就可以从下标62这个点开始，避免无谓的遍历，提升速度*/
	int flag = 0;
	for (int i = 0; i < n; ++i)
	{
		while (point[flag].x[0] + eps < point[i].x[0]) flag++;
		pre[i] = flag;
	}
	flag = n - 1;
	for (int i = n - 1; i >= 0; --i)
	{
		while (point[flag].x[0] - eps > point[i].x[0]) flag--;
		Next[i] = flag;
	}
}

// 聚类
void cluster() {
	int x = 0;
	int cor = 0;
	for (x = 0; x < n; ++x)
	{
		list[0] = 1;
		if (tag[x] == 0)
		{
			int size = get_neighbor(x);
			if (size < m) tag[x] = -1;
			else
			{
				cor++;
				tag[x] = cor;
				for (int i = 1; i < list[0]; ++i)
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
}

JNIEXPORT jboolean JNICALL Java_intellif_minning_dbscan_impl_DBSCANImpl_initDatasFromFile
(JNIEnv *env, jobject obj, jstring jfile, jint jcount) {
	const char *file = env->GetStringUTFChars(jfile, NULL);// 从java 得到file
	n = (int)jcount;// 从java 得到 count

	cout << "before allocate array" << endl;

	try
	{
		allocaArrays(n);// 分配数组空间
	}
	catch (const std::exception& e)
	{
		cout << "alloca arrays exception: " << e.what() << endl;
	}

	cout << "before read file" << endl;

	ifstream fin(file); //打开文件流操作
	if (!fin)
	{
		cout << "file not found" << endl;
		return (jboolean)false;
	}
	string line;
	int point_count = 0;
	while (getline(fin, line))   //整行读取，换行符“\n”区分，遇到文件尾标志eof终止读取
	{
		istringstream sin(line); //将整行字符串line读入到字符串流istringstream中
		vector<string> fields; //声明一个字符串向量
		string field;
		while (getline(sin, field, ',')) //将字符串流sin中的字符读入到field字符串中，以逗号为分隔符
		{
			fields.push_back(field); //将刚刚读取的字符串添加到向量fields中
		}
		string alls = Trim(fields[0]); // 文件中每行都是一个字符串

		size_t pos = alls.find(" ");
		string features = alls.substr(pos + 1);// 特征值转换，并初始化temp，此时的temp顺序是按照文件中读取的顺序
		istringstream featurestream(features);
		string feature;
		int dims = 0;
		while (getline(featurestream, feature, ' '))
		{
			tmp[point_count].x[dims++] = stof(feature);
		}
		//tmp[point_count].img = Trim(fields[1]); //清除掉向量fields中第二个元素的无效字符，并赋值给变量img
												// 初始化sub，数值为文件读取的顺序id，1，2，3，4...这样递增
		sub[point_count] = point_count;
		point_count++;
		if (point_count >= n)
		{
			break;
		}
	}

	cout << "before sort sub" << endl;

	/* 将数组的下标按照tem[i].x[0]排序从小到大排序，排序后sub数值为 temp[i].x[0]的值从小到大的id */
	sort(sub, sub + n, compare);

	/* 按照新下标将数组从小到大赋值给point数组，得到一个顺序的数组，并记录数组中每个数据的原始下标 */
	for (int i = 0; i < n; ++i)
	{
		point[i] = tmp[sub[i]];
		resub[sub[i]] = i;
	}

	env->ReleaseStringUTFChars(jfile, file);

	cout << "init points from file success" << endl;

	return (jboolean)true;
}

// dbscan
JNIEXPORT void JNICALL Java_intellif_minning_dbscan_impl_DBSCANImpl_runDBSCAN
(JNIEnv *env, jobject obj, jfloat jeps, jint jminPts) {
	eps = (float)jeps;
	m = (int)jminPts;

	clock_t start, finish;
	start = clock();

	cout << "before preHandle" << endl;

	// 1.预处理，减少后续寻找邻居点的遍历次数
	preHandle();

	cout << "before cluster" << endl;

	// 2.聚类
	cluster();

	finish = clock();

	cout << "dbscan success" << endl;

	cout << n << " speed time: " << (finish - start)*1.0 / CLOCKS_PER_SEC << "s\n" << endl;
}

JNIEXPORT jstring JNICALL Java_intellif_minning_dbscan_impl_DBSCANImpl_saveDBSCAN
(JNIEnv *env, jobject obj) {

	ofstream fout;
	char resultFile[128];
	sprintf_s(resultFile, "%d_result.csv", n);
	fout.open(resultFile);
	for (int i = 0; i < n; i++) {
		fout << i << "," << tag[resub[i]] << endl;
	}
	fout.close();

	// 释放内存
	freeArrays();

	cout << "save result success" << endl;

	return env->NewStringUTF(resultFile);
}