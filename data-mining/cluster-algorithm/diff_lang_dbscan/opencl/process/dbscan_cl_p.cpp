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

using namespace std;

#define MAX_SOURCE_SIZE (0x1000000)

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

int *para;



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
	return tmp[a].x[0]<tmp[b].x[0];
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

cl_program program = NULL;
cl_kernel kernel_core, kernel_bfs, kernel_set = NULL;
cl_platform_id platform_id = NULL;
cl_uint ret_num_devices;
cl_uint ret_num_platforms;
cl_int ret;


void init()
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
	sort(index, index + n, compare);
	for (int i = 0; i<n; ++i)
	{
		point[i] = tmp[index[i]];
		reindex[index[i]] = i;
	}
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

	//	printf("test\n");
	FILE *fp;
	char fileName[] = "./gpuDBSCAN.cl";
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

	ret = clSetKernelArg(kernel_core, 0, sizeof(cl_mem), (void *)&memobj_point);
	ret = clSetKernelArg(kernel_core, 1, sizeof(cl_mem), (void *)&memobj_pre);
	ret = clSetKernelArg(kernel_core, 2, sizeof(cl_mem), (void *)&memobj_next);
	ret = clSetKernelArg(kernel_core, 3, sizeof(cl_mem), (void *)&memobj_core);
	ret = clSetKernelArg(kernel_core, 4, sizeof(cl_mem), (void *)&memobj_para);

	ret = clSetKernelArg(kernel_set, 0, sizeof(cl_mem), (void *)&memobj_point);
	ret = clSetKernelArg(kernel_set, 1, sizeof(cl_mem), (void *)&memobj_para);
	ret = clSetKernelArg(kernel_set, 2, sizeof(cl_mem), (void *)&memobj_tags);
	ret = clSetKernelArg(kernel_set, 3, sizeof(cl_mem), (void *)&memobj_core);

}

bool *core = new bool[n]();

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

int main()
{

	init();

	/*paremeter for OpenCL Kernel */
	size_t *global, *local;
	global = (size_t*)malloc(sizeof(size_t) * 5);
	local = (size_t*)malloc(sizeof(size_t) * 5);
	global[0] = 2000;
	local[0] = 0;


	ret = clEnqueueReadBuffer(command_queue, memobj_core, CL_TRUE, 0,
		n * sizeof(bool), core, 0, NULL, NULL);
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
			std::cerr << bfs_tag << "\n";
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
		std::cerr << index[para[8]] << " as init " << para[8] << "\n";
	}
	//	ret = clEnqueueTask(command_queue, kernel, 0, NULL,NULL);
	/* Copy results from the memory buffer */

	ret = clEnqueueReadBuffer(command_queue, memobj_cor, CL_TRUE, 0,
		n * sizeof(int), tag, 0, NULL, NULL);

	for (int i = 0; i<n; ++i)
		printf("%d %d\n", i, tag[reindex[i]]);


}
