package com.chinanetcenter.wcs.pojo;

import java.util.Date;

/**
 * 配置文件对象
 * Created by xiexb on 2014/9/3.
 */
public class ConfJson {
    /**
     * AK,SK
     * 必填项
     */
    public String accessKey;
    public String secretKey;
    /**
     * 目标空间
     * 必填项
     */
    public String bucket;
    /**
     * 文件key前缀
     * 如果将 keyPrefix 设为 abc/，在 syncDir中存在一个文件 a.txt， 则上传到wcs云存储后，此资源的key为 abc/a.txt
     */
    public String keyPrefix;
    /**
     * 计算hash的线程数
     * 非必填项
     */
    public Integer countHashThreadNum;

    /**
     * 比对文件Hash时，一次性从服务器查询compareHashFileNum个文件的hash
     * 非必填项
     */
    public Integer compareHashFileNum;

    /**
     * 比对hash的线程数
     * 非必填项
     */
    public Integer compareHashThreadNum;

    /**
     * 同步线程数上传文件
     * 非必填项
     */
    public Integer threadNum;
    /**
     * 小于规定大小的文件不进行上传操作
     */
    public Long minFileSize;

    /**
     * overwrite配置项，可配置值为0或者1，默认为1。1表示覆盖，其他值表示不覆盖
     */
    public Integer overwrite;
    /**
     * 本地源目录路径
     * 必填项
     */
    public String syncDir;
    /**
     * 进行分片上传的阈值，默认值为4MB，当文件大小小于等于4MB时直传整个文件，当文件大于4MB时进行分片上传该文件
     * 非必填项，当设置小于1时，默认为4MB
     */
    public Integer sliceThreshold;
    /**
     * 是否同步删除WCS云存储中的文件
     * 默认值为0，当本地文件删除时并不删除存储在WCS的对应文件。
     * 想删除本地文件的同时也删除存储在存储在WCS的文件,则设置为1
     * 非必填项，当值不是0或1时默认为0
     */
    public Integer deletable;
    public String uploadDomain;//上传域名
    /**
     * 上传文件地址，通常不让用户配置，默认直接上传至REST
     * 可配置上传节点地址
     */
    public String mgrDomain;//管理域名
    /**
     * 分片设置
     */
    public Integer sliceThread;
    public Integer sliceBlockSize;
    public Integer sliceChunkSize;

    /* Default rate is 1024*1024KB/s */
    public Integer maxRate;
    public String taskBeginTime;
    public String taskEndTime;
    public Date dTaskBeginTime;
    public Date dTaskEndTime;
    public String logLevel;
    /**
     * lastModifyTime配置项，可配置值为0或者1，默认为0。0表示不上传修改时间，1表示上传修改时间
     */
    public Integer isLastModifyTime;
    /**
     * httpLog的文件路径信息。d:/log/httplog.log  或者/var/log/httplog.log
     */
    public String logFilePath;

    /**
     * 配置为1时，只扫描文件列表，记录修改时间，不计算hash，不对比hash，不上传文件
     * 配置为0或者不填时，进行计算hash,比对hash,上传文件
     */
    public Integer scanOnly;
    /**
     * 控制是否比对HASH上传，默认配置为是
     * 配置为1时，比对Hash上传；配置为0时,上传文件无需进行Hash计算可直接进行上传
     */
    public Integer isCompareHash;

    //上传失败重试数
    public Integer uploadErrorRetry;

    /**
     * 同步模式syncMode，可配置为0或者1，
     * 默认值为0，表示单空间多目录的上传模式，
     * 1表示多空间多文件的上传模式
     */
    public Integer syncMode;

    /**
     * 目标空间及本地文件路
     * 格式为bucket1:dir1,dir2|bucket2:dir3,dir4,dir5，多个空间之间以|分隔，
     * 只能是一个空间对应多个本地路径，支持同时指定文件夹和文件名，以英文逗号分隔
     */
    public String bucketAndDir;

    /**
     * log4j 路径地址
     * 配置"logPrefix":"/cache1/log/"，则日志的完整路径为：/cache1/log/wcs-rsync-hash.log
     * 配置"logPrefix":"/tmp/myprefix-"，则日志的完整路径为：/tmp/myprefix-wcs-rsync-hash.log
     * 配置"logPrefix":"mylog/"，则日志的完整路径为：当前路径/mylog/wcs-rsync-hash.log
     */
    public String logPrefix;

    /**
     * 是否跳过406响应将任务置为成功
     */
    public String isSkip406;

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public Integer getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(Integer threadNum) {
        this.threadNum = threadNum;
    }

    public String getSyncDir() {
        return syncDir;
    }

    public void setSyncDir(String syncDir) {
        this.syncDir = syncDir;
    }

    public Integer getSliceThreshold() {
        return sliceThreshold;
    }

    public void setSliceThreshold(Integer sliceThreshold) {
        this.sliceThreshold = sliceThreshold;
    }

    public Integer getDeletable() {
        return deletable;
    }

    public void setDeletable(Integer deletable) {
        this.deletable = deletable;
    }

    public String getUploadDomain() {
        return uploadDomain;
    }

    public void setUploadDomain(String uploadDomain) {
        this.uploadDomain = uploadDomain;
    }

    public String getMgrDomain() {
        return mgrDomain;
    }

    public void setMgrDomain(String mgrDomain) {
        this.mgrDomain = mgrDomain;
    }

    public Integer getSliceThread() {
        return sliceThread;
    }

    public void setSliceThread(Integer sliceThread) {
        this.sliceThread = sliceThread;
    }

    public Integer getSliceBlockSize() {
        return sliceBlockSize;
    }

    public void setSliceBlockSize(Integer sliceBlockSize) {
        this.sliceBlockSize = sliceBlockSize;
    }

    public Integer getSliceChunkSize() {
        return sliceChunkSize;
    }

    public void setSliceChunkSize(Integer sliceChunkSize) {
        this.sliceChunkSize = sliceChunkSize;
    }

    public Integer getMaxRate() {
        return maxRate;
    }

    public void setMaxRate(Integer maxRate) {
        this.maxRate = maxRate;
    }

    public String getTaskBeginTime() {
        return taskBeginTime;
    }

    public void setTaskBeginTime(String taskBeginTime) {
        this.taskBeginTime = taskBeginTime;
    }

    public String getTaskEndTime() {
        return taskEndTime;
    }

    public void setTaskEndTime(String taskEndTime) {
        this.taskEndTime = taskEndTime;
    }

    public Date getdTaskBeginTime() {
        return dTaskBeginTime;
    }

    public void setdTaskBeginTime(Date dTaskBeginTime) {
        this.dTaskBeginTime = dTaskBeginTime;
    }

    public Date getdTaskEndTime() {
        return dTaskEndTime;
    }

    public void setdTaskEndTime(Date dTaskEndTime) {
        this.dTaskEndTime = dTaskEndTime;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public Integer getCountHashThreadNum() {
        return countHashThreadNum;
    }

    public void setCountHashThreadNum(Integer countHashThreadNum) {
        this.countHashThreadNum = countHashThreadNum;
    }

    public Integer getCompareHashThreadNum() {
        return compareHashThreadNum;
    }

    public void setCompareHashThreadNum(Integer compareHashThreadNum) {
        this.compareHashThreadNum = compareHashThreadNum;
    }

    public Integer getCompareHashFileNum() {
        return compareHashFileNum;
    }

    public void setCompareHashFileNum(Integer compareHashFileNum) {
        this.compareHashFileNum = compareHashFileNum;
    }

    public Long getMinFileSize() {
        return minFileSize;
    }

    public void setMinFileSize(Long minFileSize) {
        this.minFileSize = minFileSize;
    }

    public Integer getOverwrite() {
        return overwrite;
    }

    public void setOverwrite(Integer overwrite) {
        this.overwrite = overwrite;
    }

    public Integer getIsLastModifyTime() {
        return isLastModifyTime;
    }

    public void setIsLastModifyTime(Integer isLastModifyTime) {
        this.isLastModifyTime = isLastModifyTime;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    public Integer getScanOnly() {
        return scanOnly;
    }

    public void setScanOnly(Integer scanOnly) {
        this.scanOnly = scanOnly;
    }

    public Integer getIsCompareHash() {
        return isCompareHash;
    }

    public void setIsCompareHash(Integer isCompareHash) {
        this.isCompareHash = isCompareHash;
    }

    public Integer getUploadErrorRetry() {
        return uploadErrorRetry;
    }

    public void setUploadErrorRetry(Integer uploadErrorRetry) {
        this.uploadErrorRetry = uploadErrorRetry;
    }

    public Integer getSyncMode() {
        return syncMode;
    }

    public void setSyncMode(Integer syncMode) {
        this.syncMode = syncMode;
    }

    public String getBucketAndDir() {
        return bucketAndDir;
    }

    public void setBucketAndDir(String bucketAndDir) {
        this.bucketAndDir = bucketAndDir;
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    public void setLogPrefix(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    public String getIsSkip406() {
        return isSkip406;
    }

    public void setIsSkip406(String isSkip406) {
        this.isSkip406 = isSkip406;
    }

    @Override
    public String toString() {
        return "ConfJson{" +
                "accessKey='" + accessKey + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", bucket='" + bucket + '\'' +
                ", keyPrefix='" + keyPrefix + '\'' +
                ", countHashThreadNum=" + countHashThreadNum +
                ", compareHashFileNum=" + compareHashFileNum +
                ", compareHashThreadNum=" + compareHashThreadNum +
                ", threadNum=" + threadNum +
                ", minFileSize=" + minFileSize +
                ", overwrite=" + overwrite +
                ", syncDir='" + syncDir + '\'' +
                ", sliceThreshold=" + sliceThreshold +
                ", deletable=" + deletable +
                ", uploadDomain='" + uploadDomain + '\'' +
                ", mgrDomain='" + mgrDomain + '\'' +
                ", sliceThread=" + sliceThread +
                ", sliceBlockSize=" + sliceBlockSize +
                ", sliceChunkSize=" + sliceChunkSize +
                ", maxRate=" + maxRate +
                ", taskBeginTime='" + taskBeginTime + '\'' +
                ", taskEndTime='" + taskEndTime + '\'' +
                ", dTaskBeginTime=" + dTaskBeginTime +
                ", dTaskEndTime=" + dTaskEndTime +
                ", logLevel='" + logLevel + '\'' +
                ", isLastModifyTime=" + isLastModifyTime +
                ", logFilePath='" + logFilePath + '\'' +
                ", scanOnly=" + scanOnly +
                ", isCompareHash=" + isCompareHash +
                ", uploadErrorRetry=" + uploadErrorRetry +
                ", syncMode=" + syncMode +
                ", bucketAndDir='" + bucketAndDir + '\'' +
                ", logPrefix='" + logPrefix + '\'' +
                ", isSkip406='" + isSkip406 + '\'' +
                '}';
    }


}
