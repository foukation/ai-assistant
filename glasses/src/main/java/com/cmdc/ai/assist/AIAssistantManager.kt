package com.cmdc.ai.assist

import android.app.Application
import android.content.Context
import com.cmdc.ai.assist.api.ASRIntelligentDialogue
import com.cmdc.ai.assist.api.DeviceConnect
import com.cmdc.ai.assist.api.GateWay
import com.cmdc.ai.assist.constraint.AIAssistConfig
import com.cmdc.ai.assist.constraint.DeviceReportRequest
import com.github.gzuliyujiang.oaid.DeviceIdentifier
import com.starryos.deviceconnect.DeviceInputInfo
import com.starryos.deviceconnect.DeviceManager
import com.starryos.deviceconnect.DeviceManagerCallback
import com.starryos.deviceconnect.DeviceManagerMsg
import com.starryos.deviceconnect.DeviceOutputInfo
import com.starryos.deviceconnect.DeviceUploadInfo
import com.starryos.deviceconnect.DmActionCallback
import timber.log.Timber
import timber.log.Timber.DebugTree
import timber.log.Timber.plant
import kotlin.String

/**
 * AI助手管理器类
 * 该类用于管理AI助手的相关操作和状态
 * 注意：该类不允许外部直接实例化，以确保其作为单例模式实现
 */
class AIAssistantManager private constructor(private val config: AIAssistConfig) {

    /**
     * AIAssistantManager 初始化和获取实例入口
     * */
    companion object {

        @Volatile
        private var instance: AIAssistantManager? = null

        /**
         * 初始化AI助手的功能
         *
         * 此函数用于设置AI助手的初始配置和上下文环境，使其能够根据提供的配置信息
         * 进行正确的操作和响应
         *
         * @param context 应用程序的上下文，用于访问应用程序资源和数据库等
         * @param config AI助手的配置信息，包括但不限于语言设置、识别模型等
         */
        fun initialize(context: Context, config: AIAssistConfig) {
            instance ?: synchronized(this) {
                instance ?: AIAssistantManager(config).also {
                    DeviceIdentifier.register(context as Application?)
                    if (BuildConfig.DEBUG) {
                        plant(DebugTree())
                    }
                    instance = it
                    /*it.sdkLogInit(context)
                    it.setEdgeCacheFilePath(context)*/
                    it.initWithConfig()
                    /*it.getGateWay()*/
                }
            }
        }

        /**
         * 获取当前对象的实例
         *
         * 此函数用于实现单例模式的延迟初始化它检查实例是否已经存在，
         * 如果不存在，则抛出IllegalStateException异常，表明实例尚未初始化
         * 这种方法确保了在实例未被初始化时，不会返回null，避免了空指针异常的发生
         *
         * @return 当前对象的实例如果实例未初始化，则抛出IllegalStateException
         */
        fun getInstance() = instance ?: throw IllegalStateException("Not initialized")
    }

    private val TAG = AIAssistantManager::class.simpleName.toString()
    private val PRODUCT_ID = "1889495584410234882"
    private val PRODUCT_KEY = "riAtcQzVmPLQprAL"
    private val DEVICE_NO = "YM00GCDCK01896"
    private val DEVICE_NO_TYPE = "SN"
    private val URL_DEVICE_INFO =
        "https://device.starrycfn.com:20442/v2/customer/device/secret/info"
    private lateinit var deviceOutputInfo: DeviceOutputInfo
    internal lateinit var aiAssistConfig: AIAssistConfig

    private val gateWay by lazy {
        GateWay()
    }

    private val deviceConnect by lazy {
        DeviceConnect()
    }

    // 新增：使用配置初始化各个组件
    private fun initWithConfig() {

        aiAssistConfig = config

        // 初始化设备信息
        /*getDeviceInfo()*/
        // 初始化设备管理器
        /*initDeviceManager()*/
    }

    /**
     * 提供对GateWay对象的辅助功能或信息
     *
     * 此函数可能涉及与GateWay相关的复杂操作或查询，但由于函数体不包含具体实现，
     * 具体功能细节和实现逻辑无法直接从函数签名中得知
     *
     * @return 可能返回一个GateWay对象，也可能返回null，具体取决于实现逻辑和运行时情况
     */
    fun gateWayHelp(): GateWay? {
        return gateWay
    }

    /**
     * 获取设备纳管实例
     *
     * 此函数旨在提供一种方式来获取与设备的连接帮助可能返回一个DeviceConnect对象如果成功建立连接，
     * 否则返回null设计此函数的目的是为了处理可能的连接问题，并为调用者提供一个机会来解决这些问题
     *
     * @return DeviceConnect? 可能返回一个DeviceConnect对象，表示成功建立的帮助连接，或null表示无法建立连接
     */
    fun deviceHelp(): DeviceConnect? {
        return deviceConnect
    }

    /**
     * 获取 ASR 智能对话助手
     *
     * 此方法用于初始化或获取一个ASR（自动语音识别）智能对话助手的实例该助手能够帮助处理与语音识别相关的智能对话任务，如语音命令识别、对话管理等
     *
     * @return ASRIntelligentDialogue? 可能返回一个ASR智能对话助手的实例，也可能返回null如果助手不可用或尚未实现
     */
    fun asrIntelligentDialogueHelp(): ASRIntelligentDialogue? {
        return ASRIntelligentDialogue()
    }

    internal fun getGateWay() {

        /*val agentServeReq = AgentServeReq(
            "",
            "",
            "",
            "",
            "",
            "",
            ""
        )*/

        gateWay.getGateWay({ response ->
            Timber.tag(TAG).d("%s%s", "response: ", response)
        }, { error ->
            Timber.tag(TAG).e("%s%s", "error: ", error)
        })

        gateWay.obtainDeviceInformation({ response ->
            Timber.tag(TAG).d("%s%s", "response: ", response)
            dataReport()
        }, { error ->
            Timber.tag(TAG).e("%s%s", "error: ", error)
        })

    }

    private fun dataReport() {
        gateWay.dataReport(
            DeviceReportRequest(
                deviceId = aiAssistConfig.deviceId,
                deviceSecret = aiAssistConfig.deviceSecret,
                productId = aiAssistConfig.productId,
                productKey = aiAssistConfig.productKey,
                params = emptyMap()
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
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

    private fun release() {
        disconnectAll()
        sdkLogDestroy()
    }

}
