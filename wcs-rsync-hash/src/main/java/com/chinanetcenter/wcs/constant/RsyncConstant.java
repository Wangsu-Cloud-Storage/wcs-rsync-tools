package com.chinanetcenter.wcs.constant;


import com.chinanetcenter.wcs.pojo.Counter;

/**
 * Created by lidl on 2015-03-12
 */
public class RsyncConstant {
    public static Counter delAllNum = new Counter(0L);
    public static Counter delFailedNum = new Counter(0L);
    public static long compareHashIntersactTime = 0;//比对Hash,与服务器交互时间
    public static long compareHashselectTime = 0;//比对Hash,查询数据库时间
    public static Counter hashCompareFilesNum = new Counter(0L);//Hash比对文件个数
    public static Counter hashCompareSuccessAddNum = new Counter(0L);//Hash比对文件成功，文件新增上传
    public static Counter hashCompareSuccessUpdateNum = new Counter(0L); //Hash比对文件成功，文件覆盖上传
    public static Counter hashCompareSuccessNotUploadNum = new Counter(0L);//Hash比对文件成功，文件不需要上传
    public static Counter hashCompareFailedNum = new Counter(0L);///Hash比对失败
    public static Counter uploadFilesNum = new Counter(0L);//上传文件总数
    public static Counter uploadAddSuccessFilesNum = new Counter(0L);//新增上传成功文件个数
    public static Counter uploadUpdateSuccessFilesNum = new Counter(0L);//覆盖上传成功文件个数
    public static Counter uploadAddFailedFilesNum = new Counter(0L);//新增上传失败文件个数
    public static Counter uploadUpdateFailedFilesNum = new Counter(0L);//覆盖上传失败文件个数

    //不需要上传文件数
    public static Counter notUploadFileNum_alreadyUpload = new Counter(0L);//上次已经上传成功,不需上传
    public static Counter notUploadFileNum_notUpload_last = new Counter(0L);//上次同步时，已经认定文件不需上传
    public static Counter notUploadFileNum_notUpload_new = new Counter(0L);//本次同步时，Hash比较过程中认定文件不需上传
    public static Counter notUploadFileNum_less_minFileSize = new Counter(0L);//小于规定大小的文件不进行上传操作

    //本地磁盘总共文件个数
    public static Counter localFileAllNum = new Counter(0L);

}
