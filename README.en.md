# File Synchronization Tool

## Language
- [简体中文](README.md)
- [English](README.en.md)

The file synchronization tool is a stock data synchronization tool developed based on the API provided by Wangsu Cloud Storage. It can synchronize user's local data to Wangsu Cloud Storage with the original directory structure.

Note: The file synchronization tool is suitable for migration of stock files. For synchronization of incremental files, please use the API upload interface.

 - [Download Links](#download-links)
 - [Configuration Items](#configuration-items)
 - [Command Line Synchronization Tool](#command-line-synchronization-tool)
 - [Visual Synchronization Tool](#visual-synchronization-tool)

## **Download Links**
Command line synchronization tool:
- [wcs-rsync-hash](https://wcsd.chinanetcenter.com/tool/wcs-rsync-hash.zip)

Visual synchronization tool:
- [wcs-websync-windows-x86](https://wcsd.chinanetcenter.com/tool/wcs-websync-windows-32-1.1.0.zip)
- [wcs-websync-windows-x64](https://wcsd.chinanetcenter.com/tool/wcs-websync-windows-64-1.1.0.zip)
- [wcs-websync-linux-x86](https://wcsd.chinanetcenter.com/tool/wcs-websync-linux-32-1.1.0.zip)
- [wcs-websync-linux-x64](https://wcsd.chinanetcenter.com/tool/wcs-websync-linux-64-1.1.0.zip)



Note: Choose the link to download according to your operating system

## **Configuration Items**
| Parameter | Required | Description |
|--|--|--|
| accessKey | Yes | You can log in to the [Cloud Storage Console](https://wcs.chinanetcenter.com/login) and obtain it in "Security Management - Key Management". |
| secretKey | Yes | You can log in to the [Cloud Storage Console](https://wcs.chinanetcenter.com/login) and obtain it in "Security Management - Key Management". |
| uploadDomain<br>Upload Domain | Yes | The domain name used for uploading files. You can log in to the [Cloud Storage Console](https://wcs.chinanetcenter.com/login) and obtain it in "Space Settings - Access Domain". |
| mgrDomain<br>Management Domain | Yes | The management domain name is required when the tool performs operations such as file HASH value comparison. You can log in to the [Cloud Storage Console](https://wcs.chinanetcenter.com/login) and obtain it in "Space Settings - Access Domain". |
| syncMode<br>Synchronization Mode | Yes | Only supported by the command line synchronization tool.<br>The default configuration is 0, which supports single-space multi-directory upload mode. You need to fill in bucket and syncDir, and keyPrefix as needed.<br>Configure to 1, which supports multi-space multi-file upload mode. You need to fill in bucketAndDir. |
| bucket<br>Space Name | No | Save files to the specified space, such as images, multiple configurations are not supported.<br>If using the command line synchronization tool, it is required when syncMode is configured to 0. |
| syncDir<br>Synchronization Path | No | Local path for uploading files, such as /data. Multiple paths are supported, separated by "\|", such as D:/pic-2\|D:/rsync3.<br>If using the command line synchronization tool, it is required when syncMode is configured to 0.<br>*Note: Regardless of Linux or Windows system, please use / separator for local path configuration; Windows system path needs to have a drive letter (such as C:/data)* |
| keyPrefix<br>Prefix | No | Add a specified prefix to the files uploaded to cloud storage. Multiple prefixes can be configured, corresponding to the syncDir paths one by one, default is empty.<br>For example:<br><1> If keyPrefix is configured as data/, and file 1.apk is uploaded, the file will be saved in cloud storage as data/1.apk, that is, a new folder data is added to cloud storage, and 1.apk is saved in this folder;<br><2> If keyPriefix is configured as data, and the uploaded file is 1.apk, the file will be saved in cloud storage as data1.apk;<br><3> syncDir is configured as D:/rsync1\|D:/rsync2\|D:/rsync3, keyPrefix is configured as test1/\|test2/<br>Then the files (folders) under rsync1 are saved in the cloud storage test1 directory, the files (folders) of rsync2 are saved in the test2 directory, and the files (folders) of rsync3 are saved in the root directory. If more keyPrefixes are configured than syncDir, the extra keyPrefixes will not take effect, and the first few directories will be taken.<br>If using the command line synchronization tool, fill it in when syncMode is configured to 0. |
| bucketAndDir<br>Target Space and Local Path | No | Only supported by the command line synchronization tool, required when syncMode is configured to 1.<br>Such as bucket1\|D:/dir1,D:/dir2\|prefix1,prefix2/;bucket2\|D:/dir3,D:/dir4<br>Note:<br><1> Each space can be configured with multiple local paths, local paths support folders and files, files need to have suffix names<br><2> Local paths and prefixes correspond one by one, the usage of prefixes is the same as the parameter keyPrefix<br><3> Space name, local path and prefix are separated by "\|", multiple local paths or multiple prefixes are separated by English comma ","<br><4> Multiple combinations of space, local path and prefix can be configured, separated by ";". |
| threadNum<br>Upload Concurrency | No | Number of concurrent file upload threads. The configuration range is 1-100, the default value is 1.<br>If configured as 5, 5 files can be uploaded at the same time. |
| sliceThreshold<br>Shard Upload Threshold | No | If the file size is larger than this value, shard upload is adopted. Unit is megabytes (M), configuration range is 1M-100M, default is 4M. |
| sliceThread<br>Shard Upload Concurrency | No | The default concurrency is 5, the configuration range is 1-100.<br>If configured as 5, it means 5 shards can be uploaded concurrently. |
| sliceBlockSize<br>Block Size | No | The size of the shard upload block. Value range: 4M-1024M, and it is a multiple of 4. Default is 4M. |
| sliceChunkSize<br>Chunk Size | No | The size of each shard when uploading files in shards, unit is KB, configuration range is 1024-1048576KB.<br>*Note: The three configuration items of shard upload concurrency, block size, and chunk size are only valid for shard upload.* |
| deletable<br>Synchronous Deletion | No | Configure to 0, after local files are deleted, files on cloud storage are not deleted.<br>Configure to 1, after local files are deleted, files on cloud storage will also be deleted.<br>*Note: This configuration only takes effect on files synchronized last time, not on other historical synchronized files, default is 0, no synchronous deletion.* |
| maxRate<br>Speed Limit | No | Upload speed limit, unit KB/s. Configure to 0 means no speed limit. |
| taskBeginTime<br>Start Time | No | The time when the tool starts uploading files, format is hh:mm:ss, such as 12:00:00. |
| taskEndTime<br>Stop Time | No | The time when the tool stops uploading files, format is hh:mm:ss, such as 15:00:00. |
| isCompareHash<br>Whether to Compare HASH | No | Configure whether to compare the Hash values of local files and files with the same name on cloud storage.<br>Configure to 0, means no HASH comparison, upload directly,<br>Configure to 1, means to perform HASH comparison, if the hash of files with the same name is the same, they will not be uploaded again, the default value is 1. |
| countHashThreadNum<br>Hash Calculation Concurrency | No | Number of threads for calculating hash. The configuration range is 1-100, the default value is 1. If configured as 10, 10 file hashes can be calculated at the same time. |
| compareHashThreadNum<br>Hash Comparison Concurrency | No | Compare the Hash value consistency of local and cloud storage files to determine whether they need to be re-uploaded.<br>This parameter sets the number of threads for comparing hash. The configuration range is 1-100, the default value is 1.<br>If configured as 10, 10 threads can be started at the same time to compare hash. |
| compareHashFileNum<br>Hash Comparison File Number | No | When comparing file Hash, the number of file hashes queried from cloud storage at one time. The configuration range is 1-2000, the default value is 100.<br>If configured as 100, 100 file hashes are queried from the server group at one time. |
| minFileSize<br>Minimum File | No | Files smaller than the specified size will not be uploaded. The default value is 0 (unlimited).<br>If configured as 1024, files smaller than 1024 bytes will not be uploaded. |
| overwrite<br>Whether to Overwrite | No | Whether to overwrite files with the same name on cloud storage, can be configured as 1 or 0. 1 means overwrite, 0 means not overwrite, default is 1. |
| isLastModifyTime<br>Whether to Update Server Modification Time | No | Whether the lastModifyTime saved in cloud storage is based on the local file update time, can be configured as 0 or 1, default is 0.<br>0: means to use upload time as lastModifyTime.<br>1: means to use local file modification time as lastModifyTime. |
| scanOnly<br>Whether to Only Scan File List | No | Whether to only scan file list.<br>Default is 0, upload files normally.<br>Configure to 1, only scan file list, record modification time, do not calculate hash, do not compare hash, do not upload files.<br>Note: This is a risk configuration item, please confirm with cloud storage staff before using. |
| uploadErrorRetry<br>Upload Failure Retry Count | No | The number of automatic retries for file upload failures.<br>The configuration range is 0-5, the default value is 0, which means no retry.<br>If configured as 2, file upload failures will be automatically retried 2 times. |
| logLevel | No | Effective for command line tools, specify the log level to be printed, optional debug, info, error three log levels. |
| logPrefix | No | Effective for command line tools, specify the log prefix to be printed, default is printed in the current path, if logPrefix=/tmp/ is specified, the full name of the generated log file is /tmp/wcs-rsync-hash.log. |

## **Command Line Synchronization Tool**
### **Usage Suggestions**

1. Install Java in advance, JDK requires version 1.6 or above.
2. Place the configuration file in the same path as the tool.

### **Usage Method**

1. Open the directory where the wcs-rsync-hash tool is located, such as windows directory F:\wcs-rsync or linux directory /home/tool/wcs-rsync.
2. Configure conf.json.
3. Start the service.
<br>On windows, you can hold down the Shift key in the blank area, click the right mouse button, and select "Open command window here (W)".
<br>Execute the command: java -jar wcs-rsync-hash-xxx.jar conf.json.
<br>*After file synchronization ends, if there are files that failed to synchronize, execute the command again, and the files that failed last time will be resynchronized*.
4. List files that failed to upload.
<br>Execute the command: java -jar wcs-rsync-hash-xxx.jar -listfailed conf.json.
<br>The output result is saved in the log file under the tool directory.
5. Forcibly re-upload all files: execute the command: java -jar wcs-rsync-hash-xxx.jar -igsync conf.json.
6. If an exception occurs during the synchronization process, you can check the `wcs-rsync-hash.log` log generated in the synchronization tool path, and analyze the failure reason through ERROR level logs.

## **Visual Synchronization Tool**
### **Usage Method**
#### 1. Startup
1. Edit the password configuration in \service\wcsrsynchashweb\conf.json to set the console login password.
2. Run startup.bat to start the service.
3. A TOMCAT running window will pop up. After the service is successfully started, the browser will automatically open and enter the operation interface. After the service is started, you can close the TOMCAT window without affecting use.
4. During the service startup period, on other machines, you can also implement remote operations through the browser, that is, open the browser and enter IP:Port to enter the operation interface.
//Note: Port is 8091 by default

#### 2. Usage
1. Configure accessKey, secretKey, bucket (storage space name), local synchronization path, upload domain name, and management domain name. Note: AccessKey, secretKey, upload and management domain names can be obtained from the object storage console.
2. After configuration is complete, click the "Upload Now" button to upload.
3. Click the "Progress Query" button to view the current synchronization progress.
4. Click "Log Download" to view synchronization process logs or synchronization exception logs.
![image](https://user-images.githubusercontent.com/28889058/159418991-31b3aa94-1d41-4730-8f35-ccaef80aa049.png)
![image](https://user-images.githubusercontent.com/28889058/159419658-0d5fc45d-f95e-4450-ab31-8a2abd29c80b.png)
![image](https://user-images.githubusercontent.com/28889058/159419497-b4def2a4-c1d4-4c50-aa42-3b1fc6a274cb.png)



#### 3. Shutdown:
1. Run shutdown.bat to close the service