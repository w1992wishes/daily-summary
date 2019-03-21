# DBSCAN聚类实现之OpenCL

本篇结构：

 - 简介
 - 直接以可运行程序跑
 - JNI调用
 - 可能遇到的错误

## 一、简介

为了提升聚类的效率，对于计算量大的情况，如今普遍采用GPU加速，这里使用GPU编程规范OpenCL实现。

## 二、以可运行程序跑

```
#include <stdlib.h>
#include <CL/cl.h>
#include<stdio.h>
#include<string.h>
#include<math.h>
#include<iostream>
#include<algorithm>
#include<memory>
#include <fstream>
#include <sstream>
#include <vector>
#include <ctime>

using namespace std;

#define MAX_SOURCE_SIZE (0x1000000)

#define sqr(x) ((x)*(x))
float eps;
int m;

struct pt
{
	float x[128];
};

string *imgs;//图片地址

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

int *para;
bool *core;



int getProgramBuildInfo(cl_program program, cl_device_id device)
{
	size_t log_size;
	char *program_log;
	/* Find size of log and print to std output */
	clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG,
		0, NULL, &log_size);
	program_log = (char*)malloc(log_size + 1);
	program_log[log_size] = '\0';
	clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG,
		log_size + 1, program_log, NULL);
	printf("%s\n", program_log);
	free(program_log);
	return 0;
}

bool compare(int a, int b)
{
	return tmp[a].x[0] < tmp[b].x[0];
}

cl_device_id device_id = NULL;
cl_context context = NULL;
cl_command_queue command_queue = NULL;
cl_mem memobj_point = NULL;
cl_mem memobj_pre = NULL;
cl_mem memobj_next = NULL;
cl_mem memobj_cor = NULL;
cl_mem memobj_core = NULL;
cl_mem memobj_para = NULL;
cl_mem memobj_tags = NULL;
cl_mem memobj_eps = NULL;

cl_program program = NULL;
cl_kernel kernel_core, kernel_bfs, kernel_set = NULL;
cl_platform_id platform_id = NULL;
cl_int ret;

void releaseOpenCL() {
	ret = clReleaseKernel(kernel_core);
	ret = clReleaseKernel(kernel_bfs);
	ret = clReleaseKernel(kernel_set);
	ret = clReleaseProgram(program);
	ret = clReleaseMemObject(memobj_point);
	ret = clReleaseMemObject(memobj_pre);
	ret = clReleaseMemObject(memobj_next);
	ret = clReleaseMemObject(memobj_cor);
	ret = clReleaseMemObject(memobj_core);
	ret = clReleaseMemObject(memobj_para);
	ret = clReleaseMemObject(memobj_tags);
	ret = clReleaseMemObject(memobj_eps);
	ret = clReleaseCommandQueue(command_queue);
	ret = clReleaseContext(context);
	ret = clReleaseDevice(device_id);
}

void init()
{
	FILE *fp;
	char fileName[] = "./dbscan.cl";
	char *source_str;
	size_t source_size;

	/* Load the source code containing the kernel*/
	fp = fopen(fileName, "r");
	if (!fp) {
		fprintf(stderr, "Failed to load kernel.\n");
		exit(1);
	}
	source_str = (char*)malloc(MAX_SOURCE_SIZE);
	source_size = fread(source_str, 1, MAX_SOURCE_SIZE, fp);
	fclose(fp);

	/* Get Platform and Device Info */
	cl_uint ret_num_platforms;
	cl_uint ret_num_devices;
	ret = clGetPlatformIDs(1, &platform_id, &ret_num_platforms);
	ret = clGetDeviceIDs(platform_id, CL_DEVICE_TYPE_DEFAULT, 1, &device_id,
		&ret_num_devices);

	/* Create OpenCL context */
	context = clCreateContext(NULL, 1, &device_id, NULL, NULL, &ret);

	/* Create Command Queue */
	command_queue = clCreateCommandQueue(context, device_id, CL_QUEUE_PROFILING_ENABLE, &ret);

	/* Create Memory Buffer */
	memobj_point = clCreateBuffer(context, CL_MEM_READ_WRITE, (n) * sizeof(pt),
		NULL, &ret);
	memobj_pre = clCreateBuffer(context, CL_MEM_READ_WRITE, (n) * sizeof(int),
		NULL, &ret);
	memobj_next = clCreateBuffer(context, CL_MEM_READ_WRITE, n * sizeof(int),
		NULL, &ret);
	memobj_tags = clCreateBuffer(context, CL_MEM_READ_WRITE, n * sizeof(int),
		NULL, &ret);
	memobj_core = clCreateBuffer(context, CL_MEM_READ_WRITE, n * sizeof(bool),
		NULL, &ret);
	memobj_cor = clCreateBuffer(context, CL_MEM_READ_WRITE, n * sizeof(int),
		NULL, &ret);
	memobj_para = clCreateBuffer(context, CL_MEM_READ_WRITE, 10 * sizeof(int),
		NULL, &ret);
	memobj_eps = clCreateBuffer(context, CL_MEM_READ_WRITE, sizeof(float),
		NULL, &ret);

	ret = clEnqueueWriteBuffer(command_queue, memobj_core, CL_TRUE, 0,
		n * sizeof(bool), core, 0, NULL, NULL);

	ret = clEnqueueWriteBuffer(command_queue, memobj_point, CL_TRUE, 0,
		n * sizeof(pt), point, 0, NULL, NULL);

	ret = clEnqueueWriteBuffer(command_queue, memobj_pre, CL_TRUE, 0,
		n * sizeof(int), pre, 0, NULL, NULL);

	ret = clEnqueueWriteBuffer(command_queue, memobj_next, CL_TRUE, 0,
		n * sizeof(int), Next, 0, NULL, NULL);

	ret = clEnqueueWriteBuffer(command_queue, memobj_tags, CL_TRUE, 0,
		n * sizeof(int), tag, 0, NULL, NULL);

	ret = clEnqueueWriteBuffer(command_queue, memobj_cor, CL_TRUE, 0,
		n * sizeof(int), tag, 0, NULL, NULL);

	float *m_eps = new float(eps);
	ret = clEnqueueWriteBuffer(command_queue, memobj_eps, CL_TRUE, 0,
		sizeof(float), m_eps, 0, NULL, NULL);
	free(m_eps);

	para = (int*)malloc(sizeof(int) * 10);
	para[0] = n; para[1] = 4; para[2] = m; para[7] = 0;

	ret = clEnqueueWriteBuffer(command_queue, memobj_para, CL_TRUE, 0,
		10 * sizeof(int), para, 0, NULL, NULL);

	/* Create Kernel Program from the source */
	program = clCreateProgramWithSource(context, 1, (const char **)&source_str,
		(const size_t *)&source_size, &ret);

	/* Build Kernel Program */
	ret = clBuildProgram(program, 1, &device_id, NULL, NULL, NULL);

	getProgramBuildInfo(program, device_id);

	/* Create OpenCL Kernel */
	kernel_core = clCreateKernel(program, "core", &ret);
	kernel_bfs = clCreateKernel(program, "bfs", &ret);
	kernel_set = clCreateKernel(program, "set", &ret);
	/* Set OpenCL Kernel Parameters */
	ret = clSetKernelArg(kernel_bfs, 0, sizeof(cl_mem), (void *)&memobj_point);
	ret = clSetKernelArg(kernel_bfs, 1, sizeof(cl_mem), (void *)&memobj_pre);
	ret = clSetKernelArg(kernel_bfs, 2, sizeof(cl_mem), (void *)&memobj_next);
	ret = clSetKernelArg(kernel_bfs, 3, sizeof(cl_mem), (void *)&memobj_tags);
	ret = clSetKernelArg(kernel_bfs, 4, sizeof(cl_mem), (void *)&memobj_core);
	ret = clSetKernelArg(kernel_bfs, 5, sizeof(cl_mem), (void *)&memobj_cor);
	ret = clSetKernelArg(kernel_bfs, 6, sizeof(cl_mem), (void *)&memobj_para);
	ret = clSetKernelArg(kernel_bfs, 7, sizeof(cl_mem), (void *)&memobj_eps);

	ret = clSetKernelArg(kernel_core, 0, sizeof(cl_mem), (void *)&memobj_point);
	ret = clSetKernelArg(kernel_core, 1, sizeof(cl_mem), (void *)&memobj_pre);
	ret = clSetKernelArg(kernel_core, 2, sizeof(cl_mem), (void *)&memobj_next);
	ret = clSetKernelArg(kernel_core, 3, sizeof(cl_mem), (void *)&memobj_core);
	ret = clSetKernelArg(kernel_core, 4, sizeof(cl_mem), (void *)&memobj_para);
	ret = clSetKernelArg(kernel_core, 5, sizeof(cl_mem), (void *)&memobj_eps);

	ret = clSetKernelArg(kernel_set, 0, sizeof(cl_mem), (void *)&memobj_point);
	ret = clSetKernelArg(kernel_set, 1, sizeof(cl_mem), (void *)&memobj_para);
	ret = clSetKernelArg(kernel_set, 2, sizeof(cl_mem), (void *)&memobj_tags);
	ret = clSetKernelArg(kernel_set, 3, sizeof(cl_mem), (void *)&memobj_core);
}

void para_update()
{
	ret = clEnqueueWriteBuffer(command_queue, memobj_para, CL_TRUE, 0,
		10 * sizeof(int), para, 0, NULL, NULL);
}

void para_download()
{
	ret = clEnqueueReadBuffer(command_queue, memobj_para, CL_TRUE, 0,
		10 * sizeof(int), para, 0, NULL, NULL);
}

/* 动态分配数组空间 */
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
	imgs = new string[n];
	core = new bool[n]();
}

/* 释放数组空间 */
void freeArray() {
	delete[]sub;
	delete[]tag;
	delete[]tmp;
	delete[]point;
	delete[]resub;
	delete[]pre;
	delete[]Next;
	delete[]list;
	delete[]inside;
	delete[]imgs;
	delete[]core;
	sub = NULL;
	tag = NULL;
	tmp = NULL;
	point = NULL;
	resub = NULL;
	pre = NULL;
	Next = NULL;
	list = NULL;
	inside = NULL;
	imgs = NULL;
	core = NULL;
}

/* 删除字符串中空格，制表符tab等无效字符 */
string Trim(string& str)
{
	//str.find_first_not_of(" \t\r\n"),在字符串str中从索引0开始，返回首次不匹配"\t\r\n"的位置
	str.erase(0, str.find_first_not_of(" \t\r\n"));
	str.erase(str.find_last_not_of(" \t\r\n") + 1);
	return str;
}

/* 从文件读取数据源对tmp等数组初始化 */
void readFile(string file) {
	ifstream fin(file); //打开文件流操作
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
		imgs[point_count] = Trim(fields[1]); //清除掉向量fields中第二个元素的无效字符，并赋值给变量img

		sub[point_count] = point_count; // 初始化sub，数值为文件读取的顺序id，1，2，3，4...这样递增
		point_count++;
		if (point_count >= n)
		{
			break;
		}
	}

	/* 将数组的下标按照tem[i].x[0]排序从小到大排序，排序后sub数值为 temp[i].x[0]的值从小到大的id */
	sort(sub, sub + n, compare);

	/* 按照新下标将数组从小到大赋值给point数组，得到一个顺序的数组，并记录数组中每个数据的原始下标 */
	for (int i = 0; i < n; ++i)
	{
		point[i] = tmp[sub[i]];
		resub[sub[i]] = i;
	}
}

void preExecute() {
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

// 保存聚类结果
void saveDbscanResults() {
	ofstream fout;
	fout.open("result.html");
	for (int i = 0; i < n; i++) {
		fout << "<img src='" << imgs[i] << "'/>" << tag[resub[i]] << endl;
	}
	fout.close();
}

int main(int argc, const char * argv[])
{
	string inputFileName(argv[1]);//数据源文件
	string nstr(argv[2]);
	string epsli(argv[3]);
	string minPts(argv[4]);

	n = stoi(nstr); // 从用户输入得到数据点个数
	eps = stof(epsli); // eps
	m = stoi(minPts); // minPts

					  // 1.分配数组空间
	allocaArrays(n);

	// 2.从csv文件读取数据初始化
	readFile(inputFileName);

	clock_t start, finish;
	start = clock();

	// 3.对源数据进行预处理，为后续寻找邻居点减少遍历时间
	preExecute();

	// 4.初始化opencl
	init();

	/*paremeter for OpenCL Kernel */
	size_t *global, *local;
	global = (size_t*)malloc(sizeof(size_t) * 5);
	local = (size_t*)malloc(sizeof(size_t) * 5);
	global[0] = 2000;
	local[0] = 0;

	int bfs_tag = 1;
	int corcnt = 0;
	para[6] = bfs_tag;
	para_update();
	ret = clEnqueueNDRangeKernel(command_queue, kernel_core, 1, 0, global, NULL,
		0, 0, NULL);
	clFinish(command_queue);
	ret = clEnqueueTask(command_queue, kernel_set, 0, NULL,
		NULL);
	clFinish(command_queue);
	para_download();
	//std::cerr << idex[para[8]] << " as init\n";
	while (para[4] == 1)
	{
		para[3] = ++corcnt;
		while (para[4] == 1)
		{
			para[5] = bfs_tag++;
			para[6] = bfs_tag;
			para[4] = 0;
			para_update();
			global[0] = 8192 * 2;
			local[0] = 32;
			ret = clEnqueueNDRangeKernel(command_queue, kernel_bfs, 1, 0, global, local,
				0, 0, NULL);
			clFinish(command_queue);
			para_download();
		}
		para[5] = bfs_tag++;
		para[6] = bfs_tag;
		para_update();
		ret = clEnqueueTask(command_queue, kernel_set, 0, NULL,
			NULL);
		clFinish(command_queue);
		para_download();
	}
	//	ret = clEnqueueTask(command_queue, kernel, 0, NULL,NULL);
	/* Copy results from the memory buffer */
	ret = clEnqueueReadBuffer(command_queue, memobj_core, CL_TRUE, 0,
		n * sizeof(bool), core, 0, NULL, NULL);
	ret = clEnqueueReadBuffer(command_queue, memobj_cor, CL_TRUE, 0,
		n * sizeof(int), tag, 0, NULL, NULL);
	free(global);
	free(local);

	finish = clock();

	cout << n << " speed time: " << (finish - start)*1.0 / CLOCKS_PER_SEC << "s\n" << endl;

	// 保存聚类结果
	saveDbscanResults();

	// 释放数组空间
	freeArray();

	//回收资源
	releaseOpenCL();

	return 0;
}
```

有一个相应的核文件dbscan.cl：

```
#define sqr(x) ((x)*(x))
struct pt
{
	float x[128];
};
// para: 0:n 1:esp 2:maxM 3:cor 4:flag 5:lasttag 6:rectag
__kernel void bfs(__global struct pt* point,
	__global int *pre,__global int *next,
	__global int *tags,__global bool *core,__global int  *cor, __global int *para, __global float *eps)
{
	int tn=get_local_size(0);
	int gn=get_num_groups(0);
	int x=get_group_id(0);//返回各个工作项的一维全局ID
	int y=get_local_id(0);

	int py,tpx;
	float tmp;

	local int px;


	if (y==0)
	{
		px=x;
		while ((tags[px]!=para[5])&&(px<para[0])) px+=gn;
		cor[px]=para[3];
	}
	barrier(CLK_LOCAL_MEM_FENCE);

	while (px<para[0])
	{
		py=pre[px]+y;
		while (py<=next[px])
		{
			int dim;
			tmp = 0.0f;
			for (dim = 0; dim < 128; dim++) {
				tmp += sqr(point[px].x[dim] - point[py].x[dim]);
			}
			if (tmp<=(*eps)*(*eps))
			{
				if (cor[py]==0) cor[py]=para[3];
				if ((tags[py]==0)&&(core[py])) {tags[py]=para[6]; para[4]=1;}
			}
			py+=tn;
		}
		barrier(CLK_LOCAL_MEM_FENCE);
		if (y==0)
		{
			px+=gn;
			while ((tags[px]!=para[5])&&(px<para[0])) px+=gn;
		}
		barrier(CLK_LOCAL_MEM_FENCE);
	}
}

// para: 0:n 1:esp 2:maxM 3:cor 4:flag
__kernel void core(__global struct pt* point,
	__global int* pre,__global int *next,
	__global bool* core, __global int* para, __global float *eps)
{
	int gn=get_global_size(0);
	int i,cnt;
	float tmp;
	int px=get_global_id(0);
	while (px<para[0])
	{
		cnt=1;
		for (i=pre[px];i<=next[px];++i)
		{
			int dim;
			tmp = 0.0f;
			for (dim = 0; dim < 128; dim++) {
				tmp += sqr(point[px].x[dim] - point[i].x[dim]);
			}
			if (tmp<=(*eps)*(*eps)) cnt++;
		}
		if (cnt>para[2]) core[px]=true; else core[px]=false;
		px+=gn;
	}
}

// para: 0:n 1:esp 2:maxM 3:cor 4:flag 5:lasttag 6:rectag 7:set_st
__kernel void set(__global struct pt* point,
	__global int *para,
	__global int *tags,__global bool* core)
{
	para[4]=0;
	for (int i=para[7];i<para[0];++i)
	if ((core[i])&&(tags[i]==0))
	{
		tags[i]=para[6];
		para[4]=1; para[8]=i; para[9]=core[i];
		break;
	}
}
```

* 编译生成可执行文件，命令如下(为什么要加-I和-lOpenCL，参考可能遇到的问题1,2,3)：
    * g++ -std=c++11 -I/usr/local/cuda-8.0/targets/x86_64-linux/include dbscan_opencl.cpp -lOpenCL
* 然后通过向main方法传入四个参数分别是：
    * 文件名
    * 数据量
    * eps
    * minPts

## 三、JNI调用

前几步同JNI调用C++一样，不同在于本地方法的实现不是用C++实现，而是OpenCL的编程模式实现，然后只需要根据OpenCL重新生成动态库文件。

```
#include "intellif_minning_dbscan_impl_DBSCANImpl.h"
#include <stdlib.h>
#include <CL/cl.h>
#include<stdio.h>
#include<string.h>
#include<math.h>
#include<iostream>
#include<algorithm>
#include<memory>
#include <fstream>
#include <sstream>
#include <vector>
#include <ctime>

using namespace std;

#define MAX_SOURCE_SIZE (0x1000000)

#define sqr(x) ((x)*(x))
float eps;
int m;

struct pt
{
	float x[128];
};

string *imgs;//图片地址

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

int *para;
bool *core;



int getProgramBuildInfo(cl_program program, cl_device_id device)
{
	size_t log_size;
	char *program_log;
	/* Find size of log and print to std output */
	clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG,
		0, NULL, &log_size);
	program_log = (char*)malloc(log_size + 1);
	program_log[log_size] = '\0';
	clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG,
		log_size + 1, program_log, NULL);
	printf("%s\n", program_log);
	free(program_log);
	return 0;
}

bool compare(int a, int b)
{
	return tmp[a].x[0] < tmp[b].x[0];
}

cl_device_id device_id = NULL;
cl_context context = NULL;
cl_command_queue command_queue = NULL;
cl_mem memobj_point = NULL;
cl_mem memobj_pre = NULL;
cl_mem memobj_next = NULL;
cl_mem memobj_cor = NULL;
cl_mem memobj_core = NULL;
cl_mem memobj_para = NULL;
cl_mem memobj_tags = NULL;
cl_mem memobj_eps = NULL;

cl_program program = NULL;
cl_kernel kernel_core, kernel_bfs, kernel_set = NULL;
cl_platform_id platform_id = NULL;
cl_int ret;

void releaseOpenCL() {
	ret = clReleaseKernel(kernel_core);
	ret = clReleaseKernel(kernel_bfs);
	ret = clReleaseKernel(kernel_set);
	ret = clReleaseProgram(program);
	ret = clReleaseMemObject(memobj_point);
	ret = clReleaseMemObject(memobj_pre);
	ret = clReleaseMemObject(memobj_next);
	ret = clReleaseMemObject(memobj_cor);
	ret = clReleaseMemObject(memobj_core);
	ret = clReleaseMemObject(memobj_para);
	ret = clReleaseMemObject(memobj_tags);
	ret = clReleaseMemObject(memobj_eps);
	ret = clReleaseCommandQueue(command_queue);
	ret = clReleaseContext(context);
	ret = clReleaseDevice(device_id);
}

void initOpenCL()
{
	FILE *fp;
	char fileName[] = "./dbscan.cl";
	char *source_str;
	size_t source_size;

	/* Load the source code containing the kernel*/
	fp = fopen(fileName, "r");
	if (!fp) {
		fprintf(stderr, "Failed to load kernel.\n");
		exit(1);
	}
	source_str = (char*)malloc(MAX_SOURCE_SIZE);
	source_size = fread(source_str, 1, MAX_SOURCE_SIZE, fp);
	fclose(fp);

	/* Get Platform and Device Info */
	cl_uint ret_num_platforms;
	cl_uint ret_num_devices;
	ret = clGetPlatformIDs(1, &platform_id, &ret_num_platforms);
	ret = clGetDeviceIDs(platform_id, CL_DEVICE_TYPE_GPU, 1, &device_id,
		&ret_num_devices);

	/* Create OpenCL context */
	context = clCreateContext(NULL, 1, &device_id, NULL, NULL, &ret);

	/* Create Command Queue */
	command_queue = clCreateCommandQueue(context, device_id, CL_QUEUE_PROFILING_ENABLE, &ret);

	/* Create Memory Buffer */
	memobj_point = clCreateBuffer(context, CL_MEM_READ_WRITE, (n) * sizeof(pt),
		NULL, &ret);
	memobj_pre = clCreateBuffer(context, CL_MEM_READ_WRITE, (n) * sizeof(int),
		NULL, &ret);
	memobj_next = clCreateBuffer(context, CL_MEM_READ_WRITE, n * sizeof(int),
		NULL, &ret);
	memobj_tags = clCreateBuffer(context, CL_MEM_READ_WRITE, n * sizeof(int),
		NULL, &ret);
	memobj_core = clCreateBuffer(context, CL_MEM_READ_WRITE, n * sizeof(bool),
		NULL, &ret);
	memobj_cor = clCreateBuffer(context, CL_MEM_READ_WRITE, n * sizeof(int),
		NULL, &ret);
	memobj_para = clCreateBuffer(context, CL_MEM_READ_WRITE, 10 * sizeof(int),
		NULL, &ret);
	memobj_eps = clCreateBuffer(context, CL_MEM_READ_WRITE, sizeof(float),
		NULL, &ret);

	ret = clEnqueueWriteBuffer(command_queue, memobj_core, CL_TRUE, 0,
		n * sizeof(bool), core, 0, NULL, NULL);

	ret = clEnqueueWriteBuffer(command_queue, memobj_point, CL_TRUE, 0,
		n * sizeof(pt), point, 0, NULL, NULL);

	ret = clEnqueueWriteBuffer(command_queue, memobj_pre, CL_TRUE, 0,
		n * sizeof(int), pre, 0, NULL, NULL);

	ret = clEnqueueWriteBuffer(command_queue, memobj_next, CL_TRUE, 0,
		n * sizeof(int), Next, 0, NULL, NULL);

	ret = clEnqueueWriteBuffer(command_queue, memobj_tags, CL_TRUE, 0,
		n * sizeof(int), tag, 0, NULL, NULL);

	ret = clEnqueueWriteBuffer(command_queue, memobj_cor, CL_TRUE, 0,
		n * sizeof(int), tag, 0, NULL, NULL);

	float *m_eps = new float(eps);
	ret = clEnqueueWriteBuffer(command_queue, memobj_eps, CL_TRUE, 0,
		sizeof(float), m_eps, 0, NULL, NULL);
	free(m_eps);

	para = (int*)malloc(sizeof(int) * 10);
	para[0] = n; para[1] = 4; para[2] = m; para[7] = 0;

	ret = clEnqueueWriteBuffer(command_queue, memobj_para, CL_TRUE, 0,
		10 * sizeof(int), para, 0, NULL, NULL);

	/* Create Kernel Program from the source */
	program = clCreateProgramWithSource(context, 1, (const char **)&source_str,
		(const size_t *)&source_size, &ret);

	/* Build Kernel Program */
	ret = clBuildProgram(program, 1, &device_id, NULL, NULL, NULL);

	getProgramBuildInfo(program, device_id);

	/* Create OpenCL Kernel */
	kernel_core = clCreateKernel(program, "core", &ret);
	kernel_bfs = clCreateKernel(program, "bfs", &ret);
	kernel_set = clCreateKernel(program, "set", &ret);
	/* Set OpenCL Kernel Parameters */
	ret = clSetKernelArg(kernel_bfs, 0, sizeof(cl_mem), (void *)&memobj_point);
	ret = clSetKernelArg(kernel_bfs, 1, sizeof(cl_mem), (void *)&memobj_pre);
	ret = clSetKernelArg(kernel_bfs, 2, sizeof(cl_mem), (void *)&memobj_next);
	ret = clSetKernelArg(kernel_bfs, 3, sizeof(cl_mem), (void *)&memobj_tags);
	ret = clSetKernelArg(kernel_bfs, 4, sizeof(cl_mem), (void *)&memobj_core);
	ret = clSetKernelArg(kernel_bfs, 5, sizeof(cl_mem), (void *)&memobj_cor);
	ret = clSetKernelArg(kernel_bfs, 6, sizeof(cl_mem), (void *)&memobj_para);
	ret = clSetKernelArg(kernel_bfs, 7, sizeof(cl_mem), (void *)&memobj_eps);

	ret = clSetKernelArg(kernel_core, 0, sizeof(cl_mem), (void *)&memobj_point);
	ret = clSetKernelArg(kernel_core, 1, sizeof(cl_mem), (void *)&memobj_pre);
	ret = clSetKernelArg(kernel_core, 2, sizeof(cl_mem), (void *)&memobj_next);
	ret = clSetKernelArg(kernel_core, 3, sizeof(cl_mem), (void *)&memobj_core);
	ret = clSetKernelArg(kernel_core, 4, sizeof(cl_mem), (void *)&memobj_para);
	ret = clSetKernelArg(kernel_core, 5, sizeof(cl_mem), (void *)&memobj_eps);

	ret = clSetKernelArg(kernel_set, 0, sizeof(cl_mem), (void *)&memobj_point);
	ret = clSetKernelArg(kernel_set, 1, sizeof(cl_mem), (void *)&memobj_para);
	ret = clSetKernelArg(kernel_set, 2, sizeof(cl_mem), (void *)&memobj_tags);
	ret = clSetKernelArg(kernel_set, 3, sizeof(cl_mem), (void *)&memobj_core);
}

void para_update()
{
	ret = clEnqueueWriteBuffer(command_queue, memobj_para, CL_TRUE, 0,
		10 * sizeof(int), para, 0, NULL, NULL);
}

void para_download()
{
	ret = clEnqueueReadBuffer(command_queue, memobj_para, CL_TRUE, 0,
		10 * sizeof(int), para, 0, NULL, NULL);
}

/* 动态分配数组空间 */
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
	imgs = new string[n];
	core = new bool[n]();
}

/* 释放数组空间 */
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
	delete[]imgs;
	delete[]core;
	sub = NULL;
	tag = NULL;
	tmp = NULL;
	point = NULL;
	resub = NULL;
	pre = NULL;
	Next = NULL;
	list = NULL;
	inside = NULL;
	imgs = NULL;
	core = NULL;
}

/* 删除字符串中空格，制表符tab等无效字符 */
string Trim(string& str)
{
	//str.find_first_not_of(" \t\r\n"),在字符串str中从索引0开始，返回首次不匹配"\t\r\n"的位置
	str.erase(0, str.find_first_not_of(" \t\r\n"));
	str.erase(str.find_last_not_of(" \t\r\n") + 1);
	return str;
}

/* 从文件读取数据源对tmp等数组初始化 */
void readFile(string file) {
	ifstream fin(file); //打开文件流操作
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
		imgs[point_count] = Trim(fields[1]); //清除掉向量fields中第二个元素的无效字符，并赋值给变量img

		sub[point_count] = point_count; // 初始化sub，数值为文件读取的顺序id，1，2，3，4...这样递增
		point_count++;
		if (point_count >= n)
		{
			break;
		}
	}

	/* 将数组的下标按照tem[i].x[0]排序从小到大排序，排序后sub数值为 temp[i].x[0]的值从小到大的id */
	sort(sub, sub + n, compare);

	/* 按照新下标将数组从小到大赋值给point数组，得到一个顺序的数组，并记录数组中每个数据的原始下标 */
	for (int i = 0; i < n; ++i)
	{
		point[i] = tmp[sub[i]];
		resub[sub[i]] = i;
	}
}

// 对源数据进行预处理，为后续寻找邻居点减少遍历时间
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
	/*paremeter for OpenCL Kernel */
	size_t *global, *local;
	global = (size_t*)malloc(sizeof(size_t) * 5);
	local = (size_t*)malloc(sizeof(size_t) * 5);
	global[0] = 2000;
	local[0] = 0;

	int bfs_tag = 1;
	int corcnt = 0;
	para[6] = bfs_tag;
	para_update();
	ret = clEnqueueNDRangeKernel(command_queue, kernel_core, 1, 0, global, NULL,
		0, 0, NULL);
	clFinish(command_queue);
	ret = clEnqueueTask(command_queue, kernel_set, 0, NULL,
		NULL);
	clFinish(command_queue);
	para_download();
	//std::cerr << idex[para[8]] << " as init\n";
	while (para[4] == 1)
	{
		para[3] = ++corcnt;
		while (para[4] == 1)
		{
			para[5] = bfs_tag++;
			para[6] = bfs_tag;
			para[4] = 0;
			para_update();
			global[0] = 8192 * 2;
			local[0] = 32;
			ret = clEnqueueNDRangeKernel(command_queue, kernel_bfs, 1, 0, global, local,
				0, 0, NULL);
			clFinish(command_queue);
			para_download();
		}
		para[5] = bfs_tag++;
		para[6] = bfs_tag;
		para_update();
		ret = clEnqueueTask(command_queue, kernel_set, 0, NULL,
			NULL);
		clFinish(command_queue);
		para_download();
	}
	//	ret = clEnqueueTask(command_queue, kernel, 0, NULL,NULL);
	/* Copy results from the memory buffer */
	ret = clEnqueueReadBuffer(command_queue, memobj_core, CL_TRUE, 0,
		n * sizeof(bool), core, 0, NULL, NULL);
	ret = clEnqueueReadBuffer(command_queue, memobj_cor, CL_TRUE, 0,
		n * sizeof(int), tag, 0, NULL, NULL);
	free(global);
	free(local);
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
		string features = Trim(fields[0]); //清除掉向量fields中第一个元素的无效字符，并赋值给变量features(特征值字符串)

		istringstream featurestream(features);// 特征值转换， 并初始化temp，此时的temp顺序是按照文件中读取的顺序
		string feature;
		int dims = 0;
		while (getline(featurestream, feature, '_'))
		{
			tmp[point_count].x[dims++] = stof(feature);
		}
		imgs[point_count] = Trim(fields[1]); //清除掉向量fields中第二个元素的无效字符，并赋值给变量img
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

	// 预处理，减少后续寻找邻居点的遍历次数
	preHandle();

	// 初始化opencl
	initOpenCL();

	cout << "before cluster" << endl;

	// 聚类
	cluster();

	finish = clock();

	cout << "dbscan success" << endl;

	cout << n << " speed time: " << (finish - start)*1.0 / CLOCKS_PER_SEC << "s\n" << endl;
}

JNIEXPORT jstring JNICALL Java_intellif_minning_dbscan_impl_DBSCANImpl_saveDBSCAN
(JNIEnv *env, jobject obj) {

	ofstream fout;
	char resultFile[128] = "result.csv";
	fout.open(resultFile);
	for (int i = 0; i < n; i++) {
		fout << i << "," << tag[resub[i]] << endl;
	}
	fout.close();

	// 释放内存
	freeArrays();

	//回收资源
	releaseOpenCL();

	cout << "save result success" << endl;

	return env->NewStringUTF(resultFile);
}
```

编译生成动态链接库：

g++ -std=c++11 -fPIC -I/usr/local/cuda-8.0/targets/x86_64-linux/include -I `$JAVA_HOME`/include -I `$JAVA_HOME`/include/linux -shared -o libdbscan.so dbscan.cpp -lOpenCL

然后运行：
eg: java -Djava.library.path=lib/opencl intellif.minning.dbscan.DBSCANImplTest 500.csv 500 0.87 1 

## 四、可能遇到的错误

### 4.1、问题1

linux下生成动态文件库时执行如下命令：

g++ -fPIC -I `$JAVA_HOME`/include -I `$JAVA_HOME`/include/linux -shared -o libdbscan.so dbscan.cpp

出现错误，提示 CL/cl.h: No such file or directory。

主要原因是未指定CL/cl.h依赖：

* 先通过`find / -iname cl.h 2>/dev/null` 找到cl.h位置，我电脑上是/usr/local/cuda-8.0/targets/x86_64-linux/include
* 生成动态文件指定相应的依赖：

g++ -std=c++11 -fPIC -I/usr/local/cuda-8.0/targets/x86_64-linux/include -I `$JAVA_HOME`/include -I `$JAVA_HOME`/include/linux -shared -o libdbscan.so dbscan.cpp

可以参考这个解决：https://stackoverflow.com/questions/21301905/compiling-an-opencl-program-using-a-cl-cl-h-file。

### 4.2、问题2

运行的时候又出现错误：

symbol lookup error: /mnt/hd/wanqingfeng_workspace/test/lib/opencl/libdbscan.so: undefined symbol: clGetPlatformIDs

在生成动态库时加上-lOpenCL命令，**确保链接到OpenCL库**，重新生成动态库，然后运行，错误解决。

g++ -std=c++11 -fPIC -I/usr/local/cuda-8.0/targets/x86_64-linux/include -I `$JAVA_HOME`/include -I `$JAVA_HOME`/include/linux -shared -o libdbscan.so dbscan.cpp -lOpenCL

### 4.3、问题3

不论是在生成可执行文件，还是再生成动态链接库时，都要注意-lOpenCL的位置，如果没有放在文件后面，可能会遇到错误：

undefined reference to clGetPlatformIDs 
undefined reference to clGetDeviceIDs
...

等一堆警告，解决方案是将-lOpenCL放到文件名后面。 

![](http://p7ohlnlz4.bkt.clouddn.com/201805022_opencl_01.png)

参看解决：
https://stackoverflow.com/questions/7783345/library-is-linked-but-reference-is-undefined





