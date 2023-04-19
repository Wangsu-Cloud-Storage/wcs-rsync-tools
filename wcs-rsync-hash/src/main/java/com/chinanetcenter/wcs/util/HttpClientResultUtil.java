package com.chinanetcenter.wcs.util;

import com.chinanetcenter.api.entity.HttpClientResult;
import com.chinanetcenter.api.entity.PutPolicy;
import com.chinanetcenter.api.exception.WsClientException;
import com.chinanetcenter.api.http.HttpClientUtil;
import com.chinanetcenter.api.sliceUpload.BaseBlockUtil;
import com.chinanetcenter.api.util.Config;
import com.chinanetcenter.api.util.EncodeUtils;
import com.chinanetcenter.api.util.JsonMapper;
import com.chinanetcenter.wcs.command.BaseCommand;
import com.chinanetcenter.wcs.core.RsyncCore;
import com.chinanetcenter.wcs.pojo.ConfJson;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

//import com.chinanetcenter.api.exception.HttpClientException;

/**
 * Created by xiexb on 2014/7/13.
 */
public class HttpClientResultUtil {
    public static final String USER_AGENT = "wcs-rsync-hash-1.7.3";//同步工具UA
    public static String AK = "";
    public static String SK = "";
    public static String BUCKET = "";
    public static String UPLOAD_DOMAIN = "http://wrsync.wcs.biz.matocloud.com";//REST地址
    public static String MGR_DOMAIN = "http://wrsync.wcs.biz.matocloud.com";//默认直接上传至REST,特殊情况可以让用户配置上传至上传节点
    public static long sliceLimitSize;
    private static Logger logger = Logger.getLogger(RsyncCheckUtil.class);

    /**
     * 根据配置文件进行初始化
     *
     * @param confJson
     */
    public static void init(ConfJson confJson) {
        if (StringUtils.isNotBlank(confJson.uploadDomain)) {
            UPLOAD_DOMAIN = StringUtils.startsWith(confJson.uploadDomain, "http://") || StringUtils.startsWith(confJson.uploadDomain, "https://") ? confJson.uploadDomain : "http://" + confJson.uploadDomain;
        }
        if (StringUtils.isNotBlank(confJson.mgrDomain)) {
            MGR_DOMAIN = StringUtils.startsWith(confJson.mgrDomain, "http://") || StringUtils.startsWith(confJson.mgrDomain, "https://") ? confJson.mgrDomain : "http://" + confJson.mgrDomain;
        }
        AK = confJson.accessKey;
        SK = confJson.secretKey;
        BUCKET = confJson.bucket;
        sliceLimitSize = confJson.sliceThreshold * BaseCommand.MB;//分片上传文件大小限制
        /***
         * 由于直接使用了wcs-java-sdk,以下需要进行设置
         */
        if (StringUtils.isNotBlank(confJson.logFilePath)) {
            Config.init(AK, SK, confJson.logFilePath);
        } else {
            Config.init(AK, SK);//设置分片上传的AK/SK
        }
        Config.PUT_URL = UPLOAD_DOMAIN;//设置分片上传的地址


        BaseBlockUtil.THREAD_NUN = confJson.sliceThread;
        BaseBlockUtil.BLOCK_SIZE = (int) (confJson.sliceBlockSize * BaseCommand.MB);
        BaseBlockUtil.CHUNK_SIZE = (int) (confJson.sliceChunkSize * BaseCommand.KB);
//        BaseBlockUtil.maxRate = confJson.maxRate;
//        OverAllBandwidthLimiterUtils.setMaxRate(confJson.maxRate);
//        HttpLogUtil.loadLog4jProperties();//加载httplog日志文件配置信息
    }

    /**
     * 上传文件
     *
     * @param fileKey
     * @param file
     * @return
     */
    public static HttpClientResult uploadResult(String fileKey, File file) throws Exception {
        PutPolicy putPolicy = new PutPolicy();
        putPolicy.setDeadline(String.valueOf(DateUtil.nextSecond(3600, new Date()).getTime()));
        putPolicy.setScope(BUCKET + ":" + fileKey);
        putPolicy.setFsizeLimit(2 * BaseCommand.GB);//2G
        putPolicy.setOverwrite(RsyncCore.confJson.overwrite);//默认直接覆盖
        if (RsyncCore.confJson.isLastModifyTime == 1) {
            putPolicy.setLastModifiedTime(String.valueOf(file.lastModified()));
        }
        JsonMapper putJsonMapper = JsonMapper.nonEmptyMapper();
        String uploadPutpolicy = putJsonMapper.toJson(putPolicy);
        String putSigningStr = EncodeUtils.urlsafeEncode(uploadPutpolicy);
        String putSkValue = BaseCommand.produceToken(SK, putSigningStr);
        String uploadToken = AK + ":" + putSkValue + ":" + putSigningStr;
        Map<String, String> uploadParamMap = new HashMap<String, String>();
        uploadParamMap.put("token", uploadToken);
        Map<String, String> headMap = new HashMap<String, String>();
        headMap.put("User-Agent", USER_AGENT);
        HttpClientResult httpClientResult = HttpClientUtil.httpPost(UPLOAD_DOMAIN + "/file/upload", uploadParamMap, headMap, file);
        return httpClientResult;
    }

    /**
     * 删除文件
     *
     * @param fileKey
     * @return
     */
    public static HttpClientResult deleteResult(String fileKey) throws Exception {
        String entry = BUCKET + ":" + fileKey;
        String encodedEntryURI = EncodeUtils.urlsafeEncode(entry);
        Map<String, String> headMap = new HashMap<String, String>();
        String signingStr = "/delete/" + encodedEntryURI + "\n";
        String encodedSign = BaseCommand.produceToken(SK, signingStr);
        String accessToken = AK + ":" + encodedSign;
        headMap.put("Authorization", accessToken);
        headMap.put("User-Agent", USER_AGENT);
        HttpClientResult httpClientResult = HttpClientUtil.httpPost(MGR_DOMAIN + "/fileManageCmd/delete/" + encodedEntryURI, null, headMap, null);
        return httpClientResult;
    }

    /**
     * 启动时鉴权校验
     */
    public static HttpClientResult filesyncResult() throws Exception {
        String entry = BUCKET;//空间名
        String encodedEntryURI = EncodeUtils.urlsafeEncode(entry);
        Map<String, String> headMap = new HashMap<String, String>();
        String signingStr = "/filesync/" + encodedEntryURI + "\n";
        String encodedSign = BaseCommand.produceToken(SK, signingStr);
        String accessToken = AK + ":" + encodedSign;
        headMap.put("Authorization", accessToken);
        headMap.put("User-Agent", USER_AGENT);
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("=========wcsRsyncHash Auth=============\n");
        stringBuffer.append("accessToken:" + accessToken + "\n");
        stringBuffer.append("User-Agent:" + USER_AGENT + "\n");
        stringBuffer.append("auth address:" + MGR_DOMAIN + "/filesync/" + encodedEntryURI + "\n");
        stringBuffer.append("=======================================\n");
        logger.info(stringBuffer);
        HttpClientResult httpClientResult = HttpClientUtil.httpPost(MGR_DOMAIN + "/filesync/" + encodedEntryURI, null, headMap, null);
        return httpClientResult;
    }

    /**
     *
     */
    public static HttpClientResult getFilesHash(String filesJson) throws Exception {
        String entry = BUCKET;//空间名
        String encodedEntryURI = EncodeUtils.urlsafeEncode(entry);
        String signingStr = "/getFilesHash/" + encodedEntryURI + "\n";
        String encodedSign = BaseCommand.produceToken(SK, signingStr);
        String accessToken = AK + ":" + encodedSign;
        String url = MGR_DOMAIN + "/fileManageCmd/getFilesHash/" + encodedEntryURI;
        HttpPost httpPost = null;
        CloseableHttpResponse ht = null;
        String response = "";
        try {
            httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", accessToken);
            httpPost.addHeader("User-Agent", USER_AGENT);
            httpPost.setEntity(new StringEntity(EncodeUtils.urlsafeEncode(filesJson)));
            CloseableHttpClient hc = HttpClients.createDefault();
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(120000).setConnectTimeout(120000).build();//设置请求和传输超时时间
            httpPost.setConfig(requestConfig);
            ht = hc.execute(httpPost);
            HttpEntity het = ht.getEntity();
            InputStream is = het.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf8"));
            String readLine;
            while ((readLine = br.readLine()) != null) {
                response = response + readLine;
            }
            is.close();
            br.close();
            int status = ht.getStatusLine().getStatusCode();
            if (status == 200) {
                response = EncodeUtils.urlsafeDecodeString(response);
            }
            return new HttpClientResult(status, response);
        } catch (Exception e) {
            throw new WsClientException(e);
        } finally {
            if (httpPost != null) {
                httpPost.releaseConnection();
            }
            if (ht != null) {
                try {
                    ht.close();
                } catch (IOException ignored) {
                }
            }
        }
    }


}
