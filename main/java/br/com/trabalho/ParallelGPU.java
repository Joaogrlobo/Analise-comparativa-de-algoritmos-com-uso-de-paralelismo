package br.com.trabalho;

import org.jocl.*;
import static org.jocl.CL.*;

public class ParallelGPU {

    private static final String programSource =
        "__kernel void countWord(__global const char* text, const int text_len, " +
        "                        __global const char* word, const int word_len, " +
        "                        __global int* global_count) {" +
        "    int gid = get_global_id(0);" +
        "    if (gid > text_len - word_len) return;" +
        "    bool match = true;" +
        "    for(int i = 0; i < word_len; i++) {" +
        "        char t = text[gid + i];" +
        "        char w = word[i];" +
        "        if(t >= 'a' && t <= 'z') t -= 32;" +
        "        if(w >= 'a' && w <= 'z') w -= 32;" +
        "        if(t != w) { match = false; break; }" +
        "    }" +
        "    if(match) {" +
        "        bool start_ok = (gid == 0) || !( (text[gid-1] >= 'a' && text[gid-1] <= 'z') || (text[gid-1] >= 'A' && text[gid-1] <= 'Z') );" +
        "        bool end_ok = (gid + word_len == text_len) || !( (text[gid+word_len] >= 'a' && text[gid+word_len] <= 'z') || (text[gid+word_len] >= 'A' && text[gid+word_len] <= 'Z') );" +
        "        if(start_ok && end_ok) {" +
        "            atomic_inc(global_count);" +
        "        }" +
        "    }" +
        "}";

    public static int contarPalavra(String texto, String palavra) throws Exception {
        setExceptionsEnabled(true);

        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[0];

        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, numDevices, devices, null);
        cl_device_id device = devices[0]; // Usando o primeiro device (GPU/CPU)

        cl_context context = clCreateContext(contextProperties, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue commandQueue = clCreateCommandQueue(context, device, 0, null);

        byte[] textBytes = texto.getBytes("UTF-8");
        byte[] wordBytes = palavra.getBytes("UTF-8");
        int textLen = textBytes.length;
        int wordLen = wordBytes.length;

        int[] resultCount = new int[]{0};

        cl_mem memText = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
            Sizeof.cl_char * textLen, Pointer.to(textBytes), null);
        cl_mem memWord = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
            Sizeof.cl_char * wordLen, Pointer.to(wordBytes), null);
        cl_mem memResult = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
            Sizeof.cl_int, Pointer.to(resultCount), null);

        cl_program program = clCreateProgramWithSource(context, 1, new String[]{programSource}, null, null);
        clBuildProgram(program, 0, null, null, null, null);

        cl_kernel kernel = clCreateKernel(program, "countWord", null);

        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memText));
        clSetKernelArg(kernel, 1, Sizeof.cl_int, Pointer.to(new int[]{textLen}));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memWord));
        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{wordLen}));
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(memResult));

        long global_work_size[] = new long[]{textLen};
        
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, global_work_size, null, 0, null, null);
        clEnqueueReadBuffer(commandQueue, memResult, CL_TRUE, 0, Sizeof.cl_int, Pointer.to(resultCount), 0, null, null);

        clReleaseMemObject(memText);
        clReleaseMemObject(memWord);
        clReleaseMemObject(memResult);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);

        return resultCount[0];
    }
}
