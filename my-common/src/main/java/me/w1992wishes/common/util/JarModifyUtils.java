package me.w1992wishes.common.util;

import java.io.*;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * jarPath: jar包所在路径
 * jarFilePath: jar中想要修改文件所在的路径
 * regex：正则表达式
 * replacement：替换的字符串
 * 注意：Jar包内的jar包内的文件不适用！
 */
public class JarModifyUtils {

    public static void replace(String jarPath, String jarFilePath, String regex, String replacement) throws IOException {
        File file = new File(jarPath);
        JarFile jarFile = new JarFile(file);// 通过jar包的路径 创建Jar包实例
        replace(jarFile, jarFilePath, regex, replacement);
    }

    public static void replace(JarFile jarFile, String jarFilePath, String regex, String replacement) throws IOException {
        JarEntry entry = jarFile.getJarEntry(jarFilePath);//通过某个文件在jar包中的位置来获取这个文件
        //创建该文件输入流
        InputStream input = jarFile.getInputStream(entry);
        //获取entries集合lists
        List<JarEntry> lists = new LinkedList<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            lists.add(jarEntry);
        }
        String s = readFile(input, regex, replacement);// 读取并修改文件内容
        writeFile(lists, jarFilePath, jarFile, s);// 将修改后的内容写入jar包中的指定文件
        jarFile.close();
    }

    private static String readFile(InputStream input, String regex, String replacement)
            throws IOException {
        InputStreamReader isr = new InputStreamReader(input);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder buf = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            // 此处根据实际需要修改某些行的内容
            buf.append(line);
            buf.append(System.getProperty("line.separator"));
        }
        br.close();
        return buf.toString().replaceAll(regex, replacement);
    }

    private static void writeFile(List<JarEntry> lists, String jarFilePath,
                                 JarFile jarFile, String content) throws IOException {
        FileOutputStream fos = new FileOutputStream(jarFile.getName(), true);
        JarOutputStream jos = new JarOutputStream(fos);
        try {
            for (JarEntry je : lists) {
                if (je.getName().equals(jarFilePath)) {
                    // 将内容写入文件中
                    jos.putNextEntry(new JarEntry(jarFilePath));
                    jos.write(content.getBytes());
                } else {
                    //表示将该JarEntry写入jar文件中 也就是创建该文件夹和文件
                    jos.putNextEntry(new JarEntry(je));
                    jos.write(streamToByte(jarFile.getInputStream(je)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭流
            jos.close();
        }
    }

    private static byte[] streamToByte(InputStream inputStream) {
        ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outSteam.write(buffer, 0, len);
            }
            outSteam.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outSteam.toByteArray();
    }


    public static void main(String[] args) throws IOException {
        JarModifyUtils jarTool = new JarModifyUtils();
        jarTool.replace("D:\\IDEA\\workSpace\\demo.jar"
                , "spring/spring-aop.xml", "expression=\".*\"", "expression=\"%%\"");
    }

}