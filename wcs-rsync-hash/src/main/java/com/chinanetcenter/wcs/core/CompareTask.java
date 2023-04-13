package com.chinanetcenter.wcs.core;

import com.chinanetcenter.api.entity.HttpClientResult;
import com.chinanetcenter.api.util.JsonMapper;
import com.chinanetcenter.wcs.constant.RsyncConstant;
import com.chinanetcenter.wcs.constant.SyncOperateEnum;
import com.chinanetcenter.wcs.pojo.FileMeta;
import com.chinanetcenter.wcs.pojo.ServerFileMeta;
import com.chinanetcenter.wcs.util.HttpClientResultUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lidl on 15-3-10.
 */
public class CompareTask implements Runnable {
    private static Logger logger = Logger.getLogger(CompareTask.class);
    private List<FileMeta> fileMetaList;

    public CompareTask(List<FileMeta> fileMetaList) {
        this.fileMetaList = fileMetaList;
    }

    @Override
    public void run() {
        String filesJson = convertFileMetaList(fileMetaList);
        try {
            logger.debug("发送到服务器的文件列表JSON:" + filesJson);
            long startTime1 = System.currentTimeMillis();
            HttpClientResult httpClientResult = HttpClientResultUtil.getFilesHash(filesJson);
            RsyncConstant.compareHashIntersactTime += (System.currentTimeMillis() - startTime1);
            String result = httpClientResult.getInnerResponse();
            Map<String, ServerFileMeta> serverFileMetaMap = convertStrToServerFileMetaMap(result);
            for (FileMeta fileMeta : fileMetaList) {
                String fileKey = fileMeta.getFileKey();
                if (!serverFileMetaMap.containsKey(fileKey)) {//若该文件在服务器上找不到，更新记录的状态为add.
                    fileMeta.setOperate(SyncOperateEnum.add.getValue());
                    fileMeta.setCompareStatus(2);
                    RsyncConstant.hashCompareSuccessAddNum.getAndIncrement();
                    logger.debug("比对文件：" + fileMeta.getFileKey() + " Hash,文件在服务器上找不到需要上传add");
                } else {
                    ServerFileMeta serverFileMeta = serverFileMetaMap.get(fileKey);
                    if (StringUtils.isNotBlank(serverFileMeta.getHash()) && serverFileMeta.getHash().equals(fileMeta.getHash())) {//若该文件在服务器上找到且hash一样。
                        RsyncConstant.notUploadFileNum_notUpload_new.getAndIncrement();
                        fileMeta.setOperate(SyncOperateEnum.notUpload.getValue());
                        fileMeta.setCompareStatus(2);
                        RsyncConstant.hashCompareSuccessNotUploadNum.getAndIncrement();
                        logger.debug("比对文件：" + fileMeta.getFileKey() + " Hash,文件在服务器上找到且hash一样,不需要上传notUpload");

                    } else {//若该文件在服务器上找到但hash不一样，更新记录的状态为update.
                        fileMeta.setOperate(SyncOperateEnum.update.getValue());
                        fileMeta.setCompareStatus(2);
                        RsyncConstant.hashCompareSuccessUpdateNum.getAndIncrement();
                        logger.debug("比对文件：" + fileMeta.getFileKey() + " Hash,文件在服务器上找到但hash不一样update");
                    }
                }
                RsyncCore.fileMetaUpdateQueue.add(fileMeta);
            }
        } catch (Exception e) {
            logger.error("比对hash失败:" + e.getMessage(), e);
            for (FileMeta fileMeta : fileMetaList) {
                fileMeta.setOperate(SyncOperateEnum.notUpload.getValue());
                fileMeta.setCompareStatus(3);
                RsyncConstant.hashCompareFailedNum.getAndIncrement();
                RsyncCore.fileMetaUpdateQueue.add(fileMeta);
            }
        }
    }

    public String convertFileMetaList(List<FileMeta> fileMetaList) {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        for (FileMeta fileMeta : fileMetaList) {//如果有前缀，文件名前加前缀
            arrayNode.add(fileMeta.getFileKey());
        }
        return arrayNode.toString();
    }

    public Map<String, ServerFileMeta> convertStrToServerFileMetaMap(String serverFileMetaListJson) {
        Map<String, ServerFileMeta> serverFileMetaMap = new HashMap<String, ServerFileMeta>();
        logger.debug("从服务器获取文件Hash JSON:" + serverFileMetaListJson.toString());
        JsonMapper jsonMapper = JsonMapper.nonEmptyMapper();
        JavaType javaType = jsonMapper.contructCollectionType(ArrayList.class, ServerFileMeta.class);
        List<ServerFileMeta> list = jsonMapper.fromJson(serverFileMetaListJson, javaType);//解析json字符串转换为ServerFileMeta对象
        for (int j = 0; j < list.size(); j++) {
            ServerFileMeta serverFileMeta = list.get(j);
            serverFileMetaMap.put(serverFileMeta.getFileName(), serverFileMeta);
        }
        return serverFileMetaMap;
    }
}
