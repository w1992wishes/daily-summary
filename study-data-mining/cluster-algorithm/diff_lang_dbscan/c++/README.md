# DBSCAN聚类实现之c++

---

本篇结构：

 - 简介
 - 直接以可运行程序跑
 - JNI调用
 - 可能遇到的错误

## 一、简介

这一篇主要是尝试DBSCAN的C++实现，是CPU版的常规实现。

## 二、直接以可运行程序跑

```
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

int *index;
int *reindex; // 排序后point对应的原下标
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
	index = new int[n];
	tag = new int[n]();
	tmp = new pt[n];
	point = new pt[n];
	reindex = new int[n];
	pre = new int[n];
	Next = new int[n];
	list = new int[n]();
	inside = new int[n]();
}

// 释放数组内存
void freeArrays() {
	delete[]index;
	delete[]tag;
	delete[]tmp;
	delete[]point;
	delete[]reindex;
	delete[]pre;
	delete[]Next;
	delete[]list;
	delete[]inside;
	index = NULL;
	tag = NULL;
	tmp = NULL;
	point = NULL;
	reindex = NULL;
	pre = NULL;
	Next = NULL;
	list = NULL;
	inside = NULL;
}

// 保存聚类结果
void saveDbscanResults() {
	ofstream fout;
	fout.open("result.html");
	for (int i = 0; i < n; i++) {
		fout << "<img src='" << tmp[i].img << "'/>" << tag[reindex[i]] << endl;
	}
	fout.close();
}

// initPoints
void initPointsFromFile(string inputFileName) {
	ifstream fin(inputFileName); //打开文件流操作
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
		string features = Trim(fields[0]); //清除掉向量fields中第一个元素的无效字符，并赋值给变量features(特征值字符串)

										   // 特征值转换， 并初始化temp，此时的temp顺序是按照文件中读取的顺序
		istringstream featurestream(features);
		string feature;
		int dims = 0;
		while (getline(featurestream, feature, '_'))
		{
			tmp[point_count].x[dims++] = stof(feature);
		}
		tmp[point_count].img = Trim(fields[1]); //清除掉向量fields中第二个元素的无效字符，并赋值给变量img
												// 初始化index，数值为文件读取的顺序id，1，2，3，4...这样递增
		index[point_count] = point_count;
		point_count++;
		if (point_count >= n)
		{
			break;
		}
	}

	/* 将数组的下标按照tem[i].x[0]排序从小到大排序，排序后index数值为 temp[i].x[0]的值从小到大的id */
	sort(index, index + n, compare);

	/* 按照新下标将数组从小到大赋值给point数组，得到一个顺序的数组，并记录数组中每个数据的原始下标 */
	for (int i = 0; i < n; ++i)
	{
		point[i] = tmp[index[i]];
		reindex[index[i]] = i;
	}
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
	int maxlen = 0;
	for (int i = n - 1; i >= 0; --i)
	{
		while (point[flag].x[0] - eps > point[i].x[0]) flag--;
		Next[i] = flag;
		if (Next[i] - pre[i] > maxlen) maxlen = Next[i] - pre[i];
	}
}

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

int main(int argc, const char * argv[])
{
	string inputFileName(argv[1]);//数据源文件
	string nstr(argv[2]);// 数据数
	string epsli(argv[3]); // eps
	string minPts(argv[4]); // minPts

	eps = stof(epsli);
	m = stoi(minPts);

	// 1.动态分配数组
	n = stoi(nstr);
	allocaArrays(n);

	// 2.从文件读取数据，初始化tmp、index等数组
	initPointsFromFile(inputFileName);

	clock_t start, finish;
	start = clock();

	// 3.预处理，减少后续寻找邻居点的遍历次数
	preHandle();

	// 4.聚类
	cluster();

	finish = clock();
	cout << n << " speed time: " << (finish - start)*1.0 / CLOCKS_PER_SEC << "s\n" << endl;

    // 5.保存聚类结果
	saveDbscanResults();

	// 6.释放内存
	freeArrays();
	return 0;
}
```

* 生成可执行文件：
    * g++ -std=c++11 dbscan_c.cpp -o dbscan
* 通过向main方法传入四个参数分别是：
    * file
    * 数据量
    * eps
    * minPts

## 2三、JNI调用

先定义一个接口：

```
public interface DBSCAN {

    boolean initDatasFromFile(String fileName, int dataCounts);

    void runDBSCAN(float eps, int minPts);

    String saveDBSCAN();
}
```

接着实现它：

```
public class DBSCANCppImpl implements DBSCAN {

    static {
        System.loadLibrary("dbscan");
    }

    @Override
    public native boolean initDatasFromFile(String fileName, int dataCounts) ;

    @Override
    public native void runDBSCAN(float eps, int minPts);

    @Override
    public native String saveDBSCAN();
}
```

大体步骤如下：

* 编译DBSCANImpl：javac DBSCANImpl.java
* 生成头文件：javah DBSCANImpl

```
/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class intellif_minning_dbscan_impl_DBSCANImpl */

#ifndef _Included_intellif_minning_dbscan_impl_DBSCANImpl
#define _Included_intellif_minning_dbscan_impl_DBSCANImpl
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     intellif_minning_dbscan_impl_DBSCANImpl
 * Method:    initDatasFromFile
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_intellif_minning_dbscan_impl_DBSCANImpl_initDatasFromFile
  (JNIEnv *, jobject, jstring, jint);

/*
 * Class:     intellif_minning_dbscan_impl_DBSCANImpl
 * Method:    runDBSCAN
 * Signature: (FI)V
 */
JNIEXPORT void JNICALL Java_intellif_minning_dbscan_impl_DBSCANImpl_runDBSCAN
  (JNIEnv *, jobject, jfloat, jint);

/*
 * Class:     intellif_minning_dbscan_impl_DBSCANImpl
 * Method:    saveDBSCAN
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_intellif_minning_dbscan_impl_DBSCANImpl_saveDBSCAN
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif

```
* 编写本地方法c++实现

```c++
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

int *index;
int *reindex; // 排序后point对应的原下标
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
	index = new int[n];
	tag = new int[n]();
	tmp = new pt[n];
	point = new pt[n];
	reindex = new int[n];
	pre = new int[n];
	Next = new int[n];
	list = new int[n]();
	inside = new int[n]();
}

// 释放数组内存
void freeArrays() {
	delete[]index;
	delete[]tag;
	delete[]tmp;
	delete[]point;
	delete[]reindex;
	delete[]pre;
	delete[]Next;
	delete[]list;
	delete[]inside;
	index = NULL;
	tag = NULL;
	tmp = NULL;
	point = NULL;
	reindex = NULL;
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
					// 分配数组空间
	allocaArrays(n);

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
		string features = Trim(fields[0]); //清除掉向量fields中第一个元素的无效字符，并赋值给变量features(特征值字符串)

		istringstream featurestream(features);// 特征值转换， 并初始化temp，此时的temp顺序是按照文件中读取的顺序
		string feature;
		int dims = 0;
		while (getline(featurestream, feature, '_'))
		{
			tmp[point_count].x[dims++] = stof(feature);
		}
		tmp[point_count].img = Trim(fields[1]); //清除掉向量fields中第二个元素的无效字符，并赋值给变量img
												// 初始化index，数值为文件读取的顺序id，1，2，3，4...这样递增
		index[point_count] = point_count;
		point_count++;
		if (point_count >= n)
		{
			break;
		}
	}

	/* 将数组的下标按照tem[i].x[0]排序从小到大排序，排序后index数值为 temp[i].x[0]的值从小到大的id */
	sort(index, index + n, compare);

	/* 按照新下标将数组从小到大赋值给point数组，得到一个顺序的数组，并记录数组中每个数据的原始下标 */
	for (int i = 0; i < n; ++i)
	{
		point[i] = tmp[index[i]];
		reindex[index[i]] = i;
	}

	env->ReleaseStringUTFChars(jfile, file);

	return (jboolean)true;
}

// dbscan
JNIEXPORT void JNICALL Java_intellif_minning_dbscan_impl_DBSCANImpl_runDBSCAN
(JNIEnv *env, jobject obj, jfloat jeps, jint jminPts) {
	eps = (float)jeps;
	m = (int)jminPts;

	clock_t start, finish;
	start = clock();

	// 1.预处理，减少后续寻找邻居点的遍历次数
	preHandle();

	// 2.聚类
	cluster();

	finish = clock();
	cout << n << " speed time: " << (finish - start)*1.0 / CLOCKS_PER_SEC << "s\n" << endl;


}

JNIEXPORT jstring JNICALL Java_intellif_minning_dbscan_impl_DBSCANImpl_saveDBSCAN
(JNIEnv *env, jobject obj) {

	ofstream fout;
	char resultFile[128] = "result.csv";
	fout.open(resultFile);
	for (int i = 0; i < n; i++) {
		fout << i << "," << tag[reindex[i]] << endl;
	}
	fout.close();

	// 释放内存
	freeArrays();

	return env->NewStringUTF(resultFile);
}
```

* 生成动态链接库，命令如下（为什么要用-std=c++11？参考可能遇到的错误1）：
g++ -std=c++11 -fPIC -I `$JAVA_HOME`/include -I `$JAVA_HOME`/include/linux -shared -o libdbscan.so dbscan.cpp
* 指定-Djava.library.path=“动态库目录”运行测试。
eg: java -Djava.library.path=lib intellif.minning.dbscan.DBSCANImplTest 10000.csv 10000 0.87 1

## 四、可能遇到的错误

### 4.1、错误1

可以利用VS生成动态文件库，也可以在linux下生成动态链接库：

g++ -fPIC -I `$JAVA_HOME`/include -I `$JAVA_HOME`/include/linux -shared -o libdbscan.so dbscan.cpp。

编译时可能会出现：

‘stof’ was not declared in this scope。

参考解决如下：https://stackoverflow.com/questions/19879506/stof-function-is-not-declared-in-this-scope-c

最后得到命令如下：

**g++ -std=c++11 -fPIC -I `$JAVA_HOME`/include -I `$JAVA_HOME`/include/linux -shared -o libdbscan.so dbscan.cpp**

### 4.2、错误2

编译成动态库后，用java调用，在windows中发现一切正常，在linux上发现一直报错，A fatal error has been detected by the Java Runtime Environment:....

why?

一步步打日志，耗费了很多才发现是分配数组空间时，index = new int[n]报错，又一通乱操作，发现把index换个名字就不报错了，换成sub。
