package com.chinanetcenter.wcs.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;

/**
 * 文件操作类
 * Created by xiexb on 2014/7/11.
 */
public class FileUtil {
    /**
     * *
     * 根据文件路径创建目录
     */
    public static synchronized boolean createDirs(File f) {
        boolean result = true;//正常会成功
        // 创建文件路径
        File parentFile = f.getParentFile();
        if (!parentFile.exists()) {
            result = parentFile.mkdirs();
        }
        return result;
    }

    public static Map getFileHashMap(String path, Map<String, String> fileHashMap) {
        File file = new File(path);
        return listFile(fileHashMap, file);
    }

    private static Map listFile(Map<String, String> fileHashMap, File f) {
        //获得当前路径下的所有文件和文件夹
        File[] allFiles = f.listFiles();
        if (allFiles != null) {
            //循环所有路径
            for (int i = 0; i < allFiles.length; i++) {
                //如果是文件夹
                if (allFiles[i].isDirectory()) {
                    //递归调用
                    listFile(fileHashMap, allFiles[i]);
                } else { //文件
                    fileHashMap.put(allFiles[i].getAbsolutePath(), null);
                }
            }
        }
        return fileHashMap;
    }

    /**
     * 判断文件是否存在
     */
    public static boolean file_exists(String path) throws Exception {
        if (path == null || path.length() == 0)
            return false;
        File f = new File(path);
        if (f.exists())
            return true;
        return false;
    }

    public static void create(String filePath) {
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(filePath, "rw");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
