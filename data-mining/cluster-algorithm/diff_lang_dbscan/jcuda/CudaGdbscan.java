import jcuda.*;
import jcuda.driver.*;
import static jcuda.driver.JCudaDriver.cuMemcpyDtoH;
import static jcuda.driver.JCudaDriver.cuMemcpyHtoD;
import static jcuda.driver.JCudaDriver.*;
import java.util.ArrayDeque;
import java.util.Arrays;

public class CudaGdbscan {
    public static int[] gdbscan(float[] xs, float[] ys, double eps, int minPts) {
        // Initialize the driver and create a context for the first device.
        cuInit(0);
        CUdevice device = new CUdevice();
        cuDeviceGet(device, 0);
        CUcontext context = new CUcontext();
        cuCtxCreate(context, 0, device);

        // Load the ptx file.
        String file = CudaGdbscan.class.getResource("getNeighbors.ptx").getFile().replace("/C:","C:");
        CUmodule module = new CUmodule();
        cuModuleLoad(module, file);

        // Obtain a function pointer to the "cudaGetNeighbors" function.
        CUfunction function = new CUfunction();
        cuModuleGetFunction(function,module,"cudaGetNeighbors");

        int length = xs.length;

        // Allocate and fill the host input data
        int[] initVis = new int[length];
        for (int i=0; i<length; i++){
            initVis[i] = -1;
        }

        int[] initNei = new int[length*length];
        for (int i=0; i< length*length; i++){
            initNei[i] = 0;
        }

        Pointer hostPointsX = Pointer.to(xs);
        Pointer hostPointsY = Pointer.to(ys);
        Pointer vis = Pointer.to(initVis);
        Pointer nei = Pointer.to(initNei);

        // Allocate the device input data, and copy the
        // host input data to the device
        CUdeviceptr cudaPointsX = new CUdeviceptr();
        cuMemAlloc(cudaPointsX,length * Sizeof.FLOAT);
        cuMemcpyHtoD(cudaPointsX, hostPointsX, length * Sizeof.FLOAT);

        CUdeviceptr cudaPointsY = new CUdeviceptr();
        cuMemAlloc(cudaPointsY,length * Sizeof.FLOAT);
        cuMemcpyHtoD(cudaPointsY, hostPointsY, length * Sizeof.FLOAT);

        CUdeviceptr cudaNeighborArray = new CUdeviceptr();
        cuMemAlloc(cudaNeighborArray,length * length * Sizeof.INT);
        cuMemcpyHtoD(cudaNeighborArray, nei, length * length * Sizeof.INT);

        CUdeviceptr cudaVis = new CUdeviceptr();
        cuMemAlloc(cudaVis,length * Sizeof.INT);
        cuMemcpyHtoD(cudaVis, vis, length * Sizeof.INT);

        // Set up the kernel parameters: A pointer to an array
        // of pointers which point to the actual values.
        Pointer kernelParameters = Pointer.to(
                Pointer.to(cudaPointsX),
                Pointer.to(cudaPointsY),
                Pointer.to(cudaVis),
                Pointer.to(new int[]{length}),
                Pointer.to(cudaNeighborArray),
                Pointer.to(new double[]{eps}),
                Pointer.to(new int[]{minPts})
        );

        // Call the kernel function.
        cuLaunchKernel(function,
                10,1,1, // Grid dimension
                10,1,1, // Block dimension
                0, null,  // Shared memory size and stream
                kernelParameters, null); // Kernel- and extra parameters
        cuCtxSynchronize();

        // Allocate host output memory and copy the device output
        // to the host.
        int[] hostNeighborArry = new int[length * length];
        int[] hostVis = new int[length];
        cuMemcpyDtoH(Pointer.to(hostNeighborArry), cudaNeighborArray, length * length * Sizeof.INT);
        cuMemcpyDtoH(Pointer.to(hostVis), cudaVis, length * Sizeof.INT);

        int[] hostIds = new int[length];
        Arrays.fill(hostIds, -1);
        hostSetIds(hostIds, hostVis, length, hostNeighborArry);

        return hostIds;
    }

    // vis[i] = > 0 表明是核心对象， ids为簇标识
    static void hostSetIds(int[] ids, int[] vis, int len, int[] hostNeighbors) {
        ArrayDeque<Integer> s = new ArrayDeque<>(len);
        int t_idx = 0;
        for (int i = 0; i < len; i++) {
            if (vis[i] >= 0) {
                if (ids[i] < 1) {
                    ids[i] = ++t_idx;
                    int src = i * len;
                    for (int j = 0; j < len; j++) {
                        if (hostNeighbors[src + j] > 0) {
                            ids[j] = t_idx;
                            s.push(j);
                        }
                    }
                    while (!s.isEmpty()) {
                        if (vis[s.peek()] >= 0) {
                            src = s.peek() * len;
                            for (int j = 0; j < len; j++) {
                                if (hostNeighbors[src + j] > 0) {
                                    if (ids[j] < 1) {
                                        ids[j] = t_idx;
                                        s.push(j);
                                    }
                                }
                            }
                        }
                        s.pop();
                    }
                }
            }
        }
    }
}
