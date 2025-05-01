package com.cmdc.ai.assist.samples

import android.content.Context
import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.api.ASRIntelligentDialogue
import com.cmdc.ai.assist.constraint.AIAssistConfig
import com.starryos.deviceconnect.DeviceInputInfo
import com.starryos.deviceconnect.DeviceManagerCallback
import com.starryos.deviceconnect.DeviceManagerMsg
import com.starryos.deviceconnect.DeviceUploadInfo
import com.starryos.deviceconnect.DmActionCallback
import org.json.JSONArray
import org.json.JSONObject

/**
 * AIAssistConfig 使用示例
 */
class AIAssistConfigSamples {

    /**
     * 创建基础配置示例
     */
    fun createBasicConfig() {
        val config = AIAssistConfig.Builder()
            .setXAipId("your_aip_id")          // 设置应用ID
            .setXAipUid("device_mac")          // 设置设备唯一标识
            .setXAipProxy("host:port")         // 设置代理
            .build()
    }

    /**
     * 创建完整配置示例
     */
    fun createFullConfig(context: Context) {
        // 1. 创建完整配置
        val config = AIAssistConfig.Builder()
            .setXAipId("your_aip_id")          // 设置应用ID
            .setXAipUid("device_mac")          // 设置设备唯一标识
            .setXAipProxy("host:port")         // 设置代理
            .setProductId("product_id")        // 设置产品ID
            .setProductKey("product_key")      // 设置产品密钥
            .setDeviceNo("device_no")          // 设置设备编号
            .setDeviceNoType("device_type")    // 设置设备类型
            .setToken("user_token")            // 设置用户令牌
            .setSn("random_string")            // 设置序列号
            .build()

        // 2. 初始化 SDK
        AIAssistantManager.initialize(context, config)
    }

    /**
     * 网关服务使用示例
     * 展示如何使用 GateWay 服务进行网关信息获取和模型调用
     */
    fun gateWayExample() {
        val manager = AIAssistantManager.getInstance()
        val gateway = manager.gateWayHelp()

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

    /**
     * 设备服务使用示例
     * 展示如何使用 DeviceConnect 服务进行设备连接和管理
     */
    fun deviceExample() {
        val manager = AIAssistantManager.getInstance()
        val device = manager.deviceHelp()

        // 1. 初始化设备日志
        device?.sdkLogInit(
            bufSize = 1024,
            filePath = "/storage/emulated/0/Android/data/your.package.name/files/logs",
            maxFileSize = 1024 * 1024,  // 1MB
            devUniqueId = "device_unique_id"
        )

        // 2. 初始化设备连接
        val initResult = device?.init(
            productId = "your_product_id",
            deviceId = "your_device_id",
            token = "your_token",
            cloudCaPath = "path/to/ca.crt",
            cloudCertificatePath = "path/to/cert.crt",
            cloudKeyPath = "path/to/key.pem",
            cloudKeyPw = "key_password",
            listener = object : DeviceManagerCallback {

                override fun messageCallback(
                    p0: Int,
                    p1: DeviceManagerMsg?
                ): String? {
                    TODO("Not yet implemented")
                }

                override fun statusCallback(p0: Int, p1: Int) {
                    TODO("Not yet implemented")
                }

                override fun onProvisionDataCallback(p0: String?) {
                    TODO("Not yet implemented")
                }

                override fun getProvisionDataCallback(): String? {
                    TODO("Not yet implemented")
                }
            }
        )

        if (initResult == 0) {
            // 3. 连接所有设备
            device.connectAll(
                uploadInfo = DeviceUploadInfo().apply {
                    // 设置上传信息
                },
                callback = object : DmActionCallback {

                    override fun onCallback(p0: Int) {
                        TODO("Not yet implemented")
                    }
                }
            )
        }
    }

    /**
     * ADR智能对话服务使用示例
     * 展示如何使用 ASRIntelligentDialogue 服务进行语音识别和对话
     */
    fun asrIntelligentDialogueExample(context: Context) {
        val manager = AIAssistantManager.getInstance()
        val asr = manager.asrIntelligentDialogueHelp()

        // 1. 设置回调监听
        asr?.setListener(object : ASRIntelligentDialogue.RealtimeAsrListener {
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
