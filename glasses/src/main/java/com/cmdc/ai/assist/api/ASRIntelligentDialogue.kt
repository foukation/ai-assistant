package com.cmdc.ai.assist.api

import DealSotaOne
import android.content.Context
import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.aiModel.MicroRecordStream
import com.cmdc.ai.assist.http.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 实时麦克风语音识别处理类
 * 负责处理实时语音输入的识别流程
 */
class ASRIntelligentDialogue internal constructor() {

    companion object {

        private const val CONFIG_FILE_NAME = "hht_ctx4.conf"

        /*private const val WS_URL = "wss://duer-kids.baidu.com/sandbox/sota/realtime_asr?sn=%s"*/

        /*private const val WS_URL = "wss://36.133.228.58:36981/app-ws/v1/asr?sign=7f9e4b2a1d8c5f3e6m9n7k4l2p8q5r3t&sn=%s"*/
        /*private const val WS_URL =
            "wss://c124df1z.cxzfdm.com:36981/app-ws/v1/asr?sign=7f9e4b2a1d8c5f3e6m9n7k4l2p8q5r3t&sn=%s"*/
        /*private const val WS_URL =
            "wss://z5f3vhk2.cxzfdm.com:30101/apgp/ws/sandbox/sota/realtime_asr?sn=xxxxx"*/
        private var WS_URL = ""

    }

    @Volatile
    private var isReleased = false

    @Volatile
    private var isRecognizing = false
    private val stateLock = Object()
    private var currentDealSotaOne: DealSotaOne? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val TAG = ASRIntelligentDialogue::class.simpleName.toString()

    /**
     * 实时语音识别监听器接口
     * 该接口定义了实时语音识别过程中不同阶段的回调方法，包括中间结果、最终结果、对话结果、错误处理和完成通知
     */
    interface RealtimeAsrListener {
        /**
         * 语音识别中间结果回调
         *
         * @param text 识别到的文本信息
         */
        fun onMidResult(text: String) {}

        /**
         * 语音识别最终结果回调
         *
         * @param text 最终识别的文本信息
         */
        fun onFinalResult(text: String) {}

        /**
         * 对话结果回调
         *
         * @param result 包含对话相关信息的JSON对象
         */
        fun onDialogueResult(result: JSONObject) {}

        /**
         * 错误回调
         *
         * @param code 错误代码
         * @param message 错误信息
         */
        fun onError(code: Int, message: String) {}

        /**
         * 完成回调
         * 表示 ASR 智能对话过程已完成
         */
        fun onComplete() {}
    }

    private var listener: RealtimeAsrListener? = null

    /**
     * 设置实时语音识别监听器
     *
     * 该方法用于将一个RealtimeAsrListener接口的实现类对象注册为监听器，以便在语音识别过程中接收实时的识别结果
     * 通过此监听器，开发者可以获取到语音识别的中间结果和最终结果，进而进行相应的业务处理
     *
     * @param listener 实现了RealtimeAsrListener接口的监听器对象，用于接收语音识别事件
     */
    fun setListener(listener: RealtimeAsrListener) {
        this.listener = listener
    }

    /**
     * 启动语音识别功能
     *
     * 此函数通过协程在IO调度器上启动语音识别任务，以便不阻塞主线程
     * 它尝试使用麦克风输入进行语音识别如果识别过程中遇到错误，
     * 将通过监听器回调错误信息，并记录错误日志
     *
     * @param context 上下文，用于访问应用环境信息
     */
    fun startRecognition(context: Context) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                startMicrophoneRecognition(context, MicroRecordStream.getInstance())
            } catch (e: Exception) {
                listener?.onError(-1, e.message ?: "Unknown error")
                Timber.tag(TAG).e(("错误: code=" + -1 + ", message=" + e.message))
            }
        }
    }

    /**
     * 开始麦克风录音识别
     */
    private suspend fun startMicrophoneRecognition(
        context: Context,
        microStream: InputStream
    ): String =
        withContext(Dispatchers.IO) {

            synchronized(stateLock) {
                if (isRecognizing) {
                    Timber.tag(TAG).e("Recognition already in progress")
                    return@withContext "Recognition already in progress"
                }
                if (isReleased) {
                    Timber.tag(TAG).e("Instance has been released")
                    return@withContext "Instance has been released"
                }
                isRecognizing = true
            }

            try {
                return@withContext suspendCancellableCoroutine { continuation ->

                    //  hht_ctx4.conf 这个文件在Android assets 目录下
                    val configFile = File(context.cacheDir, CONFIG_FILE_NAME).apply {
                        context.assets.open("hht_ctx4.conf").use { input ->
                            outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    val dealSotaOne = DealSotaOne(
                        microStream,
                        configFile
                    )
                    currentDealSotaOne = dealSotaOne

                    dealSotaOne.setListener(object : DealSotaOne.DealSotaOneListener {
                        override fun onMidResult(text: String) {
                            if (!isReleased) {
                                listener?.onMidResult(text)
                                Timber.tag(TAG).d("%s%s", "实时识别结果: ", text)
                            }
                        }

                        override fun onFinalResult(text: String) {
                            if (!isReleased) {
                                listener?.onFinalResult(text)
                                Timber.tag(TAG).d("%s%s", "最终识别结果: ", text)
                            }
                        }

                        override fun onDialogueResult(result: JSONObject) {
                            if (!isReleased) {
                                listener?.onDialogueResult(result)
                            }
                        }

                        override fun onError(code: Int, message: String) {
                            if (!isReleased) {
                                Timber.tag(TAG).e("错误: code=$code, message=$message")
                                listener?.onError(code, message)
                               // continuation.resumeWithException(Exception(message))
                            }
                        }

                        override fun onComplete() {
                            if (!isReleased) {
                                listener?.onComplete()
                                Timber.tag(TAG).d("识别完成")
                            }
                            synchronized(stateLock) {
                                isRecognizing = false
                            }
                            continuation.resume(dealSotaOne.getAsrResult())
                        }
                    })

                    // Launch a coroutine to call dealOne
                    launch(Dispatchers.IO) {
                        try {

                            WS_URL =
                                "wss://z5f3vhk2.cxzfdm.com:36981/app-ws/v1/asr?sign=${AIAssistantManager.getInstance().aiAssistConfig.token}&sn=${AIAssistantManager.getInstance().aiAssistConfig.sn}"

                            // 地址：wss://ivs.chinamobiledevice.com:11443/app-ws/v1/asr?sn=xxxx-xxxx-xxx&deviceNo=xxxxxx&productKey=xxxx&productId=xxxx&ts=1740017297000&sign=xxxxx

                            var sn = UUID.randomUUID().toString()
                            var deviceNo = AIAssistantManager.getInstance().aiAssistConfig.deviceNo
                            var productKey =
                                AIAssistantManager.getInstance().aiAssistConfig.productKey
                            var productId =
                                AIAssistantManager.getInstance().aiAssistConfig.productId
                            val timestamp = System.currentTimeMillis()
                            var sign = "xxxxxx"
                            var parameter =
                                "?sn=$sn&deviceNo=$deviceNo&productKey=$productKey&productId=$productId&ts=$timestamp&sign=$sign"

                            AIAssistantManager.getInstance().aiAssistConfig.sn = sn

                            WS_URL = ApiConfig.WSS_WEBSOCKET_ASR_BASE_URL + parameter
                            dealSotaOne.dealOne(WS_URL)
                        } catch (e: Exception) {
                            Timber.tag(TAG)
                                .e(("错误: code=" + -1 + ", message=" + "Recognition failed: ${e.message}"))
                            listener?.onError(-1, "Recognition failed: ${e.message}")
                            continuation.resumeWithException(e)
                        }
                    }

                    continuation.invokeOnCancellation {
                        dealSotaOne.stopRecognition()
                    }
                }
            } finally {
                synchronized(stateLock) {
                    isRecognizing = false
                }
            }

        }

    /**
     * 释放所有资源并停止语音识别
     * 在不再需要语音识别时调用此方法
     */
    fun release() {

        synchronized(stateLock) {
            if (isReleased) {
                return
            }
            isReleased = true
        }

        try {
            currentDealSotaOne?.let { dealSota ->
                try {
                    // 停止语音识别
                    dealSota.stopRecognition()
                } catch (e: Exception) {
                    Timber.tag(TAG).e("Error stopping recognition: ${e.message}")
                } finally {
                    currentDealSotaOne = null
                }
            }
            MicroRecordStream.getInstance().close()
        } catch (e: Exception) {
            Timber.tag(TAG).e("Error during release: ${e.message}")
        }

        coroutineScope.cancel()

    }
} 