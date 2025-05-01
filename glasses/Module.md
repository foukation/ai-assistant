# Module AI-Assist SDK

## 1. 概述

### 版本信息
- SDK 版本：0.9.1
- 发布日期：2025-02
- 文档更新：2025-02-21

### 系统要求
- Android API Level：21+
- Java Version：11+
- Kotlin Version：1.8+

### 主要功能
- AI 语音识别和对话
- 设备管理和连接
- 网关服务集成

## 2. 快速开始

### 依赖配置
```gradle
dependencies {

    implementation platform('org.jetbrains.kotlin:kotlin-bom:1.8.0')

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.jakewharton.timber:timber:4.7.1")

    // Kotlin Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1'

    implementation 'com.github.gzu-liyujiang:Android_CN_OAID:4.2.11'
    implementation "com.huawei.hms:ads-identifier:3.4.62.300"
    implementation 'com.hihonor.mcs:ads-identifier:1.0.2.301'
    implementation("com.jakewharton.timber:timber:4.7.1")
    
    // SDK
    implementation(name: 'glasses-SDK0.9.0_', ext: 'aar')
    implementation(name: 'SDK2.1.11-Java32_T1', ext: 'aar')
}
```

### 权限配置
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### 初始化步骤
```kotlin
// 1. 创建配置
AIAssistConfig config = new AIAssistConfig.Builder()
    .setXAipId("your_aip_id")
    .setXAipUid("device_mac_address")
    .setXAipProxy("host:port")
    .setProductId("1765210262753239042")
    .setProductKey("BvbpVleWUlRdcnVM")
    .setDeviceNo("DYTCN100PROD0001")
    .setDeviceNoType("TCN100")
    .setToken("user_token")
    .setSn("random_string")
    .build();

// 2. 检查配置是否有效,初始化 SDK 
if (config.isValid()) {
    // 使用配置初始化
    AIAssistantManager.Companion.initialize(this, config);
}

// 3. 获取网关实例
AIAssistantManager.Companion.getInstance().gateWayHelp();

// 4. 获取设备纳管实例
AIAssistantManager.Companion.getInstance().deviceHelp();

// 5. 获取 asr 智能对话实例
AIAssistantManager.Companion.getInstance().asrIntelligentDialogueHelp();
```

## 3. 接口定义

### 配置管理
AIAssistConfig 提供以下配置选项：
- 应用标识配置
- 设备信息配置
- 网络代理配置
- 认证信息配置

### 设备连接
设备连接管理功能：
- 设备发现和配对
- 连接状态监控
- 设备信息获取
- 设备信息上报

### 语音识别
语音识别和对话功能：
- 实时语音识别
- 智能对话交互
- 语音命令处理

### 网关服务
网关服务功能：
- 数据转发
- 协议适配
- 服务管理

## 4. API 参考

### AIAssistConfig
配置管理类，用于设置 SDK 运行参数：
```kotlin
class AIAssistConfig private constructor(
    val xAipId: String,        // 应用的id (供应商向AI业务平台申请)
    val xAipUid: String,       // 设备唯一标识(sn,imei,mac)
    val xAipProxy: String,     // 原始请求，包含主机名和端口
    val productId: String,     // 产品 id
    val productKey: String,    // 产品 key
    val deviceNo: String,      // 设备编号
    val deviceNoType: String,  // 设备类型
    val token: String,         // 用户登录验证
    val sn: String            // 用于排查日志，建议使用随机字符串
)
```

### AIAssistantManager
SDK 核心管理类，提供以下功能：
- SDK 初始化
- 设备管理
- 网关服务访问
- ASR 智能对话

## 5. 示例代码

### 基础示例
```kotlin
// 初始化示例
val config = AIAssistConfig.Builder()
    .setXAipId("your_aip_id")
    .setXAipUid("device_mac")
    .build()

AIAssistantManager.initialize(context, config)

// 获取服务示例
val gateway = AIAssistantManager.getInstance().gateWayHelp()
val device = AIAssistantManager.getInstance().deviceHelp()
val asr = AIAssistantManager.getInstance().asrIntelligentDialogueHelp()
```

### 业务流程
```kotlin
// 完整业务流程示例
class AIAssistExample {
    fun initializeSDK(context: Context) {
        // 1. 创建配置
        val config = AIAssistConfig.Builder()
            .setXAipId("your_aip_id")
            .setXAipUid("device_mac")
            .setXAipProxy("host:port")
            .setProductId("product_id")
            .setProductKey("product_key")
            .setDeviceNo("device_no")
            .setDeviceNoType("device_type")
            .setToken("user_token")
            .setSn("random_string")
            .build()

        // 2. 初始化 SDK
        AIAssistantManager.initialize(context, config)

        // 3. 获取服务实例
        val manager = AIAssistantManager.getInstance()
        
        // 4. 使用各项服务
        // 获取网关服务
        val gateway = manager.gateWayHelp()
        // 获取设备服务
        val deviceConnect = manager.deviceHelp()
        // 获取 ADR 智能对话服务
        val asrIntelligentDialogue = manager.asrIntelligentDialogueHelp()
         }

    /**
     * 网关服务使用示例
     * 展示如何使用 GateWay 服务进行网关信息获取和模型调用
     */
    fun gateWayExample() {

        // 1. 获取网关信息
        gateway?.getGateWay(
            onSuccess = { agentServeRes ->
                println("网关信息获取成功: $agentServeRes")
            },
            onError = { error ->
                println("网关信息获取失败: $error")
            }
        )
    }
    
        private val PRODUCT_ID = "1889495584410234882"
        private val PRODUCT_KEY = "riAtcQzVmPLQprAL"
        private val DEVICE_NO = "YM00GCDCK01896"
        private val DEVICE_NO_TYPE = "SN"
        private val URL_DEVICE_INFO =
            "https://device.starrycfn.com:20442/v2/customer/device/secret/info"
        private lateinit var deviceOutputInfo: DeviceOutputInfo

        private val gateWay by lazy {
            GateWay()
        }

        private val deviceConnect by lazy {
            DeviceConnect()
        }

        // 初始化SDK日志系统
        private fun sdkLogInit(context: Context) {
            deviceConnect.sdkLogInit(
                4096,
                context.cacheDir.absolutePath,
                1048576,
                DeviceIdentifier.getAndroidID(context)
            )
        }

        // 设置Edge缓存文件路径
        private fun setEdgeCacheFilePath(context: Context) {
            deviceConnect.setEdgeCacheFilePath(context.cacheDir.absolutePath)
        }

        // 获取设备信息
        private fun getDeviceInfo() {
            var inputInfo = DeviceInputInfo(
                DEVICE_NO_TYPE,
                DEVICE_NO,
                PRODUCT_ID,
                PRODUCT_KEY
            )
            deviceOutputInfo = deviceConnect.getDeviceInfo(URL_DEVICE_INFO, inputInfo)
        }

        // 初始化设备管理器
        private fun initDeviceManager() {

            var ret = deviceConnect.init(PRODUCT_ID,
                deviceOutputInfo.deviceId,
                deviceOutputInfo.deviceSecret,
                "",
                "",
                "", "",
                object : DeviceManagerCallback {
                    override fun messageCallback(
                        provisionType: Int,
                        deviceManagerMsg: DeviceManagerMsg
                    ): String {
                        Timber.tag(TAG).d("DeviceManagerCallback messageCallback")
                        return ""
                    }

                    override fun statusCallback(provisionType: Int, code: Int) {
                        Timber.tag(TAG).d("%s%s", "DeviceManagerCallback statusCallback======", code);
                        // 当此处=8时时此处处理upload
                        if (code == 8) {
                            upload()
                        }
                    }

                    override fun onProvisionDataCallback(data: String) {
                        Timber.tag(TAG).d("DeviceManagerCallback onProvisionDataCallback")
                    }

                    override fun getProvisionDataCallback(): String? {
                        Timber.tag(TAG).d("DeviceManagerCallback getProvisionDataCallback")
                        connectAll()
                        return null
                    }
                })

        }

        // 连接设备
        private fun connectAll() {

            val ips = mutableListOf<String>().apply {
                add("192.169.116.151")
                add("192.169.116.155")
                add("192.169.116.156")
                add("192.169.116.157")
            }

            val uploadInfo = DeviceUploadInfo(
                ips,
                "wireless",
                "2.4G",
                "C875F4776FA1", "3.50302.00.01", "debian 11.7", "IMEI", "CMEI"
            )

            deviceConnect.connectAll(uploadInfo, object : DmActionCallback {
                override fun onCallback(i: Int) {
                    Timber.tag(TAG).d("%s%s", "connect_all code: ", i)
                }
            })

        }

        // 将数据上传到服务器
        private fun upload() {
            var msg = DeviceManagerMsg()
            msg.setUuid("uuid1001");
            msg.setDeviceResource("dr1001");
            msg.setValue("{\"a\":1}");
            msg.setEdgeReportMsg("{\"b\":2}");
            msg.setCloudReportMsg("{\"c\":3}");
            msg.setEdgeDeviceIp("127.0.0.1");
            msg.setMsgType(DeviceManager.MSG_TYPE_SINGLE);
            msg.commandType = DeviceManager.COMMAND_TYPE_GET;
            deviceConnect.upload(DeviceManager.CLOUD, msg, object : DmActionCallback {
                override fun onCallback(i: Int) {
                    Timber.tag(TAG).d("%s%s", "upload code: ", i);
                }
            })
        }

        // 断开所有连接
        private fun disconnectAll() {
            deviceConnect.disconnectAll(object : DmActionCallback {
                override fun onCallback(i: Int) {
                    Timber.tag(TAG).d("%s%s", "disconnect_all code: ", i)
                }
            })
        }

        // 销毁SDK日志资源
        private fun sdkLogDestroy() {
            deviceConnect.sdkLogDestroy()
        }

    /**
     * ADR智能对话服务使用示例
     * 展示如何使用 ASRIntelligentDialogue 服务进行语音识别和对话
     */
    fun asrIntelligentDialogueExample(context: Context) {

        // 1. 设置回调监听
        asrIntelligentDialogue?.setListener(object : ASRIntelligentDialogue.RealtimeAsrListener {
            override fun onMidResult(text: String) {
                // 处理实时识别结果
                println("实时识别结果: $text")
            }

            override fun onFinalResult(text: String) {
                // 处理最终识别结果
                println("最终识别结果: $text")
            }

            override fun onDialogueResult(result: JSONObject) {
                // 处理对话结果
                val assistantAnswer = result.optString("assistant_answer")
                println("AI回复: $assistantAnswer")

                var isEnd = result.optInt("is_end");
                if (isEnd == 1) {
                    println("对话最终结果")
                }

                // 可以解析更多字段
                var directives = result.optJSONArray("data");
                if (directives == null) {
                    return;
                }
                var qid = result.optString("qid");
                for (i in 0 until directives.length()) {
                    var directive = directives.optJSONObject(i);
                    var header = directive.optJSONObject("header");
                    var payload = directive.optJSONObject("payload");
                    var name = header.optString("name");
                    if ("RenderProcessing".equals(name)) {
                        //currentReplyData.isGenerating = true;
                        //currentReplyData.percent = payload.optInt("percent");
                        println("图片渲染进度")
                    }
                    if ("RenderMultiImageCard".equals(name)) {
                        // 图片
                        //currentReplyData.percent = 100;
                        var images = payload.optJSONArray("images");
                        if (null == images || images.length() == 0) {
                            continue;
                        }
                    } else if ("Play".equals(name)) {
                        // 音乐
                    } else if ("RenderStreamCard".equals(name)) {
                        // 流式
                    } else if ("Speak".equals(name)) {
                        // tts
                    }
                }
            }

            override fun onError(code: Int, message: String) {
                // 处理错误
                println("识别错误: code=$code, message=$message")
            }

            override fun onComplete() {
                // 识别完成
                println("识别完成")
            }
        })

        // 2. 开始语音识别
        asr?.startRecognition(context)

        // 3. 在需要停止时调用
        // asr.release()
    }
}
```

### 错误处理
```kotlin
try {
    val config = AIAssistConfig.Builder()
        .setProductId("product_id")
        .setProductKey("product_key")
        .build()

    // 检查配置是否有效
    if (config.isValid()) {
        AIAssistantManager.initialize(context, config)
    } else {
        Log.e("AIAssist", "Invalid configuration")
    }
} catch (e: Exception) {
    Log.e("AIAssist", "Initialization failed", e)
}
```

## 6. 常见问题

### 初始化问题
- Q: SDK 初始化失败怎么办？
- A: 检查配置参数是否完整，确保必需的权限已经授予。

### 权限问题
- Q: 需要哪些权限？
- A: 主要需要 RECORD_AUDIO（语音识别）和 INTERNET（网络通信）权限。

### 连接问题
- Q: 设备连接失败如何处理？
- A: 检查网络状态，确认设备标识是否正确，查看日志中的具体错误信息。
