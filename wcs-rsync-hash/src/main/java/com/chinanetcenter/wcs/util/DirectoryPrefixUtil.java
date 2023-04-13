package com.chinanetcenter.wcs.util;

import com.chinanetcenter.wcs.pojo.ConfJson;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lidl on 2017/4/12.
 * <p>
 * 支持多路径上传，即syncDir支持配置多个路径，以|间隔[keyPrefix也支持多个，一一对应，没有对应的上的就是没有配prefix即可]。
 * keyPrefix也支持配置多个，与syncDir的路径一一对应，若有路径没有对应上，则以没有配置keyPrefix来处理即可。
 * 例如:
 * syncDir配置为D:/rsync1|D:/rsync2|D:/rsync3
 * keyPrefix配置为test1/|test2/
 * 则rsync1下的文件（文件夹）保存在云存储test1目录下，rsync2的文件（文件夹）保存在test2目录下，rsync3的文件（文件夹）保存在根目录下。若配置的keyPrefix多于syncDir，则多余的keyPrefix不生效，取前几个目录
 */
public class DirectoryPrefixUtil {

    /**
     * 目录统一标准化
     */
    public static String directoryNormal(String directorys) {
        if (directorys == null) return null;
        StringBuffer result = new StringBuffer();
        String[] dirArr = StringUtils.split(directorys, CharsetConstant.VERTICAL_LINE);
        for (String dir : dirArr) {
            String normalDirectory = getSingleNormalDirectory(dir);
            result.append(normalDirectory).append(CharsetConstant.VERTICAL_LINE);
        }
        return result.toString();
    }

    /**
     * 单个目录统一标准化
     */
    public static String getSingleNormalDirectory(String directory) {
//        if (StringUtils.startsWith(directory, "/")) {
//            directory = StringUtils.substringAfter(directory, "/");
//        }
        if (StringUtils.endsWith(directory, "/")) {
            directory = StringUtils.substringBeforeLast(directory, "/");
        }
        if (StringUtils.contains(directory, "//")) {
            directory = StringUtils.replace(directory, "//", "/");
        }
        if (StringUtils.endsWith(directory, "/") || StringUtils.contains(directory, "//")) {
            directory = getSingleNormalDirectory(directory);
        }
        return directory;
    }


    /**
     * 获取目录和前缀映射关系
     *
     * @return
     */
    public static Map<String, String> directoryRelateToPrefix(ConfJson confJson) {
        String directorys = confJson.syncDir;
        String prefixs = confJson.keyPrefix;
        Map<String, String> result = new HashMap();
        String[] dirArr = StringUtils.split(directorys, CharsetConstant.VERTICAL_LINE);
        String[] keyPrefixArr;
        if (confJson.syncMode == 1) {
            keyPrefixArr = StringUtils.split(prefixs, CharsetConstant.COMMA);
        } else {
            keyPrefixArr = StringUtils.split(prefixs, CharsetConstant.VERTICAL_LINE);
        }
        for (int i = 0; i < dirArr.length; i++) {
            File dir = new File(dirArr[i]);
            String filePath = transPath(dir.getAbsolutePath());
            String syncDir = filePath;
            if (new File(filePath).isDirectory()) {
                syncDir = filePath + "/";
            }
            if (keyPrefixArr != null && i < keyPrefixArr.length) {
                result.put(syncDir, keyPrefixArr[i]);
            } else {
                result.put(syncDir, CharsetConstant.NULL);
            }
        }
        return result;
    }

    public static String transPath(String path) {
        String transPath = path;
        if (OSInfoUtil.isWindows()) {//windows系统
            transPath = path.replaceAll("\\\\", "/");
        }
        return transPath;
    }

    public static void main(String[] args) {
        String directory = "D:////111/2///22////";
        String str = getSingleNormalDirectory(directory);
        System.out.println(str);
        String directorys = "D:////111/2///22////|D:////111/2///22////";
        String str1 = directoryNormal(directorys);
        System.out.println(str1);

        System.out.println("=======================================");
        String keyPrefix = "test1/|test2/|/|/|test3/";
        String syncDir = directoryNormal(directorys);//目录统一标准化
        System.out.println("规整后的标准目录:" + syncDir);
        String[] syncDirArr = StringUtils.split(syncDir, CharsetConstant.VERTICAL_LINE);
        System.out.println("syncDirArr.length:" + syncDirArr.length);
        String[] keyPrefixArr = StringUtils.split(keyPrefix, CharsetConstant.VERTICAL_LINE);
        StringBuffer keyPrefixBuffer = new StringBuffer();
        for (int i = 0; i < syncDirArr.length; i++) {
            if (keyPrefixArr != null && i < keyPrefixArr.length) {
                keyPrefixBuffer.append(keyPrefixArr[i]).append(CharsetConstant.VERTICAL_LINE);
            } else {
                keyPrefixBuffer.append(CharsetConstant.SLASH).append(CharsetConstant.VERTICAL_LINE);
            }
        }
        String content = keyPrefixBuffer.toString() + ":" + syncDir;
        System.out.println("进行md5的内容:" + content);

    }
}
