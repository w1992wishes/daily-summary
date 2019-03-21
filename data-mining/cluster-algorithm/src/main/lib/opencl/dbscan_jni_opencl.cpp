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

//string *imgs;//图片地址

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
	// 1.获取平台
    clGetPlatformIDs(1, &platform_id, NULL);
    if (platform_id == NULL)
    {
        puts("Get OpenCL platform failed!");
    }

    // 2.获取设备
    clGetDeviceIDs(platform_id, CL_DEVICE_TYPE_GPU, 1, &device_id, NULL);
    if (device_id == NULL)
    {
        puts("No GPU available as a compute device!");
    }

    // 3.获取上下文
    context = clCreateContext(NULL, 1, &device_id, NULL, NULL, &ret);
    if (context == NULL)
    {
        puts("Context not established!");
    }

    // 4.命令队列
    command_queue = clCreateCommandQueue(context, device_id, 0, &ret);
    if (command_queue == NULL)
    {
        puts("Command queue cannot be created!");
    }

    // 5.装载内核程序，编译CL program ，生成CL内核实例
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
    fseek(fp, 0, SEEK_END);
    const long kernelLength = ftell(fp);
    fseek(fp, 0, SEEK_SET);

    source_str = (char*)malloc(kernelLength);
    source_size = fread(source_str, 1, kernelLength, fp);
    fclose(fp);

    /* Create Kernel Program from the source */
    program = clCreateProgramWithSource(context, 1, (const char **)&source_str,
        (const size_t *)&source_size, &ret);

    /* 6.Build Kernel Program */
    ret = clBuildProgram(program, 1, &device_id, NULL, NULL, NULL);
    if (ret != CL_SUCCESS)
    {
        size_t len;
        char buffer[8 * 1024];

        printf("Error: Failed to build program executable!\n");
        clGetProgramBuildInfo(program, device_id, CL_PROGRAM_BUILD_LOG, sizeof(buffer), buffer, &len);
        printf("%s\n", buffer);
    }

    /* 7.Create Memory Buffer */
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
    memobj_eps = clCreateBuffer(context, CL_MEM_READ_ONLY, sizeof(float),
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
	//imgs = new string[n];
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
	//delete[]imgs;
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
	//imgs = NULL;
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
		//imgs[point_count] = Trim(fields[1]); //清除掉向量fields中第二个元素的无效字符，并赋值给变量img

		sub[point_count] = point_count; // 初始化sub，数值为文件读取的顺序id，1，2，3，4...这样递增
		point_count++;
		if (point_count >= n)
		{
			break;
		}
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

	readFile(file);

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
	char resultFile[128];
	sprintf_s(resultFile, "%d_result.csv", n);
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