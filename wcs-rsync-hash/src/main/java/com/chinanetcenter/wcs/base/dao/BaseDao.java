package com.chinanetcenter.wcs.base.dao;

import com.chinanetcenter.wcs.base.util.DbUtil;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public abstract class BaseDao<T> {
    private static Logger logger = Logger.getLogger(BaseDao.class);

    protected Connection conn = null;       // 数据库连接对象
    protected PreparedStatement ps = null;  // 预编译的SQL命令执行对象
    protected ResultSet rs = null;          // 结果集对象

    /**
     * 查询列表数据
     *
     * @param sqlStr
     * @param params
     * @return
     * @throws SQLException
     */
    public List<T> selectList(String sqlStr, Object... params) throws SQLException {
        List<T> results = new ArrayList<T>();
        try {
            conn = DbUtil.getConnection();
            ps = conn.prepareStatement(sqlStr);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            rs = ps.executeQuery();
            for (; rs.next(); ) {
                T t = rsToEntity(rs);
                results.add(t);
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            DbUtil.closeConnection(conn, ps, rs);
        }
        return results;
    }

    /**
     * 批量更新数据
     *
     * @param sqlStr
     * @param parasValues
     * @throws SQLException
     */
    public void executeUpdate(String sqlStr, List<Object[]> parasValues) throws SQLException {
        try {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);
            ps = conn.prepareStatement(sqlStr);
            for (int i = 0; i < parasValues.size(); i++) {
                Object[] parasValue = parasValues.get(i);
                for (int j = 0; j < parasValue.length; j++) {
                    ps.setObject(j + 1, parasValue[j]);
                }
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit(); //手工提交
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            if (e instanceof BatchUpdateException) {
                BatchUpdateException bException = (BatchUpdateException) e;
                int[] s = bException.getUpdateCounts();
                Object[] objects = parasValues.get(s.length);
                ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                for (int i = 0; i < objects.length; i++) {
                    arrayNode.add((String) objects[i]);
                }
                logger.error("executeUpdate更新失败数据:" + arrayNode.toString());
            }
            conn.rollback();
            throw e;
        } finally {
            DbUtil.closeConnection(conn, ps, rs);
        }
    }

    /**
     * 更新一条数据
     *
     * @param sqlStr
     * @throws SQLException
     */
    public void executeUpdate(String sqlStr, Object... params) throws SQLException {
        try {
            conn = DbUtil.getConnection();
            ps = conn.prepareStatement(sqlStr);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            DbUtil.closeConnection(conn, ps, rs);
        }
    }

    public abstract T rsToEntity(ResultSet rs);

}
