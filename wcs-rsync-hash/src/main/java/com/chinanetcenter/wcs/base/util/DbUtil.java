package com.chinanetcenter.wcs.base.util;

import java.sql.*;

/**
 * 数据库操作公用类
 * Created by xiexb on 2014/9/2.
 */
public class DbUtil {
    public static String dbFilePath;

    /**
     * 获取数据库连接
     *
     * @return
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
        return conn;
    }

    /**
     * 关闭数据库连接资源
     *
     * @param conn
     * @param ps
     * @param rs
     */
    public static void closeConnection(Connection conn, Statement ps, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
                rs = null;
            }
            if (ps != null) {
                ps.close();
                ps = null;
            }
            if (conn != null) {
                conn.close();
                conn = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
