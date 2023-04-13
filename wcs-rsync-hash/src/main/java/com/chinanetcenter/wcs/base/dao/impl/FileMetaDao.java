package com.chinanetcenter.wcs.base.dao.impl;

import com.chinanetcenter.wcs.base.dao.BaseDao;
import com.chinanetcenter.wcs.base.util.DbUtil;
import com.chinanetcenter.wcs.pojo.FileMeta;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by lidl on 2015/3/9.
 */
public class FileMetaDao extends BaseDao<FileMeta> {
    private static Logger logger = Logger.getLogger(FileMetaDao.class);

    @Override
    public FileMeta rsToEntity(ResultSet rs) {
        FileMeta fileMeta = new FileMeta();
        try {
            fileMeta.setFileKey(rs.getString("fileKey"));
            fileMeta.setFileSize(rs.getLong("fileSize"));
            fileMeta.setFileMtime(rs.getLong("fileMtime"));
            fileMeta.setHash(rs.getString("hash"));
            fileMeta.setFileParentPath(rs.getString("fileParentPath"));
            fileMeta.setUpdateTime(rs.getString("updateTime"));
            fileMeta.setStatus(rs.getInt("status"));
            fileMeta.setOperate(rs.getInt("operate"));
            fileMeta.setCompareStatus(rs.getInt("compareStatus"));
            fileMeta.setSyncDir(rs.getString("syncDir"));
        } catch (SQLException e) {
            logger.error("rs转换成FileMeta对象出错", e);
        }
        return fileMeta;
    }

    public Map<String, FileMeta> getFileMetaMap(String filePath) throws SQLException {
        Map<String, FileMeta> fileMetaMap = new HashMap<String, FileMeta>();
        String fileMetaListSql = "SELECT fileKey,fileSize,fileMtime,hash,fileParentPath,updateTime,status,operate,compareStatus,syncDir FROM file_meta WHERE fileParentPath=?";
        List<FileMeta> fileMetaList = selectList(fileMetaListSql, filePath);
        for (FileMeta fileMeta : fileMetaList) {
            fileMetaMap.put(fileMeta.getFileKey(), fileMeta);
        }
        return fileMetaMap;
    }

    public LinkedList<String> getFileParentPathList() throws SQLException {
        LinkedList<String> fileParentPathList = new LinkedList<String>();
        try {
            String sqlStr = "SELECT fileParentPath FROM file_meta GROUP BY fileParentPath";
            conn = DbUtil.getConnection();
            ps = conn.prepareStatement(sqlStr);
            rs = ps.executeQuery();
            for (; rs.next(); ) {
                fileParentPathList.add(rs.getString("fileParentPath"));
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            DbUtil.closeConnection(conn, ps, rs);
        }
        return fileParentPathList;
    }

    /**
     * 获取上传失败文件列表
     *
     * @return List<String>
     * @throws SQLException
     */
    public List<FileMeta> getUploadFaileList() throws SQLException {
        String fileMetaListSql = "SELECT fileKey,fileSize,fileMtime,hash,fileParentPath,updateTime,status,operate,compareStatus,syncDir FROM file_meta where operate in (1,2) and status=1";
        List<FileMeta> fileMetaList = selectList(fileMetaListSql);
        return fileMetaList;
    }

    /**
     * 获取删除失败文件列表
     *
     * @return List<String>
     * @throws SQLException
     */
    public List<FileMeta> getDeleteFaileList() throws SQLException {
        String fileMetaListSql = "SELECT fileKey,fileSize,fileMtime,hash,fileParentPath,updateTime,status,operate,compareStatus,syncDir FROM file_meta where operate=3 and status=1";
        List<FileMeta> fileMetaList = selectList(fileMetaListSql);
        return fileMetaList;
    }

    /**
     * 获取Hash比对失败文件列表
     *
     * @return List<String>
     * @throws SQLException
     */
    public List<FileMeta> getHashCompareFailedList() throws SQLException {
        String fileMetaListSql = "SELECT fileKey,fileSize,fileMtime,hash,fileParentPath,updateTime,status,operate,compareStatus,syncDir FROM file_meta where compareStatus=3";
        List<FileMeta> fileMetaList = selectList(fileMetaListSql);
        return fileMetaList;
    }

    public void batchInsert(List<FileMeta> fileMetaAddList) throws SQLException {
        String sqlStr = "REPLACE INTO  file_meta (fileKey,fileSize,fileMtime,fileParentPath,updateTime,status,operate,hash,compareStatus,syncDir) VALUES(?,?,?,?,?, ?,?,?,?,?)";
        List<Object[]> parasValues = new ArrayList<Object[]>();
        Object[] objects;
        for (FileMeta fileMeta : fileMetaAddList) {
            objects = new Object[10];
            objects[0] = fileMeta.getFileKey();
            objects[1] = fileMeta.getFileSize();
            objects[2] = fileMeta.getFileMtime();
            objects[3] = fileMeta.getFileParentPath();
            objects[4] = fileMeta.getUpdateTime();
            objects[5] = fileMeta.getStatus();
            objects[6] = fileMeta.getOperate();
            objects[7] = fileMeta.getHash();
            objects[8] = fileMeta.getCompareStatus();
            objects[9] = fileMeta.getSyncDir();
            parasValues.add(objects);
        }
        executeUpdate(sqlStr, parasValues);
    }

    public void batchUpdate(List<FileMeta> fileMetaUpdateList) throws SQLException {
        String sqlStr = "UPDATE file_meta SET fileSize=?,fileMtime=?,updateTime=?,status=?,operate=?,fileParentPath=?,hash=?,compareStatus=?,syncDir=? WHERE fileKey=?";
        List<Object[]> parasValues = new ArrayList<Object[]>();
        Object[] objects;
        for (FileMeta fileMeta : fileMetaUpdateList) {
            objects = new Object[10];
            objects[0] = fileMeta.getFileSize();
            objects[1] = fileMeta.getFileMtime();
            objects[2] = fileMeta.getUpdateTime();
            objects[3] = fileMeta.getStatus();
            objects[4] = fileMeta.getOperate();
            objects[5] = fileMeta.getFileParentPath();
            objects[6] = fileMeta.getHash();
            objects[7] = fileMeta.getCompareStatus();
            objects[8] = fileMeta.getSyncDir();
            objects[9] = fileMeta.getFileKey();
            parasValues.add(objects);
        }
        executeUpdate(sqlStr, parasValues);
    }

    public void batchDelete(List<Object[]> fileMetaDeleteList) throws SQLException {
        String sqlStr = "DELETE FROM file_meta WHERE fileKey=?";
        executeUpdate(sqlStr, fileMetaDeleteList);
    }

    public FileMeta getFileMetaByFileKey(String fileKey) throws SQLException {
        String fileMetaListSql = "SELECT fileKey,fileSize,fileMtime,hash,fileParentPath,updateTime,status,operate,compareStatus,syncDir FROM file_meta WHERE fileKey=?";
        List<FileMeta> fileMetaList = selectList(fileMetaListSql, fileKey);
        if (fileMetaList != null && fileMetaList.size() > 0) {
            return fileMetaList.get(0);
        }
        return null;
    }

    public List<FileMeta> getCompareHashPagingData(Integer operate, Integer compareStatus, int pageSize) throws SQLException {
        String fileMetaListSql = "SELECT fileKey,fileSize,fileMtime,hash,fileParentPath,updateTime,status,operate,compareStatus,syncDir FROM file_meta where operate!=? and compareStatus=? limit " + pageSize;
        List<FileMeta> fileMetaList = selectList(fileMetaListSql, operate, compareStatus);
        return fileMetaList;
    }

    public List<FileMeta> getRsyncPagingData(Integer compareStatus, Integer status, Integer operate, int pageSize) throws SQLException {
        String fileMetaListSql = "SELECT fileKey,fileSize,fileMtime,hash,fileParentPath,updateTime,status,operate,compareStatus,syncDir FROM file_meta where compareStatus=? and status=? and operate!=? limit " + pageSize;
        List<FileMeta> fileMetaList = selectList(fileMetaListSql, compareStatus, status, operate);
        return fileMetaList;
    }

    public int getUploadFailFilesNum() throws SQLException {
        String sql = "select count(*) num from file_meta where operate in (1,2) and status=1";
        try {
            conn = DbUtil.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("num");
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            DbUtil.closeConnection(conn, ps, rs);
        }
        return 0;
    }

    public int getUploadSuccessFilesNum() throws SQLException {
        String sql = "select count(*) num from file_meta where operate in (1,2) and status=0";
        try {
            conn = DbUtil.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("num");
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            DbUtil.closeConnection(conn, ps, rs);
        }
        return 0;
    }

    public int getUploadFilesNum() throws SQLException {
        String sql = "select count(*) num from file_meta where operate in (1,2) ";
        try {
            conn = DbUtil.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("num");
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            DbUtil.closeConnection(conn, ps, rs);
        }
        return 0;
    }

    public int getHashCompareFailedNum() throws SQLException {
        String sql = "select count(*) num from file_meta where  compareStatus=3 ";
        try {
            conn = DbUtil.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("num");
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            DbUtil.closeConnection(conn, ps, rs);
        }
        return 0;
    }

    public int getHashCompareFilesNum() throws SQLException {
        String sql = "select count(*) num from file_meta where  operate!=3 ";
        try {
            conn = DbUtil.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("num");
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            DbUtil.closeConnection(conn, ps, rs);
        }
        return 0;
    }

    public int getHashCompareSuccessAddNum() throws SQLException {
        String sql = "select count(*) num from file_meta where  compareStatus=2 and operate=1 ";
        try {
            conn = DbUtil.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("num");
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            DbUtil.closeConnection(conn, ps, rs);
        }
        return 0;
    }

    public int getHashCompareSuccessUpdateNum() throws SQLException {
        String sql = "select count(*) num from file_meta where  compareStatus=2 and operate=2 ";
        try {
            conn = DbUtil.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("num");
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            DbUtil.closeConnection(conn, ps, rs);
        }
        return 0;
    }

    public int getHashCompareSuccessNotUploadNum() throws SQLException {
        String sql = "select count(*) num from file_meta where  compareStatus=2 and operate=0 ";
        try {
            conn = DbUtil.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("num");
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            DbUtil.closeConnection(conn, ps, rs);
        }
        return 0;
    }

    public int getAddUploadFailFilesNum() throws SQLException {
        String sql = "select count(*) num from file_meta where operate=1 and status=1";
        try {
            conn = DbUtil.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("num");
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            DbUtil.closeConnection(conn, ps, rs);
        }
        return 0;
    }

    public int getUpdateUploadFailFilesNum() throws SQLException {
        String sql = "select count(*) num from file_meta where operate=2 and status=1";
        try {
            conn = DbUtil.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("num");
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            DbUtil.closeConnection(conn, ps, rs);
        }
        return 0;
    }
}

