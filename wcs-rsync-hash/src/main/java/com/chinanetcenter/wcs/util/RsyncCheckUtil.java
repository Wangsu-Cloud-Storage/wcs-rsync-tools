package com.chinanetcenter.wcs.util;

import com.chinanetcenter.api.entity.HttpClientResult;
import com.chinanetcenter.api.util.DateUtil;
import com.chinanetcenter.api.util.JsonMapper;
import com.chinanetcenter.wcs.base.dao.impl.FileMetaDao;
import com.chinanetcenter.wcs.base.util.DbUtil;
import com.chinanetcenter.wcs.pojo.ConfJson;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Created by lidl on 2015/3/9
 */
public class RsyncCheckUtil {
    private static Logger logger = Logger.getLogger(RsyncCheckUtil.class);

    public static ConfJson getConfJson(String confPath) {
        ConfJson confJson = null;
        BufferedReader reader = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(confPath);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            reader = new BufferedReader(inputStreamReader);
            String line;
            StringBuffer confJsonStr = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                confJsonStr.append(line.trim());
            }
            JsonMapper jsonMapper = JsonMapper.nonEmptyMapper();
            confJson = jsonMapper.fromJson(confJsonStr.toString(), ConfJson.class);
        } catch (IOException e) {
            logger.error("读取配置文件出错:", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return confJson;
    }

    public static void main(String[] args) {
        JsonMapper jsonMapper = JsonMapper.nonEmptyMapper();
        String s = "{\n" +
                "  \"accessKey\": \"72ced9249a1cb518b08a10e4719b8c4b4542c20a\",\n" +
                "  \"secretKey\": \"446edcd1048af4e94f634f2d162edf4a63f02ad2\"}";
        ConfJson confJson = jsonMapper.fromJson(s, ConfJson.class);
    }

    public static boolean confJsonValidate(ConfJson confJson) {
        if (StringUtils.isBlank(confJson.accessKey)) {
            logger.error("请设置accessKey");
            return false;
        }
        if (StringUtils.isBlank(confJson.secretKey)) {
            logger.error("请设置secretKey");
            return false;
        }
        if (StringUtils.isBlank(confJson.uploadDomain)) {
            logger.error("请设置uploadDomain");
            return false;
        }
        if (StringUtils.isBlank(confJson.mgrDomain)) {
            logger.error("请设置mgrDomain");
            return false;
        }
        if (confJson.sliceThreshold == null) {//取值范围 1-100M
            confJson.sliceThreshold = 4;//默认4M进行分片上传
        } else {
            if (confJson.sliceThreshold < 1 || confJson.sliceThreshold > 100) {
                logger.error("sliceThreshold取值范围:1-100，请检查");
                return false;
            }
        }
        if (confJson.threadNum == null) {//取值范围 1-100
            confJson.threadNum = 1;//默认1个线程
        } else {
            if (confJson.threadNum < 1 || confJson.threadNum > 100) {
                logger.error("threadNum取值范围:1-100，请检查");
                return false;
            }
        }
        if (confJson.countHashThreadNum == null) {//取值范围 1-100
            confJson.countHashThreadNum = 1;//默认1个线程
        } else {
            if (confJson.countHashThreadNum < 1 || confJson.countHashThreadNum > 100) {
                logger.error("countHashThreadNum取值范围:1-100，请检查");
                return false;
            }
        }
        if (confJson.compareHashThreadNum == null) {//取值范围 1-100
            confJson.compareHashThreadNum = 1;//默认1个线程
        } else {
            if (confJson.compareHashThreadNum < 1 || confJson.compareHashThreadNum > 100) {
                logger.error("compareHashThreadNum取值范围:1-100，请检查");
                return false;
            }
        }
        if (confJson.compareHashFileNum == null) {
            confJson.compareHashFileNum = 100;//默认一次性从服务器查询100个文件的hash
        } else {
            if (confJson.compareHashFileNum < 1 || confJson.compareHashFileNum > 2000) {
                logger.error("compareHashThreadNum取值范围:1-2000，请检查");
                return false;
            }
        }
        if (confJson.minFileSize == null) {
            confJson.minFileSize = 0L;//1024 配置项，单位为字节，默认为0，即不限制
        } else {
            if (confJson.minFileSize < 0) {
                logger.error("minFileSize取值范围:大于等于0，请检查");
                return false;
            }
        }
        if (confJson.overwrite == null) {
            confJson.overwrite = 1;//可配置值为0或者1，默认为1。1表示覆盖，其他值表示不覆盖
        } else {
            if (confJson.overwrite != 1 && confJson.overwrite != 0) {
                logger.error("overwrite取值范围:0或者1，请检查");
                return false;
            }
        }

        if (confJson.sliceThread == null) {//取值范围 1-10
            confJson.sliceThread = 5;//默认5个线程
        } else {
            if (confJson.sliceThread < 1 || confJson.sliceThread > 10) {
                logger.error("sliceThread取值范围:1-10，请检查");
                return false;
            }
        }
        if (confJson.sliceBlockSize == null) {//取值范围 4-32
            confJson.sliceBlockSize = 4;//默认4M
        } else {
            if (confJson.sliceBlockSize < 4 || confJson.sliceBlockSize > 32 || confJson.sliceBlockSize % 4 != 0) {
                logger.error("sliceBlockSize取值范围:4-32，且为4的倍数，请检查");
                return false;
            }
        }
        if (confJson.sliceChunkSize == null) {//取值范围 256-1024KB
            confJson.sliceChunkSize = 4096;//默认4M
        } else {
            if (confJson.sliceChunkSize < 256 || confJson.sliceChunkSize > 4096) {
                logger.error("sliceChunkSize取值范围:256-4096，请检查");
                return false;
            }
        }
        if (confJson.deletable == null || (confJson.deletable != 0 && confJson.deletable != 1)) {
            confJson.deletable = 0;
        }
        if (confJson.maxRate == null || confJson.maxRate <= 0) {//默认值1024*1024KB/s
            confJson.maxRate = 1024 * 1024;
        }
        if (confJson.taskBeginTime != null && confJson.taskEndTime != null) {
            confJson.setdTaskBeginTime(DateUtil.parseDate(confJson.taskBeginTime, DateUtil.TIME_PATTERN));
            confJson.setdTaskEndTime(DateUtil.parseDate(confJson.taskEndTime, DateUtil.TIME_PATTERN));
        }
        if (confJson.scanOnly == null) {
            confJson.scanOnly = 0;//可配置值为0或者1，默认为0。配置为1时，只扫描文件列表，记录修改时间，不计算hash，不对比hash，不上传文件
        } else {
            if (confJson.scanOnly != 1 && confJson.scanOnly != 0) {
                logger.error("scanOnly取值范围:0或者1，请检查");
                return false;
            }
        }
        if (confJson.isCompareHash == null) {//
            confJson.isCompareHash = 1;//默认配置为1.配置为1时，比对Hash上传；配置为0时,上传文件无需进行Hash计算可直接进行上传
        } else {
            if (confJson.isCompareHash != 1 && confJson.isCompareHash != 0) {
                logger.error("isCompareHash取值范围:0或者1，请检查");
                return false;
            }
        }
        if (confJson.isLastModifyTime == null) {
            confJson.isLastModifyTime = 0;//lastModifyTime配置项，可配置值为0或者1，默认为0。0表示不上传修改时间，1表示上传修改时间
        } else {
            if (confJson.isLastModifyTime != 1 && confJson.isLastModifyTime != 0) {
                logger.error("isLastModifyTime取值范围:0或者1，请检查");
                return false;
            }
        }
        if (confJson.logLevel == null) {//取值范围 ERROR、WARN、INFO、DEBUG
            confJson.logLevel = "debug";//默认debug
        } else {
            if (!StringUtils.equalsIgnoreCase(confJson.logLevel, "error") && !StringUtils.equalsIgnoreCase(confJson.logLevel, "warn") &&
                    !StringUtils.equalsIgnoreCase(confJson.logLevel, "info") && !StringUtils.equalsIgnoreCase(confJson.logLevel, "debug")) {
                logger.error("logLevel取值范围:error、warn、info、debug");
                return false;
            }
        }
        if (confJson.uploadErrorRetry == null) {
            confJson.uploadErrorRetry = 0;//默认
        } else {
            if (confJson.uploadErrorRetry < 0 || confJson.uploadErrorRetry > 5) {
                logger.error("uploadErrorRetry取值范围:0-5，请检查");
                return false;
            }
        }
        if (confJson.syncMode == null) {
            confJson.syncMode = 0;//默认值为0，表示单空间多目录的上传模式，1表示多空间多文件的上传模式
        } else {
            if (confJson.syncMode != 1 && confJson.syncMode != 0) {
                logger.error("syncMode取值范围:0或者1，请检查");
                return false;
            }
        }
        if (confJson.syncMode == 0) {
            if (StringUtils.isBlank(confJson.bucket)) {
                logger.error("请设置bucket");
                return false;
            }
            if (StringUtils.isBlank(confJson.syncDir)) {
                logger.error("请设置sync_dir");
                return false;
            }
            String[] synDirArr = StringUtils.split(confJson.syncDir, CharsetConstant.VERTICAL_LINE);
            for (String dir : synDirArr) {
                File dirFile = new File(dir);
                if (!dirFile.exists()) {
                    logger.error("syncDir目录不存在，请检查.同步路径:" + dir);
                    return false;
                }
                if (!dirFile.isDirectory()) {
                    logger.error("syncDir不是目录，请检查.同步路径:" + dir);
                    return false;
                }
            }
            for (int i = 0; i < synDirArr.length; i++) {
                for (int j = 0; j < synDirArr.length; j++) {
                    String normalDir = DirectoryPrefixUtil.getSingleNormalDirectory(synDirArr[i]);
                    String normalDir1 = DirectoryPrefixUtil.getSingleNormalDirectory(synDirArr[j]);
                    if (i != j && StringUtils.equals(normalDir, normalDir1)) {
                        logger.error("syncDir不能含有相同目录，请检查.同步路径:[" + synDirArr[i] + "],[" + synDirArr[j] + "]");
                        return false;
                    }
                }
            }
        } else {
            if (StringUtils.isBlank(confJson.bucketAndDir)) {
                logger.error("请设置bucketAndDir");
                return false;
            } else if (confJson.bucketAndDir.contains("\r")) {
                String[] bucketDirs = StringUtils.split(confJson.bucketAndDir, CharsetConstant.SEMICOLON);
                for (String bucketAndDir : bucketDirs) {
                    if (bucketAndDir.contains("\r")) {
                        bucketAndDir = bucketAndDir.replaceAll("\r", "\\\\r");
                        String msg = String.format("bucketAndDir值不符合规范，请检查，%s", bucketAndDir);
                        logger.error(msg);
                    }
                }
                return false;
            } else {
                String[] bucketDirs = StringUtils.split(confJson.bucketAndDir, CharsetConstant.SEMICOLON);
                for (String bucketAndDir : bucketDirs) {
                    if (bucketAndDir.contains("，")) {//中文逗号
                        logger.error("请使用英文分隔符！异常配置：" + bucketAndDir);
                        return false;
                    }
                    String[] bucketAndDirs = StringUtils.split(bucketAndDir, CharsetConstant.VERTICAL_LINE);
                    if (bucketAndDirs.length != 2 && bucketAndDirs.length != 3) {
                        logger.error("bucketAndDir值不符合规范，请检查，" + bucketAndDirs);
                        return false;
                    }
                    String dirs = bucketAndDirs[1];
                    String[] paths = dirs.split(",");
                    for (String path : paths) {
                        File file = new File(path);
                        if (!file.exists()) {
                            logger.error("路径不存在，请检查.同步路径:" + path);
                            return false;
                        }
                    }
                }
            }
        }

        HttpClientResultUtil.init(confJson);
        return true;
    }

    public static boolean tokenValidate() {
        boolean result = false;
        try {
            HttpClientResult httpClientResult = HttpClientResultUtil.filesyncResult();//验证AK,SK,bucket等合法性
            if (httpClientResult == null || httpClientResult.getStatus() != 200) {
                if (httpClientResult != null) {
                    logger.error(httpClientResult.getStatus() + "   " + httpClientResult.getResponse());//token等校验失败
                }
            } else {
                result = true;
            }
        } catch (Exception e) {
            logger.error("校验token失败:", e);
        }
        return result;
    }

    /**
     * log4j配置
     */
    public static Properties loadLog4jProperties() {
        //保留初始启动日志到特定位置
        String logsFilePath = System.getProperty("user.dir") + File.separator + "wcs-rsync-hash.log";
        boolean isWindows = Pattern.matches("^[A-z]:.*", logsFilePath);
        Properties props = new Properties();
        props.setProperty("log4j.rootLogger", "info,R");
//        props.setProperty("log4j.rootLogger", "info,R,stdout");
//        props.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
//        props.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
//        props.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%d{yyyy-MM-dd HH:mm:ss} - %p %l | %m%n");
        props.setProperty("log4j.appender.R", "org.apache.log4j.DailyRollingFileAppender");
        props.setProperty("log4j.appender.R.DatePattern", "'_'yyyy-MM-dd'.log'");
        props.setProperty("log4j.appender.R.Encoding", "UTF-8");
        //如果是windows环境保存到当前目录下  linux则保存到/tmp/wcs-rsync-hash-log/下
        props.setProperty("log4j.appender.R.File", isWindows?logsFilePath:"/tmp/wcs-rsync-hash-log/wcs-rsync-hash-"+System.currentTimeMillis()+".log");
        props.setProperty("log4j.appender.R.layout", "org.apache.log4j.PatternLayout");
        props.setProperty("log4j.appender.R.layout.ConversionPattern", "%d{yyyy-MM-dd HH:mm:ss} %p %l | %m%n");
        props.setProperty("log4j.logger.com", "debug");//默认是debug级别日志
        PropertyConfigurator.configure(props);// 装入log4j配置信息
        return props;
    }

    /**
     * 加载数据文件，overwrite为true表示忽略之前已经计算hash的数据，全部重新计算hash.
     */
    public static boolean loadDataBase(ConfJson confJson, boolean overwriteDb) {
        return loadDataBase(confJson.bucket, confJson.keyPrefix, confJson.syncDir, overwriteDb);
    }

    public static boolean loadDataBase(String bucket, String keyPrefix, String syncDir, boolean overwriteDb) {
        boolean result = false;
        try {
            String dbDir = getDBDirectory(bucket, keyPrefix, syncDir);
            //C:\Users\wangsu\.wrsync\
            String dbFilePath = System.getProperty("user.home") + File.separator + ".wcsrsynchash" + File.separator + dbDir + File.separator + "wcsrsynchash.db";
            logger.info("加载数据文件:" + dbFilePath);
            File dbFile = new File(dbFilePath);
            if (overwriteDb) {
                if (dbFile.exists()) {
                    dbFile.delete();
                }
            }
            FileUtil.createDirs(dbFile);
            DbUtil.dbFilePath = dbFilePath;
            if (!dbFile.exists()) {//数据库文件不存在，新建数据文件
                String createFileMetaSql = "CREATE TABLE file_meta (fileKey text PRIMARY KEY NOT NULL" +
                        ",fileSize bigint NOT NULL" +
                        ",fileMtime bigint NOT NULL" +
                        ",hash varchar(200) " +
                        ",updateTime varchar(30) NOT NULL" +
                        ",operate int NOT NULL" +
                        ",status int NOT NULL" +
                        ",compareStatus int NOT NULL" +
                        ",fileParentPath text NOT NULL" +
                        ",syncDir text NOT NULL" +
                        ");";
                FileMetaDao fileMetaDao = new FileMetaDao();
                fileMetaDao.executeUpdate(createFileMetaSql);
                FileMetaDao fileMetaIndexDao = new FileMetaDao();
                String createIndexSql = "CREATE INDEX fileParentPath_idx ON file_meta (fileParentPath)";
                fileMetaIndexDao.executeUpdate(createIndexSql);
            }
            result = true;
        } catch (Exception e) {
            logger.error("加载数据文件失败:", e);
        }
        return result;
    }

    public static String getDBDirectory(String bucket, String keyPrefix, String syncDir) {
        String normalSyncDir = DirectoryPrefixUtil.directoryNormal(syncDir);//目录统一标准化
        logger.info("规整后的标准目录:" + normalSyncDir);
        String[] normalSyncDirArr = StringUtils.split(normalSyncDir, CharsetConstant.VERTICAL_LINE);
        String[] keyPrefixArr = StringUtils.split(keyPrefix, CharsetConstant.VERTICAL_LINE);
        StringBuffer keyPrefixBuffer = new StringBuffer();
        for (int i = 0; i < normalSyncDirArr.length; i++) {
            if (keyPrefixArr != null && i < keyPrefixArr.length) {
                keyPrefixBuffer.append(keyPrefixArr[i]).append(CharsetConstant.VERTICAL_LINE);
            } else {
                keyPrefixBuffer.append(CharsetConstant.SLASH).append(CharsetConstant.VERTICAL_LINE);
            }
        }
        String content = bucket + ":" + keyPrefixBuffer.toString() + ":" + normalSyncDir;
        logger.info("md5前的内容:" + content);
        String dbDir = DigestUtils.md5Hex(content);
        logger.info("md5后的内容:" + dbDir);
        return dbDir;
    }
}
