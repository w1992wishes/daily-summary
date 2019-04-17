package me.w1992wishes.common.util;

import me.w1992wishes.common.exception.MyException;

import java.io.*;

/*
 * 替换文件（如果该文件含有子目录，则包括子目录所有文件）中某个字符串并写入新内容（Java代码实现）.
 *
 *原理：逐行读取源文件的内容，一边读取一边同时写一个*.tmp的文件。
 *当读取的行中发现有需要被替换和改写的目标内容时候，用新的内容替换之。
 *最终，删掉源文件，把*.tmp的文件重命名为源文件名字。
 *
 *注意！代码功能是逐行读取一个字符串，然后检测该字符串‘行’中是否含有替换的内容，有则新的内容替换待替换的内容。
 *
 * */
public class FileModifyUtils {

    private FileModifyUtils() {
    }

    public static void replace(String path, String target, String newContent) throws MyException {
        File file = new File(path);
        replaceDirectory(file, target, newContent);
    }

    public static void replaceDirectory(File dir, String target, String newContent) throws MyException {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory())
                // 如果是目录，则递归。
                replaceDirectory(f, target, newContent);
            if (f.isFile())
                replaceFile(f, target, newContent);
        }
    }

    public static void replaceFile(File file, String target, String newContent) throws MyException {

        try {
            InputStream is = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is));

            String filename = file.getName();
            // tmpfile为缓存文件，代码运行完毕后此文件将重命名为源文件名字。
            File tmpfile = new File(file.getParentFile().getAbsolutePath()
                    + File.separator + filename + ".tmp");

            BufferedWriter writer = new BufferedWriter(new FileWriter(tmpfile));

            boolean flag = false;
            String str = null;
            while (true) {
                str = reader.readLine();

                if (str == null)
                    break;

                if (str.contains(target)) {
                    String newStr = str.replace(target, newContent);
                    writer.write(newStr + "\n");

                    flag = true;
                } else
                    writer.write(str + "\n");
            }

            is.close();

            writer.flush();
            writer.close();

            if (flag) {
                file.delete();
                tmpfile.renameTo(new File(file.getAbsolutePath()));
            } else
                tmpfile.delete();
        } catch (Exception e) {
            throw new MyException("modify file failure", e);
        }
    }

    public static void main(String[] args) throws MyException {
        //代码测试：假设有一个test文件夹，test文件夹下含有若干文件或者若干子目录，子目录下可能也含有若干文件或者若干子目录（意味着可以递归操作）。
        //把test目录下以及所有子目录下（如果有）中文件含有"hi"的字符串行替换成新的"hello,world!"字符串行。
        FileModifyUtils.replace(".\\test", "hi", "hello,world!");
    }
}