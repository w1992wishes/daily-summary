import jcuda.runtime.JCuda;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) throws IOException {
        String fileName = "test1.txt";
        try (BufferedReader brt = new BufferedReader(new FileReader(fileName));
        ) {
            String line = null;
            List<Float> xs = new ArrayList<>();
            List<Float> ys = new ArrayList<>();
            while ((line = brt.readLine()) != null) {
                String[] s = line.split(" ");
                xs.add(Float.valueOf(s[0]));
                ys.add(Float.valueOf(s[1]));
            }
            float[] xsA = new float[xs.size()];
            float[] ysA = new float[ys.size()];

            for (int i = 0; i < xs.size(); ++i) {
                xsA[i] = xs.get(i);
            }

            for (int i = 0; i < ys.size(); ++i) {
                ysA[i] = ys.get(i);
            }
            JCuda.setExceptionsEnabled(true);
            int[] ids = CudaGdbscan.gdbscan(xsA, ysA, 2.0, 4);// 得到簇标识

            //int[] ids = new int[]{0,0,0,0,0,-1,0,0,0,0,0,-1,-1,0,-1,-1,-1,-1,0,0,-1,0};
            for (int i = 0; i < ids.length; i++) {
                System.out.println(xsA[i] + " " + ysA[i] + " " + ids[i]);
            }
        }
    }
}