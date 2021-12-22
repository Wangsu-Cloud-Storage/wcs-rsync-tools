## 文件同步工具
文件同步工具是基于网宿云存储提供的API开发的存量数据同步工具，可以将用户本地的数据以原有的目录结构同步到网宿云存储。

注：文件同步工具适用于存量文件的迁移，对于增量文件的同步请使用api上传接口

 - [下载链接](#下载链接)
 - [配置项](#配置项)
 - [命令行同步工具](#命令行同步工具)
 - [可视化同步工具](#可视化同步工具)

### **下载链接**
命令行同步工具：

[wcs-rsync-hash](https://wcsd.chinanetcenter.com/tool/wcs-rsync-hash.zip)



注：根据操作系统的不同，选择链接下载

### **配置项**
|参数|必填|描述|
|--|--|--|
|accessKey|是|可登陆[云存储控制台](https://wcs.chinanetcenter.com/login),在“安全管理--密钥管理”中获取。|
|secretKey|是|可登陆[云存储控制台](https://wcs.chinanetcenter.com/login),在“安全管理--密钥管理”中获取。|
|uploadDomain<br>上传域名|是|上传文件使用的域名，可登陆[云存储控制台](https://wcs.chinanetcenter.com/login)，在“空间设置--访问域名”中获取。|
|mgrDomain<br>管理域名|是|工具进行文件HASH值比对等操作时需要使用该管理域名，可登陆[云存储控制台](https://wcs.chinanetcenter.com/login)，在“空间设置--访问域名”中获取。|
|syncMode<br>同步模式|是|仅命令行同步工具支持。<br>默认配置为0，支持单空间多目录的上传模式，需要填写bucket和syncDir，keyPrefix按需填写<br>配置为1，支持多空间多文件的上传模式，需要填写bucketAndDir。|
|bucket<br>空间名称|否|文件保存到指定的空间，如images，不支持配置多个。<br>若是使用命令行同步工具，syncMode配置为0时必填。|
|syncDir<br>同步路径|否|上传文件的本地路径，如/data。支持配置多个路径，以"\|"间隔，如D:/pic-2\|D:/rsync3。<br>若是使用命令行同步工具，syncMode配置为0时必填。<br>*注意：无论Linux系统或是Windows系统，配置本地路径请使用/分隔符；Windows系统下的路径需要带盘符（如C:/data）*|
|keyPrefix<br>前缀|否|上传到云存储的文件添加指定的前缀，可配置多个，与syncDir的路径一一对应，默认为空。<br>例如：<br><1>keyPrefix配置为data/，上传文件1.apk，则该文件在云存储保存为data/1.apk，即云存储新增文件夹data，1.apk保存在该文件夹中；<br><2>keyPriefix配置为data，上传文件为1.apk，则该文件在云存储保存为data1.apk；<br><3>syncDir配置为D:/rsync1\|D:/rsync2\|D:/rsync3 keyPrefix配置为test1/\|test2/<br>则rsync1下的文件（文件夹）保存在云存储test1目录下，rsync2的文件（文件夹）保存在test2目录下，rsync3的文件（文件夹）保存在根目录下。若配置的keyPrefix多于syncDir，则多余的keyPrefix不生效，取前几个目录。<br>若是使用命令行同步工具，syncMode配置为0时填写。|
|bucketAndDir<br>目标空间及本地路径|否|仅命令行同步工具支持，syncMode配置为1时必填<br>如bucket1\|D:/dir1,D:/dir2\|prefix1,prefix2/;bucket2\|D:/dir3,D:/dir4<br>注：<br><1>每个空间可以配置多个本地路径，本地路径支持文件夹和文件，文件需要带上后缀名<br><2>本地路径和前缀一一对应，前缀的用法同参数keyPrefix<br><3>空间名、本地路径和前缀使用"\|"分隔，多个本地路径或多个前缀以英文逗号","分隔<br><4>可配置多个空间、本地路径和前缀的组合，以";"分隔。|
|threadNum<br>上传并发数|否|文件并发上传线程数。配置范围是1-100，默认值为1。<br>如果配置为５，则可同时上传５个文件。|
|sliceThreshold<br>分片上传阈值|否|文件大小如果大于该值，则采用分片上传。单位兆（M），配置范围1M－100M，默认为4M。|
|sliceThread<br>分片上传并发数|否|默认并发数为5，配置范围为1-100<br>如果配置为5，表示可并发上传5个分片|
|sliceBlockSize<br>块大小|否|分片上传块的大小。取值范围:4M-1024M，且为4的倍数。默认4M。|
|sliceChunkSize<br>片大小|否|文件分片上传时每个分片的大小，单位KB，配置范围是1024-1048576KB。<br>*注意：分片上传并发数、块大小、片大小这三个配置项只对分片上传有效。*|
|deletable<br>同步删除|否|配置为0，本地文件删除后，云存储上的文件不删除<br>配置为1，本地文件删除后，云存储上的文件也将删除<br>*注意：该配置只对上一次同步的文件生效，对其他历史同步文件不生效，默认为0，不同步删除*|
|maxRate<br>限速|否|上传速度限制，单位KB/s。配置为0则表示不限速。|
|taskBeginTime<br>开始时间|否|工具开始上传文件的时间，格式为hh:mm:ss，如12:00:00。|
|taskEndTime<br>停止时间|否|工具停止上传文件的时间，格式为hh:mm:ss，如15:00:00。|
|isCompareHash<br>是否比对HASH上传|否|配置为0，表示不进行HASH对比上传，<br>配置为1，表示进行HASH对比上传，默认值为1。|
|countHashThreadNum<br>Hash计算并发数|否|计算hash的线程数。配置范围是1-100，默认值为1。如果配置为10，则可同时计算10个文件hash。|
|compareHashThreadNum<br>Hash比对并发数|否|比对本地和云存储上文件Hash值一致性，判断是否需要重新上传。<br>该参数设置比对hash的线程数。配置范围是1-100，默认值为1。<br>如果配置为10，则可同时启动10个线程比对hash。|
|compareHashFileNum<br>Hash比对文件数|否|比对文件Hash时，一次性从云存储查询到的文件hash数量。配置范围是1-2000，默认值为100。<br>如果配置为100，则一次从服务器群查询100个文件的hash。|
|minFileSize<br>最小文件|否|小于规定大小的文件不进行上传操作。默认值为0(不限制)。<br>如果配置为1024，则小于1024字节的文件不进行上传操作。|
|overwrite<br>是否覆盖|否|是否覆盖云存储上同名文件，可配置为1或者0。1表示覆盖，0表示不覆盖，默认为1。|
|isLastModifyTime<br>是否更新服务端修改时间|否|保存在云存储的lastModifyTime是否以本地文件更新时间为准，可配置为0或者1，默认为0。<br>0:表示以上传时间为lastModifyTime。<br>1:表示以本地文件修改时间为lastModifyTime。|
|scanOnly<br>是否仅扫描文件列表|否|是否仅扫描文件列表。<br>默认为0时，正常上传文件<br>配置为1时，仅扫描文件列表，记录修改时间，不计算hash，不比对hash，不上传文件<br>备注：该项为风险配置项，在使用前请与云存储工作人员确认|
|uploadErrorRetry<br>上传失败重试数|否|文件上传失败进行自动重试的次数。<br>配置范围是0-5，默认值为0，表示不重试。<br>如果配置为2，则文件上传失败会自动重试2次。|

### **命令行同步工具**
#### **使用建议**

1. 预先安装java，JDK要求1.6以上版本。
2. 配置文件与工具放在相同路径

#### **使用方法**

1. 打开wcs-rsync-hash工具所在目录，如windows目录F:\wcs-rsync或者linux目录/home/tool/wcs-rsync
2. 配置conf.json
3. 启动服务
<br>windows下，可在空白处按住Shift键，点击右键，选择“在此处打开命令窗口(W)”
<br>执行命令：java -jar wcs-rsync-hash-xxx.jar conf.json
<br>*文件同步结束，若存在同步失败的文件，再次执行该命令，会重新同步上一次失败的文件*
4. 列出上传失败的文件
<br>执行命令：java -jar wcs-rsync-hash-xxx.jar -listfailed conf.json
<br>输出结果保存到工具目录下的log文件中
5. 强制重新上传所有文件：执行命令：java -jar wcs-rsync-hash-xxx.jar -igsync conf.json


