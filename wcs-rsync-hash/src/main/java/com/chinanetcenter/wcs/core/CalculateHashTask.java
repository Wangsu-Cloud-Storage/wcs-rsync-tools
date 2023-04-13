package com.chinanetcenter.wcs.core;

import com.chinanetcenter.api.util.DateUtil;
import com.chinanetcenter.api.util.WetagUtil;
import com.chinanetcenter.wcs.constant.SyncOperateEnum;
import com.chinanetcenter.wcs.pojo.FileMeta;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by lidl on 15-3-9.
 */
public class CalculateHashTask implements Runnable {
    private static Logger logger = Logger.getLogger(CalculateHashTask.class);
    private ConcurrentLinkedQueue<FileMeta> linkedQueue = new ConcurrentLinkedQueue<FileMeta>();
    private String fileKey;
    private long fileSize;
    private long lastModified;
    private String filePath;
    private File file;
    private String syncDir;

    public CalculateHashTask(String fileKey, long fileSize, long lastModified, String filePath, File file, ConcurrentLinkedQueue<FileMeta> linkedQueue, String syncDir) {
        this.fileKey = fileKey;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.filePath = filePath;
        this.file = file;
        this.linkedQueue = linkedQueue;
        this.syncDir = syncDir;
    }


    @Override
    public void run() {
        String formatDate = DateUtil.formatDate(new Date(), com.chinanetcenter.wcs.util.DateUtil.COMMON_PATTERN);
        String hash = WetagUtil.getEtagHash(file);
        logger.info("fileKey:" + fileKey + ",Hash:" + hash + ",[需要计算hash]");
        FileMeta fileMeta = new FileMeta(fileKey, fileSize, lastModified, filePath, formatDate, 2, SyncOperateEnum.notUpload.getValue(), hash, 0, syncDir);
        linkedQueue.add(fileMeta);
    }
}
