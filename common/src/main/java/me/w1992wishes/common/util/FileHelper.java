package me.w1992wishes.common.util;

import me.w1992wishes.common.exception.MyRuntimeException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * 文件读取工具
 *
 * @author w1992wishes 2019/3/27 11:36
 */
public class FileHelper {

    private FileHelper(){}

    /**
     * 读取文件内容
     */
    public static String getFileContent(String fileSource) {
        File file = new File(fileSource);
        try {
            return FileUtils.readFileToString(file, "UTF-8");
        } catch (IOException e) {
            throw new MyRuntimeException(e);
        }
    }

}
