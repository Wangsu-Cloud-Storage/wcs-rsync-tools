package com.chinanetcenter.wcs.pojo;

public class CacheInfo {
    public static ThreadLocal<Integer> retryNum = new ThreadLocal() {
        @Override
        protected Integer initialValue() {
            return Integer.valueOf(0);
        }
    };

    public static ThreadLocal<Boolean> isNeedRetry = new ThreadLocal() {
        @Override
        protected Boolean initialValue() {
            return true;
        }
    };


}

