package example;/*
 * JCuda - Java bindings for NVIDIA CUDA driver and runtime API
 * http://www.jcuda.org
 *
 * Copyright 2011 Marco Hutter - http://www.jcuda.org
 */

import static jcuda.driver.JCudaDriver.*;

import java.io.*;

import jcuda.*;
import jcuda.driver.*;

/**
 * This is a sample class demonstrating how to use the JCuda driver
 * bindings to load a CUDA kernel in form of an PTX file and execute 
 * the kernel. The sample reads a CUDA file, compiles it to a PTX 
 * file using NVCC, and loads the PTX file as a module. <br />
 * <br />
 * The the sample creates a 2D float array and passes it to the kernel 
 * that sums up the elements of each row of the array (each in its 
 * own thread) and writes the sums into an 1D output array.
 */
public class JCudaDriverSample
{
    /**
     * Entry point of this sample
     * 
     * @param args Not used
     * @throws IOException If an IO error occurs
     */
    public static void main(String args[]) throws IOException
    {
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);
        
        // Create the PTX file by calling the NVCC
        String ptxFileName = preparePtxFile("JCudaSampleKernel.cu");
        
        // Initialize the driver and create a context for the first device.
        cuInit(0);
        CUcontext pctx = new CUcontext();
        CUdevice dev = new CUdevice();
        cuDeviceGet(dev, 0);
        cuCtxCreate(pctx, 0, dev);

        // Load the ptx file.
        CUmodule module = new CUmodule();
        cuModuleLoad(module, ptxFileName);

        // Obtain a function pointer to the "sampleKernel" function.
        CUfunction function = new CUfunction();
        cuModuleGetFunction(function, module, "sampleKernel");

        int numThreads = 8;
        int size = 128;

        // Allocate and fill host input memory: A 2D float array with
        // 'numThreads' rows and 'size' columns, each row filled with
        // the values from 0 to size-1.
        float hostInput[][] = new float[numThreads][size];
        for(int i = 0; i < numThreads; i++)
        {
            for (int j=0; j<size; j++)
            {
                hostInput[i][j] = (float)j;
            }
        }

        // Allocate arrays on the device, one for each row. The pointers
        // to these array are stored in host memory.
        CUdeviceptr hostDevicePointers[] = new CUdeviceptr[numThreads];
        for(int i = 0; i < numThreads; i++)
        {
            hostDevicePointers[i] = new CUdeviceptr();
            cuMemAlloc(hostDevicePointers[i], size * Sizeof.FLOAT);
        }

        // Copy the contents of the rows from the host input data to
        // the device arrays that have just been allocated.
        for(int i = 0; i < numThreads; i++)
        {
            cuMemcpyHtoD(hostDevicePointers[i],
                Pointer.to(hostInput[i]), size * Sizeof.FLOAT);
        }

        // Allocate device memory for the array pointers, and copy
        // the array pointers from the host to the device.
        CUdeviceptr deviceInput = new CUdeviceptr();
        cuMemAlloc(deviceInput, numThreads * Sizeof.POINTER);
        cuMemcpyHtoD(deviceInput, Pointer.to(hostDevicePointers),
            numThreads * Sizeof.POINTER);

        // Allocate device output memory: A single column with
        // height 'numThreads'.
        CUdeviceptr deviceOutput = new CUdeviceptr();
        cuMemAlloc(deviceOutput, numThreads * Sizeof.FLOAT);

        // Set up the kernel parameters: A pointer to an array
        // of pointers which point to the actual values.
        Pointer kernelParams = Pointer.to(
            Pointer.to(deviceInput), 
            Pointer.to(new int[]{size}), 
            Pointer.to(deviceOutput)
        );
        
        // Call the kernel function.
        cuLaunchKernel(function, 
            1, 1, 1,           // Grid dimension 
            numThreads, 1, 1,  // Block dimension
            0, null,           // Shared memory size and stream 
            kernelParams, null // Kernel- and extra parameters
        ); 
        cuCtxSynchronize();

        // Allocate host output memory and copy the device output
        // to the host.
        float hostOutput[] = new float[numThreads];
        cuMemcpyDtoH(Pointer.to(hostOutput), deviceOutput,
            numThreads * Sizeof.FLOAT);

        // Verify the result
        boolean passed = true;
        for(int i = 0; i < numThreads; i++)
        {
            float expected = 0;
            for(int j = 0; j < size; j++)
            {
                expected += hostInput[i][j];
            }
            if (Math.abs(hostOutput[i] - expected) > 1e-5)
            {
                passed = false;
                break;
            }
        }
        System.out.println("Test "+(passed?"PASSED":"FAILED"));

        // Clean up.
        for(int i = 0; i < numThreads; i++)
        {
            cuMemFree(hostDevicePointers[i]);
        }
        cuMemFree(deviceInput);
        cuMemFree(deviceOutput);
    }
    
    /**
     * The extension of the given file name is replaced with "ptx".
     * If the file with the resulting name does not exist, it is 
     * compiled from the given file using NVCC. The name of the 
     * PTX file is returned. 
     * 
     * @param cuFileName The name of the .CU file
     * @return The name of the PTX file
     * @throws IOException If an I/O error occurs
     */
    private static String preparePtxFile(String cuFileName) throws IOException
    {
        int endIndex = cuFileName.lastIndexOf('.');
        if (endIndex == -1)
        {
            endIndex = cuFileName.length()-1;
        }
        String ptxFileName = cuFileName.substring(0, endIndex+1)+"ptx";
        File ptxFile = new File(ptxFileName);
        if (ptxFile.exists())
        {
            return ptxFileName;
        }
        
        File cuFile = new File(cuFileName);
        if (!cuFile.exists())
        {
            throw new IOException("Input file not found: "+cuFileName);
        }
        String modelString = "-m"+System.getProperty("sun.arch.data.model");        
        String command = 
            "nvcc " + modelString + " -ptx "+
            cuFile.getPath()+" -o "+ptxFileName;
        
        System.out.println("Executing\n"+command);
        Process process = Runtime.getRuntime().exec(command);

        String errorMessage = 
            new String(toByteArray(process.getErrorStream()));
        String outputMessage = 
            new String(toByteArray(process.getInputStream()));
        int exitValue = 0;
        try
        {
            exitValue = process.waitFor();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException(
                "Interrupted while waiting for nvcc output", e);
        }

        if (exitValue != 0)
        {
            System.out.println("nvcc process exitValue "+exitValue);
            System.out.println("errorMessage:\n"+errorMessage);
            System.out.println("outputMessage:\n"+outputMessage);
            throw new IOException(
                "Could not create .ptx file: "+errorMessage);
        }
        
        System.out.println("Finished creating PTX file");
        return ptxFileName;
    }

    /**
     * Fully reads the given InputStream and returns it as a byte array
     *  
     * @param inputStream The input stream to read
     * @return The byte array containing the data from the input stream
     * @throws IOException If an I/O error occurs
     */
    private static byte[] toByteArray(InputStream inputStream) 
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buffer[] = new byte[8192];
        while (true)
        {
            int read = inputStream.read(buffer);
            if (read == -1)
            {
                break;
            }
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }
    
    
}