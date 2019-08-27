#include "CudaDBSCAN.h"
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

using namespace std;

struct Point {
	float		dimensions[128];
	int			cluster; // cluster id
	int			noise;  // -1 noise, 0 core
    string      img;
    int         id;
};

/* to get distance between two points */
float __device__ dev_euclidean_distance(const Point &src, const Point &dest) {
    float res = 0.0;
    for(int i=0; i<128; i++){
        res += (src.dimensions[i] - dest.dimensions[i]) * (src.dimensions[i] - dest.dimensions[i]);
    }
	return sqrt(res);
}

/* the gpu function */
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

void host_algorithm_dbscan(Point* host_sample, int num, float eps, int min_nb) {
	/*sample*/
	Point* cuda_sample;
	cudaMalloc((void**)&cuda_sample, num * sizeof(Point));
	cudaMemcpy(cuda_sample, host_sample, num * sizeof(Point), cudaMemcpyHostToDevice);

	/*neighbor list*/
	int *host_neighbor = new int[num*num]();
	int *dev_neighbor;
	cudaMalloc((void**)&dev_neighbor, num * num * sizeof(int));

    /* run on gpu */
    int block_num = 10;
	int thread_num = 100;
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

			while (!expand.empty()) {/*expand the cluster*/
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

	ofstream fout;
    fout.open("result.html");
    for (int i = 0; i < num; i++) {
    	fout << host_sample[i].id << " " <<host_sample[i].cluster<< endl;
    }
    fout.close();
}

extern "C"
JNIEXPORT void JNICALL Java_CudaDBSCAN_runDBSCAN
  (JNIEnv *env, jobject obj, jobject objectList, jfloat eps, jint minPts)
  {
        const char *str ="enter native method\n";
        cout << str <<endl;

        /* get the list class */
        jclass cls_list = env->GetObjectClass(objectList);
        if(cls_list == NULL){
            cout << "not find class\n" << endl;
        }

        /* method in class List  */
        jmethodID list_get = env->GetMethodID(cls_list, "get", "(I)Ljava/lang/Object;");
        jmethodID list_size = env->GetMethodID(cls_list, "size", "()I");
        if(list_get == NULL){
            cout << "not find get method\n" << endl;
        }
        if(list_size == NULL){
            cout << "not find size method\n" << endl;
        }

        /* jni invoke list.get to get points count */
        int len = static_cast<int>(env->CallIntMethod(objectList, list_size));
        if(len > 0){
            cout << len << endl;
        }

        /* define point array */
        Point host_sample[len];

        /* init point array */
        int i;
        for (i=0; i < len; i++) {
            /* get list the element -- float[] */
            jfloatArray element = (jfloatArray)(env->CallObjectMethod(objectList, list_get, i));
            if(element == NULL){
                cout << "fetch list element failure\n" << endl;
            }

            float *f_arrays;
            f_arrays = env->GetFloatArrayElements(element,NULL);
            if(f_arrays == NULL){
                cout << "fetch float array failure\n" << endl;
            }

            host_sample[i].id = i;
            host_sample[i].noise = -1;
            host_sample[i].cluster = -1;

            int j;
            int arr_len = static_cast<int>(env->GetArrayLength(element));
            for(j=0; j<arr_len ; j++){
                host_sample[i].dimensions[j] = f_arrays[j];
            }

            /* 释放可能复制的缓冲区 */
            env->ReleaseFloatArrayElements(element, f_arrays, 0);
            /* 调用 JNI 函数 DeleteLocalRef() 删除 Local reference。Local reference 表空间有限，这样可以避免 Local reference 表的内存溢出，避免 native memory 的 out of memory */
            env->DeleteLocalRef(element);
        }

        cudaEvent_t start, end;
        cudaEventCreate(&start);
        cudaEventCreate(&end);
        cudaEventRecord(start, 0);

        /* core : run dbscan */
        host_algorithm_dbscan(host_sample, len, static_cast<float>(eps), static_cast<int>(minPts));

        cudaEventRecord(end, 0);
        cudaEventSynchronize(end);

        float time;
        cudaEventElapsedTime(&time, start, end);
        cout<<"time: "<< time <<"ms --device\n"<<endl;

}