extern "C"
__global__ void cudaGetNeighbors(float* xs, float* ys, int* vis, int len, int* neighbors, double minEps, int minPts) {

	unsigned int	tid	= blockIdx.x * blockDim.x + threadIdx.x;
	unsigned int	src;
	unsigned int	dest;
	unsigned int	point_id = tid;
	unsigned int	neighborscnt;

	while (point_id < len * len) {
		src = point_id / len;
		dest = point_id % len;
		float dist;
		if (src <= dest) {
			float srcX = xs[src];
			float destX = xs[dest];
			float srcY = ys[src];
			float destY = ys[dest];
			float xRes = srcX - destX;
			float yRes = srcY - destY;
			dist = xRes * xRes + yRes * yRes;
			if (dist <= minEps * minEps) {
				neighbors[point_id] = 1;
			}
			neighbors[dest * len + src] = neighbors[point_id];
		}
		point_id += blockDim.x * gridDim.x;
	}

	__syncthreads();

	point_id = tid;
	while (point_id < len) {
		neighborscnt = 1;
		src = point_id * len;
		for (int i = 0; i < len; i++) {
			if (point_id != i) {
				if (neighbors[src + i]) {
					neighborscnt++;
				}
			}
		}
		if (neighborscnt >= minPts) {
			vis[point_id]++;
		}
		point_id += blockDim.x * gridDim.x;
	}
}
