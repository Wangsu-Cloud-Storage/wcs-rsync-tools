package com.chinanetcenter.wcs.command;

import com.chinanetcenter.api.util.DateUtil;
import com.chinanetcenter.wcs.constant.RsyncConstant;
import com.chinanetcenter.wcs.core.RsyncCore;
import com.chinanetcenter.wcs.pojo.ConfJson;
import com.chinanetcenter.wcs.util.RsyncCheckUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * 程序主类
 * Created by lidl on 2015/3/9
 */
public class RsyncEntranceCommand {
    private static Logger logger = Logger.getLogger(RsyncEntranceCommand.class);

    public static void main(String[] args) throws Exception {
        Properties props = RsyncCheckUtil.loadLog4jProperties();//装载日志信息
        logger.info("Java的运行环境版本：" + System.getProperty("java.version"));
        if (System.getProperty("java.specification.version").compareTo("1.6") < 0) {
            logger.error("当前JDK版本过低,请安装1.6版本或者更高版本的JDK");
            return;
        }
        logger.info("当前系统的默认时区:" + TimeZone.getDefault());
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        // System.setProperty("user.timezone","Asia/Shanghai");
        logger.info("将时区改为:" + TimeZone.getDefault());
        if (args == null || args.length <= 0) {
            logger.error("请指定配置文件路径");
            return;
        }
        String commandStr = "commandStr:";
        for (String arg : args) {
            commandStr = commandStr + " " + arg;
        }
        logger.info(commandStr);
        if (StringUtils.equalsIgnoreCase(args[0], "-igsync")) {//忽略之前同步成功的文件，即删除本地DB，全部重新计算Hash
            if (args.length < 2) {
                logger.error("参数非法:-igsync [conf.json]");
                return;
            }
            ConfJson confJson = RsyncCheckUtil.getConfJson(args[1]);//解析配置文件
            if (!rsyncCommonCheck(confJson, props, true)) {
                return;
            }
            fileSync(confJson, true);
        } else if (StringUtils.equalsIgnoreCase(args[0], "-listfailed")) {//导出同步失败文件列表
            if (args.length < 2) {
                logger.error("参数非法:-listfailed [conf.json]");
                return;
            }
            ConfJson confJson = RsyncCheckUtil.getConfJson(args[1]);//解析配置文件
            if (!rsyncCommonCheck(confJson, props, false)) {
                return;
            }
            String logFilePath = "failed_" + DateUtil.formatDate(new Date(), DateUtil.SIMPLE_SECOND_PATTERN) + ".log";
            logger.info("开始导出同步失败文件列表->" + logFilePath);
            long beginSyncTime = System.currentTimeMillis();
            RsyncCore.listfailed(confJson, logFilePath);
            long costSyncTime = System.currentTimeMillis() - beginSyncTime;
            logger.info("导出同步失败文件列表结束,耗时" + costSyncTime + "毫秒.");
        } else {
            ConfJson confJson = RsyncCheckUtil.getConfJson(args[0]);//解析配置文件
            if (!rsyncCommonCheck(confJson, props, false)) {
                return;
            }
            fileSync(confJson, false);
        }
    }

    public static boolean rsyncCommonCheck(ConfJson confJson, Properties props, boolean overwriteDb) {
        if (confJson == null) {
            logger.error("无法解析配置文件,请检查");
            return false;
        }
//        logger.info(confJson.toString());
        String prefix = confJson.getLogPrefix();
        if(StringUtils.isNotEmpty(prefix)){
            boolean isWindowsFull = Pattern.matches("^[A-z]:.*", prefix);
            if(!prefix.startsWith("/") && !isWindowsFull ){
                prefix  = System.getProperty("user.dir") + File.separator + prefix;
            }
            prefix = prefix + "wcs-rsync-hash.log";
            props.setProperty("log4j.rootLogger", "info,R");
            props.setProperty("log4j.appender.R.File", prefix);
            PropertyConfigurator.configure(props);// 重新装入log4j配置信息
        }else{
            props.setProperty("log4j.rootLogger", "info,R,stdout");
            props.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
            props.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
            props.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%d{yyyy-MM-dd HH:mm:ss} - %p %l | %m%n");
            PropertyConfigurator.configure(props);// 重新装入log4j配置信息

        }
        if (!RsyncCheckUtil.confJsonValidate(confJson)) {//校验配置文件
            return false;
        }

        if (!StringUtils.equalsIgnoreCase(confJson.getLogLevel(), "debug")) {
            props.setProperty("log4j.logger.com", confJson.getLogLevel());
            PropertyConfigurator.configure(props);// 重新装入log4j配置信息
        }


        if (confJson.syncMode == 0) {//多空间不在这里加载数据
            if (!RsyncCheckUtil.tokenValidate()) {//Token校验
                return false;
            }
            if (!RsyncCheckUtil.loadDataBase(confJson, overwriteDb)) {//加载数据文件
                return false;
            }
        }
        return true;
    }

    public static void fileSync(ConfJson confJson, boolean overwriteDB) throws Exception {
        logger.info("开始同步文件......");
        long beginSyncTime = System.currentTimeMillis();
        RsyncCore.fileSync(confJson, overwriteDB);
        long costSyncTime = System.currentTimeMillis() - beginSyncTime;
        logger.debug("Hash比对与Sqlite交互的时间:" + RsyncConstant.compareHashselectTime + "毫秒.");
        logger.debug("Hash比对与WCS交互的时间:" + RsyncConstant.compareHashIntersactTime + "毫秒.");
        long hashCompareFilesNum = RsyncConstant.hashCompareFilesNum.get();//Hash比对文件个数
        long hashCompareSuccessAddNum = RsyncConstant.hashCompareSuccessAddNum.get();//Hash比对文件成功，文件新增上传
        long hashCompareSuccessUpdateNum = RsyncConstant.hashCompareSuccessUpdateNum.get();//Hash比对文件成功，文件覆盖上传
        long hashCompareSuccessNotUploadNum = RsyncConstant.hashCompareSuccessNotUploadNum.get();//Hash比对文件成功，文件不需要上传
        long hashCompareFailedNum = RsyncConstant.hashCompareFailedNum.get();//Hash比对失败

        long uploadFilesNum = RsyncConstant.uploadFilesNum.get();//上传文件总个数
        long uploadAddSuccessFilesNum = RsyncConstant.uploadAddSuccessFilesNum.get();//新增上传成功文件个数
        long uploadUpdateSuccessFilesNum = RsyncConstant.uploadUpdateSuccessFilesNum.get();//覆盖上传成功文件个数
        // long uploadAddFailedFilesNum = RsyncConstant.uploadAddFailedFilesNum.get();//新增上传失败文件个数
        // long uploadUpdateFailedFilesNum = RsyncConstant.uploadUpdateFailedFilesNum.get();//覆盖上传失败文件个数

        //分片上传时，如果一个文件有多个块，每个块上传失败就累加一次导致累计值偏大，改为查询数据库
//        long uploadAddFailedFilesNum = SqliteDbUtil.getAddUploadFailFilesNum();//新增上传失败文件个数
//        long uploadUpdateFailedFilesNum = SqliteDbUtil.getUpdateUploadFailFilesNum();//覆盖上传失败文件个数

        long uploadAddFailedFilesNum = RsyncConstant.uploadAddFailedFilesNum.get();//新增上传失败文件个数
        long uploadUpdateFailedFilesNum = RsyncConstant.uploadUpdateFailedFilesNum.get();//覆盖上传失败文件个数
        long notUploadFileNum_alreadyUpload = RsyncConstant.notUploadFileNum_alreadyUpload.get();//上次已经上传成功,不需上传
        long notUploadFileNum_notUpload_last = RsyncConstant.notUploadFileNum_notUpload_last.get();//上次同步时，已经认定文件不需上传
        long notUploadFileNum_notUpload_new = RsyncConstant.notUploadFileNum_notUpload_new.get();//本次同步时，Hash比较过程中认定文件不需上传
        long notUploadFileNum_less_minFileSize = RsyncConstant.notUploadFileNum_less_minFileSize.get();//小于规定大小的文件不进行上传操作
        long notUploadNum = notUploadFileNum_alreadyUpload + notUploadFileNum_notUpload_last + notUploadFileNum_notUpload_new + notUploadFileNum_less_minFileSize;//不需上传文件个数

        //删除
        long delAllNum = RsyncConstant.delAllNum.get();
        long delFailedNum = RsyncConstant.delFailedNum.get();
        long delSuccess = delAllNum - delFailedNum;
        // 总共文件个数50,新增上传文件个数:10,覆盖上传文件个数:10,不需要上传文件个数:10,删除文件个数：10,失败个数：10
        StringBuilder logs = new StringBuilder("\n==================供调试查看=======================\n");
        logs.append("同步文件结束,耗时").append(costSyncTime).append("毫秒.").append("\n");
        logs.append("Hash比对文件个数:").append(hashCompareFilesNum).append(", 成功:").append((hashCompareSuccessAddNum + hashCompareSuccessUpdateNum + hashCompareSuccessNotUploadNum)).append("(包括新增上传文件个数:").append(hashCompareSuccessAddNum).append(",覆盖上传文件个数:").append(hashCompareSuccessUpdateNum).append(",不需上传文件个数:").append(hashCompareSuccessNotUploadNum).append("),失败:").append(hashCompareFailedNum).append("\n");
        logs.append("实际上传文件个数:" + uploadFilesNum).append("包括(\n");
        logs.append("新增上传成功:").append(uploadAddSuccessFilesNum).append(",新增上传失败:").append(uploadAddFailedFilesNum).append("\n");
        logs.append("覆盖上传成功:").append(uploadUpdateSuccessFilesNum).append(",覆盖上传失败:").append(uploadUpdateFailedFilesNum).append("\n)\n");
        logs.append("不需上传文件:").append(notUploadNum).append("包括(\n");
        logs.append("上次已经上传成功,不需上传文件数:").append(notUploadFileNum_alreadyUpload).append("\n");
        logs.append("上次同步时，已经认定文件不需上传:").append(notUploadFileNum_notUpload_last).append("\n");
        logs.append("本次同步时上传文件小于规定大小，认定文件不需上传:").append(notUploadFileNum_less_minFileSize).append("\n");
        logs.append("本次同步时，Hash比较过程中认定文件不需上传:").append(notUploadFileNum_notUpload_new).append("\n)\n");
        logs.append("删除文件数:").append(delAllNum).append(",成功:").append(delSuccess).append("失败:").append(delFailedNum).append("\n");
        logs.append("总的失败文件数:").append((uploadAddFailedFilesNum + uploadUpdateFailedFilesNum + delFailedNum + hashCompareFailedNum)).append("\n");
        logs.append("==================================================");
        logger.debug(logs.toString());


        //总共文件个数50
        // 新增上传文件个数:10
        // 覆盖上传文件个数:10
        //不需要上传文件个数:10
        //删除文件个数:10
        //失败个数:10
        StringBuilder logsUser = new StringBuilder();
        logsUser.append("本地磁盘总共文件个数:").append(RsyncConstant.localFileAllNum.get()).append("\n");
        logsUser.append("新增上传成功文件个数:").append(uploadAddSuccessFilesNum).append("\n");
        logsUser.append("覆盖上传成功文件个数:").append(uploadUpdateSuccessFilesNum).append("\n");
        logsUser.append("不需要上传文件个数:").append(notUploadNum).append("\n");
        logsUser.append("删除文件成功个数:").append(delSuccess).append("\n");
        logsUser.append("总失败文件个数:").append((uploadAddFailedFilesNum + uploadUpdateFailedFilesNum + delFailedNum + hashCompareFailedNum)).append("\n");
        logger.info(logsUser.toString());
        System.out.println(logsUser.toString());
    }
}
