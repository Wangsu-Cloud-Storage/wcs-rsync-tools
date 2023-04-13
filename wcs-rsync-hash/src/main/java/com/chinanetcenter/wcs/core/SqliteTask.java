package com.chinanetcenter.wcs.core;

import com.chinanetcenter.wcs.base.util.SqliteDbUtil;
import com.chinanetcenter.wcs.pojo.FileMeta;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 持久化同步同步成功的数据到Sqlite数据库
 * Created by lidl on 2015-3-10
 */
public class SqliteTask implements Runnable {
    private static Logger logger = Logger.getLogger(SqliteTask.class);
    private static final int LIMITSIZE = 100;//批量执行数据库大小限制
    List<FileMeta> fileMetaAddList = new ArrayList<FileMeta>();
    List<FileMeta> fileMetaUpdateList = new ArrayList<FileMeta>();
    List<Object[]> fileMetaDeleteList = new ArrayList<Object[]>();

    @Override
    public void run() {
        while (!RsyncCore.over) {
            FileMeta addFileMeta = RsyncCore.fileMetaAddQueue.poll();
            FileMeta updateFileMeta = RsyncCore.fileMetaUpdateQueue.poll();
            Object[] delObject = RsyncCore.fileMetaDeleteQueue.poll();
            if (addFileMeta != null) {
                fileMetaAddList.add(addFileMeta);
            }
            if (updateFileMeta != null) {
                fileMetaUpdateList.add(updateFileMeta);
            }
            if (delObject != null) {
                fileMetaDeleteList.add(delObject);
            }
            executeBatchOperate(LIMITSIZE);
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        while (!RsyncCore.fileMetaAddQueue.isEmpty()) {
            fileMetaAddList.add(RsyncCore.fileMetaAddQueue.poll());
        }
        while (!RsyncCore.fileMetaUpdateQueue.isEmpty()) {
            fileMetaUpdateList.add(RsyncCore.fileMetaUpdateQueue.poll());
        }
        while (!RsyncCore.fileMetaDeleteQueue.isEmpty()) {
            fileMetaDeleteList.add(RsyncCore.fileMetaDeleteQueue.poll());
        }
        executeBatchOperate(1);
    }

    /**
     * 大于executeSizeLimit时进行数据库批量操作
     *
     * @param executeSizeLimit
     */
    private void executeBatchOperate(int executeSizeLimit) {
        if (fileMetaAddList.size() >= executeSizeLimit) {
            try {
                SqliteDbUtil.batchInsert(fileMetaAddList);
                logger.debug("批量新增数据成功:" + fileMetaAddList.size() + "条");
            } catch (SQLException se) {
                logger.error(convertFileMetaList(fileMetaAddList) + " 批量新增数据失败:", se);
            }
            fileMetaAddList.clear();
        }
        if (fileMetaUpdateList.size() >= executeSizeLimit) {
            try {
                SqliteDbUtil.batchUpdate(fileMetaUpdateList);
                logger.debug("批量更新数据成功:" + fileMetaUpdateList.size() + "条");
            } catch (SQLException se) {
                logger.error(convertFileMetaList(fileMetaUpdateList) + "批量更新数据失败:", se);
            }
            fileMetaUpdateList.clear();
        }
        if (fileMetaDeleteList.size() >= executeSizeLimit) {
            try {
                SqliteDbUtil.batchDelete(fileMetaDeleteList);
                logger.debug("批量删除数据成功:" + fileMetaDeleteList.size() + "条");
            } catch (SQLException se) {
                logger.error("批量删除数据失败:", se);
            }
            fileMetaDeleteList.clear();
        }
    }

    private String convertFileMetaList(List<FileMeta> fileMetaList) {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        for (FileMeta fileMeta : fileMetaList) {//如果有前缀，文件名前加前缀
            arrayNode.add(fileMeta.getFileKey());
        }
        return arrayNode.toString();
    }
}
