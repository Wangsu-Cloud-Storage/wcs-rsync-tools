package com.chinanetcenter.wcs.core;

import com.chinanetcenter.api.sliceUpload.JSONObjectRet;

/**
 * Created by wuyz on 2018/1/30.
 */
public abstract class MyJSONObjectRet extends JSONObjectRet {

    public int uploadErrorRetry;

    public MyJSONObjectRet() {
    }

    public MyJSONObjectRet(int uploadErrorRetry) {
        this.uploadErrorRetry = uploadErrorRetry;
    }

}
