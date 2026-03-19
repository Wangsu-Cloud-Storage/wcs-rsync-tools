package com.chinanetcenter.wcs.core;

import com.chinanetcenter.api.util.DateUtil;
import com.chinanetcenter.api.util.JsonMapper;
import com.chinanetcenter.wcs.base.util.SqliteDbUtil;
import com.chinanetcenter.wcs.constant.RsyncConstant;
import com.chinanetcenter.wcs.constant.SyncOperateEnum;
import com.chinanetcenter.wcs.pojo.ConfJson;
import com.chinanetcenter.wcs.pojo.FileMeta;
import com.chinanetcenter.wcs.util.CharsetConstant;
import com.chinanetcenter.wcs.util.DirectoryPrefixUtil;
import com.chinanetcenter.wcs.util.HttpClientResultUtil;
import com.chinanetcenter.wcs.util.RsyncCheckUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * 文件同步处理类
 * Created by lidl on 2015/3/9.
 */
public class RsyncCore {
    public static ConfJson confJson;
    public static ConcurrentLinkedQueue<FileMeta> fileMetaAddQueue = new ConcurrentLinkedQueue<FileMeta>();
    public static ConcurrentLinkedQueue<FileMeta> fileMetaUpdateQueue = new ConcurrentLinkedQueue<FileMeta>();
    public static ConcurrentLinkedQueue<Object[]> fileMetaDeleteQueue = new ConcurrentLinkedQueue<Object[]>();
    public static boolean countHashOver = false;//文件hash计算结束后，元数据已经全部持久化到Sqlite
    public static boolean compareHashOver = false;//文件hash计算结束后，元数据已经全部持久化到Sqlite
    public static volatile boolean over = false;//控制进程结束(场景1：本地文件hash计算结束；场景2：比对文件hash结束。)
    public static CountDownLatch countDownLatch = new CountDownLatch(1);
    public static Map<String, String> directoryPrefixMap = new HashMap<String, String>();
    private static Logger logger = Logger.getLogger(RsyncCore.class);
    private static LinkedList<File> directoryList = new LinkedList<File>();
    private static ExecutorService dbPool;
    private static ThreadPoolExecutor caculateHashPool;//计算hash的线程池
    private static ThreadPoolExecutor compareHashPool;//比对hash的线程池
    private static ThreadPoolExecutor rsyncPool;
    private static boolean tokenChecked = false;

    public static void fileSync(ConfJson paramConfJson, final boolean overwriteDB) {
        confJson = paramConfJson;
        if (confJson.syncMode == 0) {
            fileSync0(paramConfJson);
        } else if (confJson.syncMode == 1) {
            multiBucketsOperation(overwriteDB, new Operation() {
                @Override
                public void doAction(ConfJson confJson) {
                    fileSync0(confJson);
                }
            });
        }
    }

    interface Operation {
        void doAction(ConfJson confJson);
    }

    public static void multiBucketsOperation(boolean overwriteDB, Operation task) {
        String[] bucketDirs = StringUtils.split(confJson.bucketAndDir, CharsetConstant.SEMICOLON);
            for (String bucketAndDir : bucketDirs) {
            String[] bucketAndDirs = StringUtils.split(bucketAndDir, CharsetConstant.VERTICAL_LINE);
            if (bucketAndDirs.length != 2 && bucketAndDirs.length != 3) {
                logger.error("bucketAndDir值不符合规范，请检查");
                return;
            } else {
                String bucket = bucketAndDirs[0];
                String syncDirs = bucketAndDirs[1];
                String keyPrefix = bucketAndDirs.length == 3 ? bucketAndDirs[2] : "";
                syncDirs = syncDirs.replaceAll(",", "|");
                //                String[] syncDirArr = syncDirs.split(",");
                confJson.bucket = bucket;
                confJson.syncDir = syncDirs;
                confJson.keyPrefix = keyPrefix;
                if (!RsyncCheckUtil.loadDataBase(bucket, keyPrefix, syncDirs, overwriteDB)) {
                    logger.error(String.format("同步失败 bucket:%s syncDir:%s keyPrefix:%s", bucket, keyPrefix, syncDirs));
                    return;
                }
                HttpClientResultUtil.init(confJson);
                if (!tokenChecked) {
                    tokenChecked = true;
                    if (!RsyncCheckUtil.tokenValidate()) {//Token校验
                        throw new RuntimeException("校验token失败");
                    }
                }
                task.doAction(confJson);
            }
        }
    }

    private static void fileSync0(ConfJson paramConfJson) {
        try {
            confJson = paramConfJson;
            //========================== 文件和前缀映射关系============================
            directoryPrefixMap = DirectoryPrefixUtil.directoryRelateToPrefix(confJson);
            JsonMapper jsonMapper = new JsonMapper();
            logger.info("文件和前缀映射关系:" + jsonMapper.toJson(directoryPrefixMap));
            //========================================================================
            calculateHash();//计算Hash
            countDownLatch.await();
            countDownLatch = new CountDownLatch(1);
            compareHash();//比对文件的Hash
            countDownLatch.await();
            rsync();//根据Sqlite中文件记录的状态上传对应的文件
        } catch (Exception e) {
            logger.error("文件同步时，出错:", e);
        } finally {

        }
    }

    /**
     * 同步文件
     * 同步文件的前置条件：
     * 1.hash比对成功。compareStatus=2
     * 2.未操作(初始状态) status=2
     * 3.operate不为初始状态。operate!=0
     */
    public static void rsync() {
        over = false;
        fileSyncThreadPoolInit();
        logger.info("比对完Hash后,同步文件...BEGIN");
        long startTime = System.currentTimeMillis();
        try {
            List<FileMeta> fileMetaList = SqliteDbUtil.getRsyncPagingData(2, 2, 0, 1000);//从Sqlite中查询Hash比对成功，未曾同步过的且operate!=0的记录1000条
            if (fileMetaList == null || fileMetaList.size() == 0) {//Sqlite没有要同步的文件
                logger.info("Sqlite没有需要同步的文件，文件同步结束。");
            }
            while (fileMetaList != null && fileMetaList.size() > 0) {
                for (FileMeta fileMeta : fileMetaList) {
                    fileMeta.setStatus(3);
                }
                SqliteDbUtil.batchUpdate(fileMetaList);//将数据更新为同步中
                for (FileMeta fileMeta : fileMetaList) {
                    //存在数据库中的fileKey有包含配置文件中的前缀
                    String keyPrefix = RsyncCore.directoryPrefixMap.get(fileMeta.getSyncDir());
                    if (keyPrefix == null) {
                        JsonMapper jsonMapper = new JsonMapper();
                        logger.info("文件和前缀映射关系:" + jsonMapper.toJson(RsyncCore.directoryPrefixMap) + ",fileSyncDir=" + fileMeta.getSyncDir());
                        continue;
                    }
                    String fileKey = StringUtils.equals(CharsetConstant.NULL, keyPrefix) ? fileMeta.getFileKey() : fileMeta.getFileKey().substring(keyPrefix.length());
                    File dir = new File(fileMeta.getSyncDir());
                    String filePath = DirectoryPrefixUtil.transPath(dir.getAbsolutePath());
                    File rsynFile;
                    if (dir.isDirectory()) {
                        String syncDir = filePath + "/";
                        rsynFile = new File(syncDir + fileKey);
                        logger.debug("同步文件本地的路径:" + syncDir + fileKey);
                    } else {
                        rsynFile = new File(filePath);
                        logger.debug("同步文件本地的文件:" + filePath);
                    }
                    rsyncPool.execute(new RsyncTask(fileMeta, rsynFile, fileMeta.getOperate(), confJson.uploadErrorRetry, StringUtils.equals(confJson.getIsSkip406(),"1")));
                }
                fileMetaList = SqliteDbUtil.getRsyncPagingData(2, 2, 0, 1000);//从Sqlite中查询Hash比对成功，未曾同步过的且operate!=0的记录1000条
                if (fileMetaList == null || fileMetaList.size() == 0) {//Sqlite没有要同步的文件
                    logger.info("Sqlite没有需要同步的文件，文件同步结束。");
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("比对完Hash后,同步文件，出错:", e);
        } finally {
            rsyncPool.shutdown();
            while (!rsyncPool.isTerminated()) {
                try {
                    rsyncPool.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error("关闭rsyncPool出错:", e);
                }
            }
            over = true;//比对完Hash,同步完文件后控制SqliteTask任务结束
            dbPool.shutdown();
            while (!dbPool.isTerminated()) {
                try {
                    dbPool.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error("rsync 关闭dbPool出错:", e);
                }
            }
            logger.info("比对完Hash后,同步文件结束并将同步状态存入Sqlite数据库结束,耗时：" + (System.currentTimeMillis() - startTime) + "毫秒...END");
        }
    }

    /**
     * 进行Hash比对
     * 需要进行比对的文件：
     * 1.文件未曾比对过即compareStatus=0
     * 2.文件不是必须删除的文件即operate!=3
     * <p>
     * 每次取Sqlite中100条记录到服务器拿对应文件的hash。
     * 	若该文件在服务器上找不到，更新记录的状态为add.
     * 	若该文件在服务器上找到但hash不一样，更新记录的状态为update.
     * 	若该文件在服务器上找到且hash一样,记录状态更新为Not Upload。
     */
    public static void compareHash() {
        over = false;
        compareHashThreadPoolInit();//初始化比对Hash需要用到的线程池
        logger.info("开始比对本地文件Hash与服务器上文件Hash的差异...BEGIN");
        long startTime = System.currentTimeMillis();
        try {
            long startTime1 = System.currentTimeMillis();
            List<FileMeta> fileMetaList = SqliteDbUtil.getCompareHashPagingData(3, 0, confJson.compareHashFileNum);//从Sqlite中查询compareStatus=0(hash未比对)且operate!=3的记录100条
            RsyncConstant.compareHashselectTime += (System.currentTimeMillis() - startTime1);
            if (fileMetaList == null || fileMetaList.size() == 0) {//Sqlite没有需要比对的文件
                logger.info("Sqlite没有需要比对的文件，Hash比对结束。");
            }
            while (fileMetaList != null && fileMetaList.size() > 0) {
                for (FileMeta fileMeta : fileMetaList) {
                    fileMeta.setCompareStatus(1);
                }
                long startTime2 = System.currentTimeMillis();
                SqliteDbUtil.batchUpdate(fileMetaList);//将数据更新为比对中
                RsyncConstant.compareHashselectTime += (System.currentTimeMillis() - startTime2);
                compareHashPool.execute(new CompareTask(fileMetaList));
                long startTime3 = System.currentTimeMillis();
                fileMetaList = SqliteDbUtil.getCompareHashPagingData(3, 0, confJson.compareHashFileNum);//从Sqlitel中查询compareStatus=0(hash未比对)的记录100条
                RsyncConstant.compareHashselectTime += (System.currentTimeMillis() - startTime3);
                if (fileMetaList == null || fileMetaList.size() == 0) {//Sqite没有需要比对的文件
                    logger.info("Sqlite没有需要比对的文件，Hash比对结束。");
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("比对文件Hash时，出错:", e);
        } finally {
            compareHashPool.shutdown();
            while (!compareHashPool.isTerminated()) {
                try {
                    compareHashPool.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error("关闭compareHashPool出错:", e);
                }
            }
            over = true;//比对文件hash结束后控制SqliteTask任务结束
            dbPool.shutdown();
            while (!dbPool.isTerminated()) {
                try {
                    dbPool.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error("compareHash 关闭dbPool出错:", e);
                }
            }
            compareHashOver = true;
            if (over && compareHashOver) {//hash比对完毕且已经将相应的元数据信息存入Sqlite
                countDownLatch.countDown();
            }
            logger.info("比对文件Hash结束并将比对状态存入Sqlite数据库结束,耗时：" + (System.currentTimeMillis() - startTime) + "毫秒...END");
        }
    }

    /**
     * 计算Hash
     * 工具启动，扫描本地磁盘中的每个文件。默认上传状态Not Upload.
     * 若Sqlite中不存在该文件的记录，计算此文件的hash,并将该记录持久化到Sqlite中。
     * 若Sqlite中存在该文件记录但是文件lastModified时间跟Sqlite中记录的时间不一样。重新计算hash，持久化到Sqlite.
     * 若Sqlite中存在该文件记录且文件lastModified时间跟Sqlite中记录的时间一样。不需重新计算Hash
     */
    public static void calculateHash() {
        over = false;
        caculateHashThreadPoolInit();//初始化计算Hash需要用的线程池
        logger.info("开始计算本地文件的Hash并存入Sqlite数据库...BEGIN");
        long startTime = System.currentTimeMillis();
        try {
            LinkedList<String> fileParentPathList = SqliteDbUtil.getFileParentPathList();//获取所有文件结构
            Iterator<String> syncDirIterator = RsyncCore.directoryPrefixMap.keySet().iterator();
            while (syncDirIterator.hasNext()) {//循环扫描文件
                String syncDirTemp = syncDirIterator.next();
                logger.info("开始扫描文件[" + syncDirTemp + "]...BEGIN");
                String keyPrefix = RsyncCore.directoryPrefixMap.get(syncDirTemp);
                File dir = new File(syncDirTemp);
                String filePath = DirectoryPrefixUtil.transPath(dir.getAbsolutePath());
                File file = new File(filePath);
                String syncDir = filePath;
                if (file.isDirectory()) {
                    syncDir = filePath + "/";
                    scanDirNoRecursion(dir, filePath, syncDir, keyPrefix);//层级遍历计算本地文件的hash
                } else {
                    Map<String, FileMeta> fileMetaMap = SqliteDbUtil.getFileMetaMap(filePath);
                    fileCompare(filePath, filePath, keyPrefix, fileMetaMap, file);
                    deleteCheck(fileMetaMap);
                }
                fileParentPathList.remove(filePath);
                File tmpDir;
                while (!directoryList.isEmpty()) {
                    tmpDir = directoryList.removeFirst();//遍历下一个子文件
                    filePath = DirectoryPrefixUtil.transPath(tmpDir.getAbsolutePath());
                    scanDirNoRecursion(tmpDir, filePath, syncDir, keyPrefix);
                    fileParentPathList.remove(filePath);
                }
                logger.info("结速扫描文件[" + syncDirTemp + "]...END");
            }

            for (String fileParentPath : fileParentPathList) {//删除文件的情况
                deleteCheck(SqliteDbUtil.getFileMetaMap(fileParentPath));
            }
        } catch (Exception e) {
            logger.error("计算文件Hash时，出错:", e);
        } finally {
            caculateHashPool.shutdown();
            while (!caculateHashPool.isTerminated()) {
                try {
                    caculateHashPool.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error("关闭caculateHashPool出错:", e);
                }
            }
            over = true;//本地文件hash计算结束后控制SqliteTask任务结束
            dbPool.shutdown();
            while (!dbPool.isTerminated()) {
                try {
                    dbPool.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error("calculateHash 关闭dbPool出错:", e);
                }
            }
            countHashOver = true;
            if (over && countHashOver) {//hash计算完毕且已经将相应的元数据信息存入Sqlite
                countDownLatch.countDown();
            }
            logger.info("计算本地文件的Hash并存入Sqlite数据库结束,耗时：" + (System.currentTimeMillis() - startTime) + "毫秒...END");
        }
    }


    /**
     * 层级扫描文件
     */
    private static void scanDirNoRecursion(File dir, String fileParentPath, String syncDir, String keyPrefix) throws Exception {
        Map<String, FileMeta> fileMetaMap = SqliteDbUtil.getFileMetaMap(fileParentPath);
        File[] subfile = dir.listFiles();
        if (subfile != null) {
            for (int i = 0; i < subfile.length; i++) {
                if (subfile[i].isDirectory()) {//文件的话加入列表
                    directoryList.add(subfile[i]);
                } else {//文件
                    fileCompare(fileParentPath, syncDir, keyPrefix, fileMetaMap, subfile[i]);
                }
            }
            deleteCheck(fileMetaMap);
        } else {
            logger.error("文件路径非法.[" + dir.getAbsolutePath() + "]");
        }
    }

    private static void deleteCheck(Map<String, FileMeta> fileMetaMap) {
        if (!fileMetaMap.isEmpty()) {//本地文件没有,数据库有
            for (FileMeta fileMeta : fileMetaMap.values()) {
                String formatDate = DateUtil.formatDate(new Date(), com.chinanetcenter.wcs.util.DateUtil.COMMON_PATTERN);
                fileMeta.setOperate(SyncOperateEnum.delete.getValue());//标明数据要删除
                fileMeta.setStatus(2);//将状态置为未操作2(初始状态)
                fileMeta.setCompareStatus(2);//hash比对状态置为比对成功，因为文件要删除就不需要比对hash
                fileMeta.setUpdateTime(formatDate);
                logger.debug("Sqlite中比本地多出文件[" + fileMeta.getFileKey() + "]信息,标明需要删除。");
                fileMetaUpdateQueue.add(fileMeta);
                RsyncConstant.delAllNum.getAndIncrement();
            }
        }
    }

    private static void fileCompare(String fileParentPath, String syncDir, String keyPrefix, Map<String, FileMeta> fileMetaMap, File subfile) {
        RsyncConstant.localFileAllNum.getAndIncrement();//本地磁盘总共文件个数
        String formatDate = DateUtil.formatDate(new Date(), DateUtil.COMMON_PATTERN);
        String fileKey;
        if (keyPrefix == null || StringUtils.equals(CharsetConstant.NULL, keyPrefix)) {
            fileKey = getFileKey(subfile.getAbsolutePath(), syncDir);
        } else {
            fileKey = keyPrefix + getFileKey(subfile.getAbsolutePath(), syncDir);
        }
        long fileSize = subfile.length();
        long lastModified = subfile.lastModified();
        if (fileSize < RsyncCore.confJson.minFileSize && RsyncCore.confJson.minFileSize != 0) {//小于规定大小的文件不进行上传操作
            logger.info("文件[" + fileKey + "][fileSize:" + fileSize + "字节]小于规定的大小[minFileSize:" + RsyncCore.confJson.minFileSize + "字节]不进行上传");
            RsyncConstant.notUploadFileNum_less_minFileSize.getAndIncrement();
        } else {//minFileSize不做限制，或者文件大小大于minFileSize
            FileMeta fileMetaTemp = fileMetaMap.get(fileKey);
            if (RsyncCore.confJson.scanOnly == 1) {
                FileMeta fileMeta = new FileMeta(fileKey, fileSize, lastModified, fileParentPath, formatDate, 0, SyncOperateEnum.add.getValue(), "", 2, syncDir);
                if (fileMetaTemp == null) {
                    fileMetaAddQueue.add(fileMeta);
                } else {
                    fileMetaUpdateQueue.add(fileMeta);
                }
            } else {
                if (confJson.isCompareHash == 0) {//不比对Hash直接上传
                    FileMeta fileMeta = new FileMeta(fileKey, fileSize, lastModified, fileParentPath, formatDate, 2, SyncOperateEnum.add.getValue(), null, 2, syncDir);
                    if (fileMetaTemp == null) {
                        logger.info("源文件路径[" + syncDir + "]下fileKey:" + fileKey + ",[SqliteDb not find。不比对Hash直接上传]");
                        fileMetaAddQueue.add(fileMeta);
                    } else {
                        if (fileMetaTemp.getFileMtime().longValue() != lastModified) {
                            logger.info("源文件路径[" + syncDir + "]下fileKey:" + fileKey + ",[Sqlite中lastModified与文件不一致。不比对Hash直接上传]");
                            fileMetaUpdateQueue.add(fileMeta);
                        } else {
                            if (fileMetaTemp.getStatus() != 0) {
                                logger.info("源文件路径[" + syncDir + "]下fileKey:" + fileKey + ",[Sqlite中lastModified与文件一致但上传失败。不比对Hash直接上传]");
                                fileMetaUpdateQueue.add(fileMeta);
                            } else {
                                int operate = fileMetaTemp.getOperate().intValue();
                                String operateName = SyncOperateEnum.get(operate).name();
                                RsyncConstant.notUploadFileNum_alreadyUpload.getAndIncrement();
                                logger.info("源文件路径[" + syncDir + "]下fileKey:" + fileKey + ",[Sqlite中lastModified与文件一致且上传成功。不需重复" + operateName + "]");
                            }
                        }
                    }
                } else {
                    if (fileMetaTemp == null) {
                        logger.debug("源文件路径[" + syncDir + "]下fileKey:" + fileKey + ",SqliteDb not find");
                        RsyncConstant.hashCompareFilesNum.getAndIncrement();
                        caculateHashPool.execute(new CalculateHashTask(fileKey, fileSize, lastModified, fileParentPath, subfile, fileMetaAddQueue, syncDir));
                    } else {
                        if (fileMetaTemp.getFileMtime().longValue() != lastModified) {
                            RsyncConstant.hashCompareFilesNum.getAndIncrement();
                            caculateHashPool.execute(new CalculateHashTask(fileKey, fileSize, lastModified, fileParentPath, subfile, fileMetaUpdateQueue, syncDir));
                        } else {
                            FileMeta fileMeta = null;
                            int compareStatus = fileMetaTemp.getCompareStatus().intValue();
                            int operate = fileMetaTemp.getOperate().intValue();
                            int status = fileMetaTemp.getStatus().intValue();
                            String hash = fileMetaTemp.getHash();
                            if (compareStatus == 2 && operate == 0) {
                                RsyncConstant.notUploadFileNum_notUpload_last.getAndIncrement();
                                fileMeta = new FileMeta(fileKey, fileSize, lastModified, fileParentPath, formatDate, 0, operate, hash, 2, syncDir);
                                logger.debug("源文件路径[" + syncDir + "]下fileKey:" + fileKey + ",Hash:" + hash + ",[1.Sqlite中lastModified与文件一致不需要计算hash;2:hash比对成功,不需重复比对Hash;3:文件在服务器上已经存在不需上传]");
                            } else if (compareStatus == 2 && operate != 0 && status == 0) {//Sqlite中存在Hash比对成功，文件操作成功记录
                                RsyncConstant.notUploadFileNum_alreadyUpload.getAndIncrement();
                                String operateName = SyncOperateEnum.get(operate).name();
                                logger.debug("源文件路径[" + syncDir + "]下fileKey:" + fileKey + ",Hash:" + hash + ",[1.Sqlite中lastModified与文件一致不需要计算hash;2:hash比对成功,不需重复比对Hash;3:" + operateName + "成功,不需要重复" + operateName + "]");
                                fileMetaMap.remove(fileKey);
                                return;
                            } else if (compareStatus == 2 && operate != 0 && status == 1) {//Sqlite中存在Hash比对成功，文件操作失败记录
                                String operateName = SyncOperateEnum.get(operate).name();
                                fileMeta = new FileMeta(fileKey, fileSize, lastModified, fileParentPath, formatDate, 2, operate, hash, 2, syncDir);
                                logger.debug("源文件路径[" + syncDir + "]下fileKey:" + fileKey + ",Hash:" + hash + ",[1.Sqlite中lastModified与文件一致不需要计算hash;2:hash比对成功,不需重复比对Hash;3:" + operateName + "失败,需要重复" + operateName + "]");
                            } else if (compareStatus == 2 && operate != 0 && status == 2) {//Sqlite中存在Hash比对成功，未曾操作过
                                String operateName = SyncOperateEnum.get(operate).name();
                                fileMeta = new FileMeta(fileKey, fileSize, lastModified, fileParentPath, formatDate, 2, operate, hash, 2, syncDir);
                                logger.debug("源文件路径[" + syncDir + "]下fileKey:" + fileKey + ",Hash:" + hash + ",[1.Sqlite中lastModified与文件一致不需要计算hash;2:hash比对成功,不需重复比对Hash;3:" + operateName + "未曾操作过,需要重复" + operateName + "]");
                            } else if (compareStatus == 2 && operate != 0 && status == 3) {//Sqlite中存在Hash比对成功，上次客户端异常终止，导致status停留在处理中状态
                                String operateName = SyncOperateEnum.get(operate).name();
                                fileMeta = new FileMeta(fileKey, fileSize, lastModified, fileParentPath, formatDate, 2, operate, hash, 2, syncDir);
                                logger.debug("源文件路径[" + syncDir + "]下fileKey:" + fileKey + ",Hash:" + hash + ",[1.Sqlite中lastModified与文件一致不需要计算hash;2:hash比对成功,不需重复比对Hash;3:" + operateName + "上次客户端异常终止,需要重复" + operateName + "]");
                            } else if (compareStatus == 1) {//Hash比对中，客户端异常终止,初始化compareStatus,operate,status
                                RsyncConstant.hashCompareFilesNum.getAndIncrement();
                                fileMeta = new FileMeta(fileKey, fileSize, lastModified, fileParentPath, formatDate, 2, SyncOperateEnum.notUpload.getValue(), hash, 0, syncDir);
                                logger.debug("源文件路径[" + syncDir + "]下fileKey:" + fileKey + ",Hash:" + hash + ",[1:Sqlite中lastModified与文件一致不需要计算hash;2:Hash比对中，客户端异常终止,需要重复比对Hash;初始化compareStatus,operate,status]");
                            } else if (compareStatus == 0) {//Hash未曾比对,初始化compareStatus,operate,status
                                RsyncConstant.hashCompareFilesNum.getAndIncrement();
                                fileMeta = new FileMeta(fileKey, fileSize, lastModified, fileParentPath, formatDate, 2, SyncOperateEnum.notUpload.getValue(), hash, 0, syncDir);
                                logger.debug("源文件路径[" + syncDir + "]下fileKey:" + fileKey + ",Hash:" + hash + ",[1:Sqlite中lastModified与文件一致不需要计算hash;2:Hash未曾比对过,需要比对Hash;初始化compareStatus,operate,status]");
                            } else {//Hash比对失败,初始化compareStatus,operate,status
                                RsyncConstant.hashCompareFilesNum.getAndIncrement();
                                fileMeta = new FileMeta(fileKey, fileSize, lastModified, fileParentPath, formatDate, 2, SyncOperateEnum.notUpload.getValue(), hash, 0, syncDir);
                                logger.debug("源文件路径[" + syncDir + "]下fileKey:" + fileKey + ",Hash:" + hash + ",[1:Sqlite中lastModified与文件一致不需要计算hash;2:hash比对失败,初始化compareStatus,operate,status]");
                            }
                            fileMetaUpdateQueue.add(fileMeta);
                        }
                    }
                }

            }
        }
        fileMetaMap.remove(fileKey);
    }


    private static String getFileKey(String fileAbsolutePath, String syncDir) {
        String transPath = DirectoryPrefixUtil.transPath(fileAbsolutePath);
        File file = new File(syncDir);
        if (!file.isDirectory()) {
            syncDir = DirectoryPrefixUtil.transPath(file.getParentFile().getAbsolutePath()) + "/";
        }
        String fileKey = StringUtils.substringAfter(transPath, syncDir);
        return fileKey;
    }

    public static boolean excuteTask() {
        boolean excuteTask = true;
        if (confJson.dTaskBeginTime != null && confJson.dTaskEndTime != null) {
            Date formatNowDate = DateUtil.parseDate(DateUtil.formatDate(new Date(), DateUtil.TIME_PATTERN), DateUtil.TIME_PATTERN);

            if (confJson.dTaskBeginTime.after(confJson.dTaskEndTime)) {//都已经转为1970年1月1号的时间进行判断 20:00:00-03:00:00格式的情况，开始时间比结束时间大
                if (formatNowDate.after(confJson.dTaskBeginTime) || formatNowDate.before(confJson.dTaskEndTime)) {
                    excuteTask = true;
                } else {
                    excuteTask = false;
                }
            } else if (confJson.dTaskBeginTime.before(confJson.dTaskEndTime)) {//03:00:00-20:00:00的情况，开始时间比结束时间小
                if (formatNowDate.after(confJson.dTaskBeginTime) && formatNowDate.before(confJson.dTaskEndTime)) {
                    excuteTask = true;
                } else {
                    excuteTask = false;
                }
            }
        }
        return excuteTask;
    }

    private static void caculateHashThreadPoolInit() {
        logger.info("开始初始化计算Hash需要的线程池...");
        /**
         * 定义一个单一线程池，计算完hash后用于将数据持久化到数据库
         */
        dbPool = Executors.newSingleThreadExecutor();
        dbPool.execute(new SqliteTask());

        /***
         * 用于计算hash
         * 定义一个核心线程数为countHashThreadNum，等待线程数为200的线程池，当文件数超出threadNum+200时，将阻塞,
         */
        caculateHashPool = new ThreadPoolExecutor(confJson.countHashThreadNum, confJson.countHashThreadNum, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(200), new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                if (!executor.isShutdown()) {
                    try {
                        executor.getQueue().put(r);//这里将阻塞
                    } catch (InterruptedException e) {
                        // should not be interrupted
                    }
                }
            }
        });
    }

    private static void compareHashThreadPoolInit() {
        logger.info("开始初始化比对Hash需要的线程池...");
        /**
         * 定义一个单一线程池，用于比对hash时，元数据信息发生改变，持久化到Sqlite
         */
        dbPool = Executors.newSingleThreadExecutor();
        dbPool.execute(new SqliteTask());
        /***
         * 用于比对hash
         * 定义一个核心线程数为compareHashThreadNum，等待线程数为200的线程池，当文件数超出threadNum+200时，将阻塞,
         */
        compareHashPool = new ThreadPoolExecutor(confJson.compareHashThreadNum, confJson.compareHashThreadNum, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(200), new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                if (!executor.isShutdown()) {
                    try {
                        executor.getQueue().put(r);//这里将阻塞
                    } catch (InterruptedException e) {
                        // should not be interrupted
                    }
                }
            }
        });
    }

    private static void fileSyncThreadPoolInit() {
        logger.info("开始初始化同步文件需要的线程池...");
        /**
         * 定义一个单一线程池，用于同步文件时，元数据信息发生改变，持久化到Sqlite
         */
        dbPool = Executors.newSingleThreadExecutor();
        dbPool.execute(new SqliteTask());
        /***
         * 用于同步文件
         * 定义一个核心线程数为threadNum，等待线程数为200的线程池，当文件数超出threadNum+200时，将阻塞,
         */
        rsyncPool = new ThreadPoolExecutor(confJson.threadNum, confJson.threadNum, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(200), new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                if (!executor.isShutdown()) {
                    try {
                        executor.getQueue().put(r);//这里将阻塞
                    } catch (InterruptedException e) {
                        // should not be interrupted
                    }
                }
            }
        });
    }

    public static void listfailed(ConfJson confJson, final String logFilePath) {
        RsyncCore.confJson = confJson;
        if (RsyncCore.confJson.syncMode == 0) {
            listfailed0(logFilePath, null);
        } else {
            multiBucketsOperation(false, new Operation() {
                @Override
                public void doAction(ConfJson confJson) {
                    listfailed0(logFilePath, confJson);
                }
            });
        }
    }

    private static void listfailed0(String logFilePath, ConfJson confJson) {
        BufferedWriter bw = null;
        try {
            if (confJson != null) {
                bw = new BufferedWriter(new FileWriter(confJson.bucket + "-" + DigestUtils.md5Hex(confJson.syncDir + confJson.keyPrefix) + logFilePath));
            } else {
                bw = new BufferedWriter(new FileWriter(logFilePath));
            }
            List<FileMeta> hashCompareFaileList = SqliteDbUtil.getHashCompareFailedList();//读取Hash比对失败文件列表
            if (hashCompareFaileList != null && hashCompareFaileList.size() > 0) {
                bw.write("Hash比对失败文件个数为:" + hashCompareFaileList.size() + "\r\n");
                for (FileMeta failedFile : hashCompareFaileList) {
                    bw.write(failedFile.getFileKey() + "\r\n");
                }
            } else {
                bw.write("Hash比对失败文件个数为:0\r\n");
            }
            List<FileMeta> uploadFaileList = SqliteDbUtil.getUploadFaileList();//读取上传失败文件列表
            if (uploadFaileList != null && uploadFaileList.size() > 0) {
                bw.write("上传失败文件个数为:" + uploadFaileList.size() + "\r\n");
                for (FileMeta failedFile : uploadFaileList) {
                    bw.write(failedFile.getFileKey() + "\r\n");
                }
            } else {
                bw.write("上传失败文件个数为:0\r\n");
            }
            List<FileMeta> deleteFaileList = SqliteDbUtil.getDeleteFaileList();//读取删除失败文件列表
            if (deleteFaileList != null && deleteFaileList.size() > 0) {
                bw.write("删除失败文件个数为:" + deleteFaileList.size() + "\r\n");
                for (FileMeta failedFile : deleteFaileList) {
                    bw.write(failedFile.getFileKey() + "\r\n");
                }
            } else {
                bw.write("删除失败文件个数为:0\r\n");
            }
            bw.flush();
        } catch (Exception e) {
            logger.error("导出同步失败文件列表出错:", e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}