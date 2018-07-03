extern "C"
__global__ void sampleKernel(float** globalInputData, int size, float* globalOutputData)
{
  const unsigned int tidX = threadIdx.x;
  globalOutputData[tidX] = 0;
  for (int i=0; i<size; i++)
  {
    globalOutputData[tidX] += globalInputData[tidX][i];
  }
  __syncthreads();
}