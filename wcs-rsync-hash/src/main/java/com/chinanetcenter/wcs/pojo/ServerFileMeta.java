package com.chinanetcenter.wcs.pojo;

/**
 * Created by lidl on 15-3-11.
 */
public class ServerFileMeta {
    private String fileName;
    private String hash;
    private String lastModified;
    private long fileSize;

    public ServerFileMeta() {

    }

    public ServerFileMeta(String fileName, String hash, String lastModified, long fileSize) {
        this.fileName = fileName;
        this.hash = hash;
        this.lastModified = lastModified;
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
