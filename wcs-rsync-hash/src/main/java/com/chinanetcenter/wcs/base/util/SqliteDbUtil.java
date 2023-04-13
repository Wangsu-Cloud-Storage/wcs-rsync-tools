package com.chinanetcenter.wcs.base.util;

import com.chinanetcenter.wcs.base.dao.impl.FileMetaDao;
import com.chinanetcenter.wcs.pojo.FileMeta;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by xiexb on 2014/9/29.
 */
public class SqliteDbUtil {

    public synchronized static Map<String, FileMeta> getFileMetaMap(String filePath) throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getFileMetaMap(filePath);
    }

    public synchronized static LinkedList<String> getFileParentPathList() throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getFileParentPathList();
    }

    public synchronized static void batchInsert(List<FileMeta> fileMetaAddList) throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        fileMetaDao.batchInsert(fileMetaAddList);
    }

    public synchronized static void batchUpdate(List<FileMeta> fileMetaUpdateList) throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        fileMetaDao.batchUpdate(fileMetaUpdateList);
    }

    public synchronized static void batchDelete(List<Object[]> fileMetaDeleteList) throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        fileMetaDao.batchDelete(fileMetaDeleteList);
    }

    public synchronized static List<FileMeta> getUploadFaileList() throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getUploadFaileList();
    }

    public synchronized static List<FileMeta> getDeleteFaileList() throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getDeleteFaileList();
    }

    public synchronized static List<FileMeta> getHashCompareFailedList() throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getHashCompareFailedList();
    }

    public synchronized static FileMeta getFileMetaMapByFileKey(String fileKey) throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getFileMetaByFileKey(fileKey);
    }

    public synchronized static List<FileMeta> getCompareHashPagingData(Integer operate, Integer compareStatus, int pageSize) throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getCompareHashPagingData(operate, compareStatus, pageSize);
    }

    public synchronized static List<FileMeta> getRsyncPagingData(Integer compareStatus, Integer status, Integer operate, int pageSize) throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getRsyncPagingData(compareStatus, status, operate, pageSize);
    }

    public synchronized static int getUploadFailFilesNum() throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getUploadFailFilesNum();
    }

    public synchronized static int getUploadSuccessFilesNum() throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getUploadSuccessFilesNum();
    }

    public synchronized static int getUploadFilesNum() throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getUploadFilesNum();
    }

    public synchronized static int getHashCompareFailedNum() throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getHashCompareFailedNum();
    }

    public synchronized static int getHashCompareFilesNum() throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getHashCompareFilesNum();
    }

    public synchronized static int getHashCompareSuccessAddNum() throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getHashCompareSuccessAddNum();
    }

    public synchronized static int getHashCompareSuccessUpdateNum() throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getHashCompareSuccessUpdateNum();
    }

    public synchronized static int getHashCompareSuccessNotUploadNum() throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getHashCompareSuccessNotUploadNum();
    }

    public synchronized static int getAddUploadFailFilesNum() throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getAddUploadFailFilesNum();
    }

    public synchronized static int getUpdateUploadFailFilesNum() throws SQLException {
        FileMetaDao fileMetaDao = new FileMetaDao();
        return fileMetaDao.getUpdateUploadFailFilesNum();
    }
}
