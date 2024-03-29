#include "cuda_runtime.h"
#include "device_functions.h"
#include "cublas_v2.h"
#include "device_launch_parameters.h"
#include <iostream>
#include <fstream>
#include <sstream>
#include <cstdlib>
#include <ctime>
#include <math.h>
#include <queue>
#include <string.h>
#include <stdlib.h>
#include <vector>
#include<stdio.h>
#include<algorithm>
#include<memory>

using namespace std;

struct Point {
	float		dimensions[128];
	int			cluster;
	int			noise;  //-1 noise;
    string      img;
};

float eps = 0.87f;//neighborhood radius
int min_nb = 1;
int n;
Point *host_sample;
int block_num = 10;
int thread_num = 100;

float __device__ dev_euclidean_distance(const Point &src, const Point &dest) {
    float res = 0.0;
    for(int i=0; i<128; i++){
        res += (src.dimensions[i] - dest.dimensions[i]) * (src.dimensions[i] - dest.dimensions[i]);
    }
	return sqrt(res);
}

// to get the total list
void __global__ dev_region_query(Point* sample, int num, int* neighbors, float eps, int min_nb) {

	unsigned int	tid = blockIdx.x * blockDim.x + threadIdx.x;
	unsigned int	line,col,pointer = tid;
	unsigned int	count;

	while (pointer < num * num) {//全场唯一id
		line = pointer / num;
		col = pointer % num;
		float radius;
		if (line <= col) {
			radius = dev_euclidean_distance(sample[line], sample[col]);
			if (radius <= eps) {
				neighbors[pointer] = 1;
			}
			neighbors[col * num + line] = neighbors[pointer];//对角线
		}
		pointer += blockDim.x * gridDim.x;
	}
	__syncthreads();

	pointer = tid;
	while (pointer < num) {
		count = 1;
		line = pointer * num;
		for (int i = 0; i < num; i++) {
			if (pointer != i && neighbors[line+i]) {//包含p点邻域元素个数
					count++;
			}
		}
		if (count >= min_nb) {
			sample[pointer].noise++;
		}
		pointer += blockDim.x * gridDim.x;
	}
}

void host_algorithm_dbscan() {
	int num = n;
	// sample
	Point* cuda_sample;
	cudaMalloc((void**)&cuda_sample, num * sizeof(Point));
	cudaMemcpy(cuda_sample, host_sample, num * sizeof(Point), cudaMemcpyHostToDevice);

	// neighbor list
	int *host_neighbor = new int[num*num]();
	int *dev_neighbor;
	cudaMalloc((void**)&dev_neighbor, num * num * sizeof(int));

	dev_region_query << <block_num, thread_num >> > (cuda_sample, num, dev_neighbor, eps, min_nb);

	cudaMemcpy(host_sample, cuda_sample, num * sizeof(Point), cudaMemcpyDeviceToHost);
	cudaMemcpy(host_neighbor, dev_neighbor, num * num * sizeof(int), cudaMemcpyDeviceToHost);

	queue<int> expand;
	int cur_cluster = 0;

	for (int i = 0; i < num; i++) {
		if (host_sample[i].noise >= 0 && host_sample[i].cluster < 1) {
			host_sample[i].cluster = ++cur_cluster;
			int src = i * num;
			for (int j = 0; j < num; j++) {
				if (host_neighbor[src + j]) {
					host_sample[j].cluster = cur_cluster;
					expand.push(j);
				}
			}

			while (!expand.empty()) {//expand the cluster
				if (host_sample[expand.front()].noise >= 0) {
					src = expand.front() * num;
					for (int j = 0; j < num; j++) {
						if (host_neighbor[src + j] && host_sample[j].cluster < 1) {
							host_sample[j].cluster = cur_cluster;
							expand.push(j);
						}
					}
				}
				expand.pop();
			}
		}
	}
	cudaFree(cuda_sample);cudaFree(dev_neighbor);
}

//删除字符串中空格，制表符tab等无效字符
string Trim(string& str)
{
	//str.find_first_not_of(" \t\r\n"),在字符串str中从索引0开始，返回首次不匹配"\t\r\n"的位置
	str.erase(0, str.find_first_not_of(" \t\r\n"));
	str.erase(str.find_last_not_of(" \t\r\n") + 1);
	return str;
}

bool initDatasFromFile(string inputFileName) {
	try
	{
		host_sample = new Point[n];// 分配数组空间
	}
	catch (const std::exception& e)
	{
		cout << "alloca arrays exception: " << e.what() << endl;
	}

	cout << "before read file" << endl;

	ifstream fin(inputFileName); //打开文件流操作
	if (!fin)
	{
		cout << "file not found" << endl;
		return false;
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
			host_sample[point_count].dimensions[dims++] = stof(feature);
		}
		host_sample[point_count].img = Trim(fields[1]); //清除掉向量fields中第二个元素的无效字符，并赋值给变量img
		host_sample[point_count].noise = -1;
        host_sample[point_count].cluster = -1;
		point_count++;
		if (point_count >= n)
		{
			break;
		}
	}

	cout << "init points from file success" << endl;

	return true;
}

int main(int argc, char* argv[]) {
    string inputFileName(argv[1]);//数据源文件
    string nstr(argv[2]);// 数据数
    string epsli(argv[3]); // eps
    string minPts(argv[4]); // minPts

    eps = stof(epsli);
    min_nb = stoi(minPts);
	n = stoi(nstr);
	initDatasFromFile(inputFileName);

	clock_t start, finish;
	start = clock();
	host_algorithm_dbscan();
	finish = clock();

	cout << "dbscan success" << endl;

	cout << n << " speed time: " << (finish - start)*1.0 / CLOCKS_PER_SEC << "s\n" << endl;

	ofstream fout;

	char resultFile[128] = "result.html";
	fout.open(resultFile);
	for (int i = 0; i < n; i++) {
		fout <<"<img src='"<< host_sample[i].img << "'/>"<<host_sample[i].cluster<< endl;
	}
	fout.close();

	// 释放内存
	delete []host_sample;
	return 0;
}