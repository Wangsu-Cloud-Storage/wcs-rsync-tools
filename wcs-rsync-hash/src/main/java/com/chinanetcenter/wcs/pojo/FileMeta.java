package com.chinanetcenter.wcs.pojo;

/**
 * 文件元信息
 * Created by lidl on 2015-3-11
 */
public class FileMeta {
    private String fileKey;//文件key
    private Long fileSize;//文件大小
    private Long fileMtime;//修改时间
    private String fileParentPath;//文件路径
    private String updateTime;//同步时间
    private Integer status;//同步状态 0成功，1失败,2未操作(初始状态)，3文件在同步中
    private Integer operate;//操作类型(0 notUpload:不上传【初始状态】,1 add 上传文件,2 update 覆盖上传,3 delete 删除文件)
    private String hash;//文件的hash值
    private Integer compareStatus;//0:hash未比对,1:hash比对中：2:比对成功。3：hash比对失败
    private String syncDir;//本地源目录路径

    public FileMeta() {
    }

    public FileMeta(String fileKey, Long fileSize, Long fileMtime, String fileParentPath, String updateTime, Integer status, Integer operate, String hash, Integer compareStatus, String syncDir) {
        this.fileKey = fileKey;
        this.fileSize = fileSize;
        this.fileMtime = fileMtime;
        this.fileParentPath = fileParentPath;
        this.updateTime = updateTime;
        this.status = status;
        this.operate = operate;
        this.hash = hash;
        this.compareStatus = compareStatus;
        this.syncDir = syncDir;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getFileMtime() {
        return fileMtime;
    }

    public void setFileMtime(Long fileMtime) {
        this.fileMtime = fileMtime;
    }

    public String getFileParentPath() {
        return fileParentPath;
    }

    public void setFileParentPath(String fileParentPath) {
        this.fileParentPath = fileParentPath;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getOperate() {
        return operate;
    }

    public void setOperate(Integer operate) {
        this.operate = operate;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Integer getCompareStatus() {
        return compareStatus;
    }

    public void setCompareStatus(Integer compareStatus) {
        this.compareStatus = compareStatus;
    }

    public String getSyncDir() {
        return syncDir;
    }

    public void setSyncDir(String syncDir) {
        this.syncDir = syncDir;
    }
}
