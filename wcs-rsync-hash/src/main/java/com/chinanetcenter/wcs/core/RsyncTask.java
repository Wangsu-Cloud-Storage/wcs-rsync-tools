package com.chinanetcenter.wcs.core;

import com.chinanetcenter.api.entity.HttpClientResult;
import com.chinanetcenter.api.entity.PutPolicy;
import com.chinanetcenter.api.exception.WsClientException;
import com.chinanetcenter.api.sliceUpload.PutExtra;
import com.chinanetcenter.api.util.TokenUtil;
import com.chinanetcenter.api.wsbox.SliceUploadResumable;
import com.chinanetcenter.wcs.constant.RsyncConstant;
import com.chinanetcenter.wcs.constant.SyncOperateEnum;
import com.chinanetcenter.wcs.pojo.CacheInfo;
import com.chinanetcenter.wcs.pojo.FileMeta;
import com.chinanetcenter.wcs.util.DateUtil;
import com.chinanetcenter.wcs.util.HttpClientResultUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 同步文件多线程
 * Created by lidl on 2015-3-11
 */
public class RsyncTask implements Runnable {
    private static Logger logger = Logger.getLogger(RsyncTask.class);
    private FileMeta fileMeta;
    private File rsyncFile;
    private int operateType;
    private String fileKey;
    private int sleepCout;
    private int uploadErrorRetry;

    public RsyncTask(FileMeta fileMeta, File rsyncFile, int operateType, int uploadErrorRetry) {
        this.fileMeta = fileMeta;
        this.rsyncFile = rsyncFile;
        this.operateType = operateType;
        this.fileKey = fileMeta.getFileKey();
        this.sleepCout = 0;
        this.uploadErrorRetry = uploadErrorRetry;
    }

    private static PutExtra getPutExtra(String sliceConfigPath) {
        File configFile = new File(sliceConfigPath);
        if (!configFile.exists() || configFile.length() <= 0) return null;
        FileReader reader;
        int fileLen = (int) configFile.length();
        char[] chars = new char[fileLen];
        try {
            reader = new FileReader(configFile);
            reader.read(chars);
            String txt = String.valueOf(chars);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode obj = objectMapper.readTree(txt);
            PutExtra putExtra = new PutExtra(obj);
            return putExtra;
        } catch (Exception e) {
            logger.error("get putExtra error,file reUpload,fileLen:" + fileLen + ",path" + sliceConfigPath + ",message:" + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void run() {
        while (!RsyncCore.excuteTask()) {//配置了任务运行时间段且当前时间在时间段之外
            if (sleepCout % 360 == 0) {//1小时打一条日志
                logger.debug(fileKey + "->当前时间未在设置的同步时间区间!");
            }
            try {
                Thread.sleep(10000);//休眠10秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sleepCout++;
        }
        if (operateType == SyncOperateEnum.delete.getValue()) {
            delete();
        } else {
            if (fileMeta.getFileSize() > HttpClientResultUtil.sliceLimitSize) {
                sliceUpload();
            } else {
                upload();
            }
        }
    }

    /**
     * 直传文件
     */
    private void upload() {
        RsyncConstant.uploadFilesNum.getAndIncrement();
        CacheInfo.isNeedRetry.set(true);
        CacheInfo.retryNum.set(0);
        if (rsyncFile.exists()) {
            while (CacheInfo.retryNum.get() <= uploadErrorRetry && CacheInfo.isNeedRetry.get()) {
                try {
                    if (CacheInfo.retryNum.get() > 0) {
                        logger.debug("正在重试上传文件:【" + fileKey + "】,文件大小:" + fileMeta.getFileSize() + ",重试次数:" + CacheInfo.retryNum.get());
                    } else {
                        logger.debug("正在上传文件:【" + fileKey + "】,文件大小:" + fileMeta.getFileSize());
                    }
                    long beginSyncTime = System.currentTimeMillis();
                    HttpClientResult uploadHttpClientResult = HttpClientResultUtil.uploadResult(fileKey, rsyncFile);
                    if (uploadHttpClientResult == null || uploadHttpClientResult.getStatus() != 200) {
                        //状态码为5xx；状态码为408则需要进行重试上传
                        if ((null != uploadHttpClientResult && uploadHttpClientResult.getStatus() == 408) || (null != uploadHttpClientResult && uploadHttpClientResult.getStatus() >= 500 && uploadHttpClientResult.getStatus() <= 599)) {
                            if (CacheInfo.retryNum.get() >= uploadErrorRetry) {
                                CacheInfo.isNeedRetry.set(false);
                            } else {
                                CacheInfo.isNeedRetry.set(true);
                            }
                        } else {
                            CacheInfo.isNeedRetry.set(false);
                        }
                        if (uploadHttpClientResult != null) {
                            if (CacheInfo.retryNum.get() > 0) {
                                logger.error("重试上传文件失败【" + fileKey + "】:" + uploadHttpClientResult.getStatus() + "   " + uploadHttpClientResult.getResponse() + "重试次数:" + CacheInfo.retryNum.get());//token等校验失败
                            } else {
//                                if (uploadHttpClientResult.getStatus() == 406) {
//                                    RsyncConstant.uploadFilesNum.getAndDecrement();
//                                    RsyncConstant.notUploadFileNum_alreadyUpload.getAndIncrement();
//                                    RsyncConstant.notUploadFileNum_alreadyUpload.getAndIncrement();
//                                }
                                logger.error("上传文件失败【" + fileKey + "】:" + uploadHttpClientResult.getStatus() + "   " + uploadHttpClientResult.getResponse());//token等校验失败
                            }
                        } else {
                            if (CacheInfo.retryNum.get() > 0) {
                                logger.error("重试上传文件失败【" + fileKey + "】");//token等校验失败
                            } else {
                                logger.error("上传文件失败【" + fileKey + "】");//token等校验失败
                            }
                        }
                        if (!CacheInfo.isNeedRetry.get() || CacheInfo.retryNum.get() >= uploadErrorRetry) {

                            if (SyncOperateEnum.add.getValue() == operateType) {//新增上传失败
                                RsyncConstant.uploadAddFailedFilesNum.getAndIncrement();
                            }
                            if (SyncOperateEnum.update.getValue() == operateType) {//覆盖上传失败
                                RsyncConstant.uploadUpdateFailedFilesNum.getAndIncrement();
                            }
                            fileMeta.setStatus(1);
                        }

                    } else {
                        if (SyncOperateEnum.add.getValue() == operateType) {//新增上传成功
                            RsyncConstant.uploadAddSuccessFilesNum.getAndIncrement();
                        }
                        if (SyncOperateEnum.update.getValue() == operateType) {//覆盖上传成功
                            RsyncConstant.uploadUpdateSuccessFilesNum.getAndIncrement();
                        }
                        long costSyncTime = System.currentTimeMillis() - beginSyncTime;
                        if (CacheInfo.retryNum.get() > 0) {
                            logger.debug("【" + fileKey + "】重试上传成功,耗时" + costSyncTime + "毫秒,重试次数:" + CacheInfo.retryNum.get());
                        } else {
                            logger.debug("【" + fileKey + "】上传成功,耗时" + costSyncTime + "毫秒");
                        }
                        fileMeta.setStatus(0);
                        //上传成功就不需要重试
                        CacheInfo.isNeedRetry.set(false);
                    }
                } catch (Exception e) {
                    if (CacheInfo.retryNum.get() > 0) {
                        logger.error("重试上传文件失败【" + fileKey + "】,重试次数:" + CacheInfo.retryNum.get() + " :", e);
                    } else {
                        logger.error("上传文件失败【" + fileKey + "】:", e);
                    }
                    CacheInfo.isNeedRetry.set(isNeedRetry(e));
                    if (CacheInfo.retryNum.get() == uploadErrorRetry || CacheInfo.isNeedRetry.get() == false) {
                        if (SyncOperateEnum.add.getValue() == operateType) {//新增上传失败
                            RsyncConstant.uploadAddFailedFilesNum.getAndIncrement();
                        }
                        if (SyncOperateEnum.update.getValue() == operateType) {//覆盖上传失败
                            RsyncConstant.uploadUpdateFailedFilesNum.getAndIncrement();
                        }
                        fileMeta.setStatus(1);
                    }
                }
                CacheInfo.retryNum.set(CacheInfo.retryNum.get() + 1);
            }
            fileMeta.setUpdateTime(DateUtil.formatDate(new Date(), DateUtil.COMMON_PATTERN));
            RsyncCore.fileMetaUpdateQueue.add(fileMeta);//加入数据库更新列表
        } else {
            fileMeta.setStatus(1);
            fileMeta.setUpdateTime(DateUtil.formatDate(new Date(), DateUtil.COMMON_PATTERN));
            RsyncCore.fileMetaUpdateQueue.add(fileMeta);//加入数据库更新列表
            if (SyncOperateEnum.add.getValue() == operateType) {//新增上传失败
                RsyncConstant.uploadAddFailedFilesNum.getAndIncrement();
            }
            if (SyncOperateEnum.update.getValue() == operateType) {//覆盖上传失败
                RsyncConstant.uploadUpdateFailedFilesNum.getAndIncrement();
            }
            logger.error("上传文件时，找不到本地文件:" + rsyncFile.getAbsolutePath());
        }
    }

    /**
     * 分片上传文件
     */
    private void sliceUpload() {
        RsyncConstant.uploadFilesNum.getAndIncrement();
        CacheInfo.isNeedRetry.set(true);
        CacheInfo.retryNum.set(0);
        if (rsyncFile.exists()) {
            while (CacheInfo.retryNum.get() <= uploadErrorRetry && CacheInfo.isNeedRetry.get()) {
                try {
                    if (CacheInfo.retryNum.get() > 0) {
                        logger.debug("正在重试上传文件:【" + fileKey + "】,文件大小:" + fileMeta.getFileSize() + ",使用分片上传方式,重试次数:" + CacheInfo.retryNum.get());
                    } else {
                        logger.debug("正在上传文件:【" + fileKey + "】,文件大小:" + fileMeta.getFileSize() + ",使用分片上传方式");
                    }
                    final long beginSyncTime = System.currentTimeMillis();
                    final String bucketName = HttpClientResultUtil.BUCKET;//空间名
                    String key = DigestUtils.md5Hex(bucketName + ":" + fileKey);
                    final String sliceConfigPath = System.getProperty("user.home") + File.separator + ".wcsrsynchash" + File.separator + "sliceData" + File.separator + key + "_sliceConfig.properties";
                    final String filePath = rsyncFile.getAbsolutePath();
                    PutPolicy putPolicy = new PutPolicy();
                    putPolicy.setScope(bucketName + ":" + fileKey);
                    putPolicy.setOverwrite(RsyncCore.confJson.overwrite);//默认直接覆盖
                    if (RsyncCore.confJson.isLastModifyTime == 1) {
                        putPolicy.setLastModifiedTime(String.valueOf(rsyncFile.lastModified()));
                    }
                    PutExtra putExtra = getPutExtra(sliceConfigPath);
                    if (putExtra == null) {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("bucketName", bucketName);
                        params.put("fileKey", fileKey);
                        params.put("filePath", filePath);
                        params.put("putPolicy", putPolicy.toString());
                        String token = TokenUtil.getUploadToken(putPolicy);
                        params.put("token", token);
                        putExtra = new PutExtra();
                        putExtra.params = params;
                    }
                    MyJSONObjectRet jsonObjectRet = new MyJSONObjectRet(uploadErrorRetry) {
                        int progress = 0;

                        @Override
                        public void onSuccess(JsonNode obj) {//TODO 暂时不进行hash计算
                           /* File fileHash = new File(filePath);
                            String eTagHash = WetagUtil.getEtagHash(fileHash.getParent(), fileHash.getName());//根据文件内容计算hash
                            SliceUploadHttpResult result = new SliceUploadHttpResult(obj);
                            if(eTagHash.equals(result.getHash())){
                                System.out.println("上传完成");
                            }else{
                                System.out.println("hash not equal,eTagHash:" + eTagHash + " ,hash:" + result.getHash() );
                            }*/
                            File sliceConfigFile = new File(sliceConfigPath);
                            if (sliceConfigFile.exists()) {//删除分片记录文件
                                sliceConfigFile.delete();
                            }
                            long costSyncTime = System.currentTimeMillis() - beginSyncTime;
                            if (SyncOperateEnum.add.getValue() == operateType) {//新增上传成功
                                RsyncConstant.uploadAddSuccessFilesNum.getAndIncrement();
                            }
                            if (SyncOperateEnum.update.getValue() == operateType) {//覆盖上传成功
                                RsyncConstant.uploadUpdateSuccessFilesNum.getAndIncrement();
                            }
                            if (CacheInfo.retryNum.get() > 0) {
                                logger.debug("【" + fileKey + "】分片上传成功,耗时" + costSyncTime + "毫秒,重试次数:" + CacheInfo.retryNum.get());
                            } else {
                                logger.debug("【" + fileKey + "】分片上传成功,耗时" + costSyncTime + "毫秒");
                            }
                            fileMeta.setStatus(0);
                            CacheInfo.isNeedRetry.set(false);
                        }

                        @Override
                        public void onSuccess(byte[] body) {
                            System.out.println(new String(body));
                        }

                        @Override
                        public void onFailure(Exception ex) {
                            boolean isWsClientException = false;
                            Throwable t = null;
                            if (ex instanceof WsClientException) {
                                t = ex;
                                isWsClientException = true;

                            } else {
                                t = ex.getCause();
                                while (null != t) {
                                    if (t instanceof WsClientException) {
                                        isWsClientException = true;
                                        break;
                                    } else {
                                        t = t.getCause();
                                    }
                                }
                            }
                            if (isWsClientException) {
                                WsClientException he = (WsClientException) t;
                                if (he.code == 412) {//删除分片记录文件，否则无法重新上传
                                    File sliceConfigFile = new File(sliceConfigPath);
                                    if (sliceConfigFile.exists()) {//删除分片记录文件
                                        sliceConfigFile.delete();
                                    }
                                }
                                //状态码为5xx；状态码为408则需要进行重试上传
                                if (he.code == 408 || (he.code >= 500 && he.code <= 599)) {
                                    if (CacheInfo.retryNum.get() >= uploadErrorRetry) {
                                        CacheInfo.isNeedRetry.set(false);
                                    } else {
                                        CacheInfo.isNeedRetry.set(true);
                                    }
                                    if (CacheInfo.retryNum.get() > 0) {
                                        logger.error("重试分片上传文件失败【" + fileKey + "】:" + he.code + "   " + ex.getMessage() + "重试次数:" + CacheInfo.retryNum.get());//token等校验失败
                                    } else {
                                        logger.error("分片上传文件失败【" + fileKey + "】:" + he.code + "   " + ex.getMessage());//token等校验失败
                                    }
                                } else {
                                    CacheInfo.isNeedRetry.set(false);
                                }
                            } else {
                                if (CacheInfo.retryNum.get() > 0) {
                                    logger.error("重试分片上传文件失败【" + fileKey + "】,重试次数:" + CacheInfo.retryNum.get() + " :", ex);
                                } else {
                                    logger.error("分片上传文件失败【" + fileKey + "】:", ex);
                                }
                                CacheInfo.isNeedRetry.set(isNeedRetry(ex));
                            }
                            if (!CacheInfo.isNeedRetry.get() || CacheInfo.retryNum.get() >= uploadErrorRetry) {

                                if (SyncOperateEnum.add.getValue() == operateType) {//新增上传失败
                                    RsyncConstant.uploadAddFailedFilesNum.getAndIncrement();
                                }
                                if (SyncOperateEnum.update.getValue() == operateType) {//覆盖上传失败
                                    RsyncConstant.uploadUpdateFailedFilesNum.getAndIncrement();
                                }
                                fileMeta.setStatus(1);
                            }


                        }

                        @Override
                        public void onProcess(long current, long total) {
                            int percent = (int) (current * 100 / total);
                            int progressTemp = 0;
                            if (percent < 100) {
                                progressTemp = percent / 10 % 10;//取出个位数
                            } else {
                                progressTemp = percent / 10;
                            }
                            if (progressTemp - progress >= 1) {
                                progress = progressTemp;
                                logger.debug("【" + fileKey + "】分片上传进度:" + percent + " %");
                            }
                        }

                        @Override
                        public void onPersist(JsonNode obj) {
                            File configFile = new File(sliceConfigPath);
                            synchronized (configFile) {
                                FileOutputStream fileOutputStream = null;
                                try {
                                    if (!configFile.getParentFile().exists()) {
                                        configFile.getParentFile().mkdirs();
                                    }
                                    if (!configFile.exists()) {
                                        configFile.createNewFile();
                                    }
                                    ObjectMapper objectMapper = new ObjectMapper();
                                    fileOutputStream = new FileOutputStream(configFile);
                                    fileOutputStream.write(objectMapper.writeValueAsString(obj).getBytes());
                                    fileOutputStream.flush();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    if (fileOutputStream != null) {
                                        try {
                                            fileOutputStream.close();
                                        } catch (IOException e) {
                                        }
                                    }
                                }
                            }
                        }
                    };

                    SliceUploadResumable sliceUploadResumable = new SliceUploadResumable();
                    sliceUploadResumable.execUpload(bucketName, fileKey, filePath, putPolicy, putExtra, jsonObjectRet);
                } catch (Exception e) {
                    if (CacheInfo.retryNum.get() > 0) {
                        logger.error("重试分片上传文件失败【" + fileKey + "】,重试次数:" + CacheInfo.retryNum.get() + " :", e);
                    } else {
                        logger.error("分片上传文件失败【" + fileKey + "】:", e);
                    }
                    CacheInfo.isNeedRetry.set(isNeedRetry(e));
                    if (CacheInfo.retryNum.get() == uploadErrorRetry || CacheInfo.isNeedRetry.get() == false) {
                        if (SyncOperateEnum.add.getValue() == operateType) {//新增上传失败
                            RsyncConstant.uploadAddFailedFilesNum.getAndIncrement();
                        }
                        if (SyncOperateEnum.update.getValue() == operateType) {//覆盖上传失败
                            RsyncConstant.uploadUpdateFailedFilesNum.getAndIncrement();
                        }
                        fileMeta.setStatus(1);
                    }
                }
                CacheInfo.retryNum.set(CacheInfo.retryNum.get() + 1);
            }
            fileMeta.setUpdateTime(DateUtil.formatDate(new Date(), DateUtil.COMMON_PATTERN));
            RsyncCore.fileMetaUpdateQueue.add(fileMeta);//加入数据库更新列表
        } else {
            fileMeta.setStatus(1);
            fileMeta.setUpdateTime(DateUtil.formatDate(new Date(), DateUtil.COMMON_PATTERN));
            RsyncCore.fileMetaUpdateQueue.add(fileMeta);//加入数据库更新列表
            if (SyncOperateEnum.add.getValue() == operateType) {//新增上传失败
                RsyncConstant.uploadAddFailedFilesNum.getAndIncrement();
            }
            if (SyncOperateEnum.update.getValue() == operateType) {//覆盖上传失败
                RsyncConstant.uploadUpdateFailedFilesNum.getAndIncrement();
            }
            logger.error("上传文件时，找不到本地文件:" + rsyncFile.getAbsolutePath());
        }
    }

    /**
     * 删除文件
     */
    private void delete() {
        try {
            logger.debug("正在删除文件:" + fileKey);
            long beginSyncTime = System.currentTimeMillis();
            if (RsyncCore.confJson.deletable == 1) {//同步删除服务器
                HttpClientResult deleteHttpClientResult = HttpClientResultUtil.deleteResult(fileKey);
                if (deleteHttpClientResult == null || (deleteHttpClientResult.getStatus() != 200 && deleteHttpClientResult.getStatus() != 404)) {//"404", "File Not Found"文件可能已经被服务端删除
                    if (deleteHttpClientResult != null) {//"406", "File Is Busy
                        logger.error("删除文件失败【" + fileKey + "】:" + deleteHttpClientResult.getStatus() + "   " + deleteHttpClientResult.getResponse());//token等校验失败
                    }
                    fileMeta.setStatus(1);
                    fileMeta.setUpdateTime(DateUtil.formatDate(new Date(), DateUtil.COMMON_PATTERN));
                    RsyncCore.fileMetaUpdateQueue.add(fileMeta);//加入数据库更新列表
                    RsyncConstant.delFailedNum.getAndIncrement();
                    return;
                }
            }
            Object[] objects = new Object[1];
            objects[0] = fileMeta.getFileKey();
            RsyncCore.fileMetaDeleteQueue.add(objects);//加入数据库删除列表
            long costSyncTime = System.currentTimeMillis() - beginSyncTime;
            logger.debug(fileKey + "删除成功,耗时" + costSyncTime + "毫秒");
        } catch (Exception e) {
            logger.error("删除文件失败【" + fileKey + "】:", e);
            fileMeta.setStatus(1);
            fileMeta.setUpdateTime(DateUtil.formatDate(new Date(), DateUtil.COMMON_PATTERN));
            RsyncCore.fileMetaUpdateQueue.add(fileMeta);//加入数据库更新列表
            RsyncConstant.delFailedNum.getAndIncrement();
        }
    }

    public boolean isNeedRetry(Exception e) {
        boolean isNeedRetry = false;
        if (null != e) {
            if (null != e.getCause() && e.getCause() instanceof Exception) {
                isNeedRetry = isNeedRetry((Exception) e.getCause());
            }
            if (e instanceof SocketTimeoutException || e instanceof ConnectException || e instanceof SocketException ||
                    e instanceof HttpHostConnectException || e instanceof UnknownHostException) {
                isNeedRetry = true;
            }
        }
        return isNeedRetry;
    }

}
