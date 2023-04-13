package com.chinanetcenter.wcs.constant;

/**
 * Created by lidl on 2015/3/9
 */
public enum SyncOperateEnum {
    /**
     * 不上传【初始状态】
     */
    notUpload(0),
    /**
     * 上传文件
     */
    add(1),
    /**
     * 覆盖上传
     */
    update(2),
    /**
     * 删除文件
     */
    delete(3);

    SyncOperateEnum(int value) {
        this.value = value;
    }

    private int value;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public static SyncOperateEnum get(int value) {
        for (SyncOperateEnum tmp : SyncOperateEnum.values()) {
            if (tmp.getValue() == value) {
                return tmp;
            }
        }
        throw new RuntimeException("could not find SyncOperateEnum by value, value is " + value);
    }
}
