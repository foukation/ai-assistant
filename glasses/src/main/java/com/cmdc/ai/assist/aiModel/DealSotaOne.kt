import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.aiModel.AudioFilePcmUtils
import com.cmdc.ai.assist.aiModel.AudioFileUtils
import com.cmdc.ai.assist.http.RequestApi
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import timber.log.Timber
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * DealSotaOne 类 - 处理语音识别的WebSocket通信
 * 负责建立WebSocket连接，发送音频数据，接收识别结果
 *
 * @property wavPath 音频文件路径，当使用文件模式时必需
 * @property confPath 配置文件路径
 * @property microStream 麦克风数据流，当使用麦克风模式时必需
 */
class DealSotaOne {

    companion object {
        private const val TIME_CUT = 160 // 每个音频段的时间间隔(毫秒)
        private const val ADD_OPUS = "off" // 压缩标志
        private const val LOG_LEN = 400 // 日志长度限制

        /*private const val CHUNK_SIZE = 10240 // 音频分段大小(字节)*/
        private const val CHUNK_SIZE = 5120 // 音频分段大小(字节)
        /*private const val CHUNK_SIZE = 1024 // 音频分段大小(字节)*/
    }

    private val audioBuffer = ByteArrayOutputStream()
    private val conf: File
    private val wavPath: String?
    private var microStream: InputStream? = null
    private var isUsingMicrophone: Boolean = false
    private var isStopped = false  // 添加停止标记
    private val TAG = DealSotaOne::class.simpleName.toString()

    /**
     * 文件模式构造函数
     */
    constructor(wavPath: String, conf: File) {
        this.wavPath = wavPath
        this.conf = conf
        this.isUsingMicrophone = false
    }

    /**
     * 麦克风模式构造函数
     */
    constructor(microStream: InputStream, conf: File) {
        this.wavPath = null
        this.conf = conf
        this.microStream = microStream
        this.isUsingMicrophone = true
    }

    /**
     * Context数据类 - 存储会话上下文信息
     *
     * @property directives 指令列表
     * @property ret 返回状态码(0:成功, 非0:失败)
     * @property lock 线程同步锁
     * @property startData 启动配置数据
     * @property timeBegin 开始时间戳
     * @property timeSend 发送完成时间戳
     * @property timeAsr ASR完成时间戳
     * @property timeEnd 结束时间戳
     * @property timeFinal 最终完成时间戳
     * @property asrText ASR识别结果文本
     */
    data class Context(
        val directives: MutableList<Map<String, Any>> = mutableListOf(),
        var ret: Int = 0,
        val lock: ReentrantLock = ReentrantLock(),
        var startData: JSONObject? = null,
        val timeBegin: Long = System.currentTimeMillis(),
        var timeSend: Long = 0,
        var timeAsr: Long = 0,
        var timeEnd: Long = 0,
        var timeFinal: Long = 0,
        var asrText: String = ""
    )

    // 类级别的上下文对象，用于跨方法共享状态
    private var ctx: Context? = null

    // 添加回调接口
    interface DealSotaOneListener {
        fun onMidResult(text: String)  // ASR中间结果
        fun onFinalResult(text: String)  // ASR最终结果
        fun onDialogueResult(result: JSONObject)  // 直接传递data对象用于analyzeDirectives
        fun onError(code: Int, message: String)
        fun onComplete()
    }

    private var listener: DealSotaOneListener? = null

    fun setListener(listener: DealSotaOneListener) {
        this.listener = listener
    }

    /**
     * 读取WAV文件内容
     *
     * @param wavFile WAV文件路径
     * @return 音频字节数组
     */
    private fun readWav(wavFile: String): ByteArray {
        return File(wavFile).readBytes()
    }

    /**
     * 将字节数组分割成固定大小的块
     * 用于将音频数据分段发送
     *
     * @param data 原始字节数组
     * @param chunkSize 块大小
     * @return 分割后的字节数组列表
     */
    internal fun chunkList(data: ByteArray, chunkSize: Int): List<ByteArray> {
        return data.asSequence()
            .chunked(chunkSize)
            .map { it.toByteArray() }
            .toList()
    }

    /**
     * 生成指定长度的随机字符串
     * 用于生成唯一标识符
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    private fun generateRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9') + '-'
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    /**
     * 生成带前缀的自定义字符串
     *
     * @param prefix 前缀
     * @param totalLength 总长度
     * @return 生成的字符串
     */
    internal fun generateCustomString(prefix: String, totalLength: Int): String {
        val suffixLength = totalLength - prefix.length
        return if (suffixLength > 0) {
            prefix + generateRandomString(suffixLength)
        } else {
            "Error: Total length should be at least the length of the prefix."
        }
    }

    /**
     * 发送音频数据
     * 包括发送start信号、音频数据和finish信号
     *
     * @param webSocket WebSocket连接对象
     * @param ctx 上下文对象
     * @param wavSegs 音频数据分段列表
     */
    private fun sendAudioData(webSocket: WebSocket?, ctx: Context, wavSegs: List<ByteArray>) {
        if (webSocket == null) {
            Timber.tag(TAG).d("[sendAudioData] Error: WebSocket is null")
            ctx.ret = 1
            return
        }

        try {
            if (ctx.ret != 0) {
                Timber.tag(TAG).d("[sendAudioData] Error: Context ret is not 0")
                return
            }

            // 发送start信号
            val startSignal = createStartSignal(ctx)

            Timber.tag(TAG)
                .d("[sendAudioData] ${System.currentTimeMillis() / 1000.0} Sending start signal:")
            Timber.tag(TAG).d("[sendAudioData] Start signal content: ${startSignal.toString(4)}")

            if (!webSocket.send(startSignal.toString())) {
                Timber.tag(TAG).d("[sendAudioData] Failed to send start signal")
                ctx.ret = 1
                return
            }
            Timber.tag(TAG).d("[sendAudioData] Start signal sent successfully")

            if (ctx.ret != 0) {
                Timber.tag(TAG)
                    .d("[sendAudioData] Error: Context ret became non-zero after sending start signal")
                return
            }

            // 分段发送音频数据
            /*val sleepTime = TIME_CUT.toLong() * 2*/
            val sleepTime = TIME_CUT.toLong()

            if (isUsingMicrophone) {
                Timber.tag(TAG).d("[sendAudioData] Starting microphone mode data transmission")
                // 麦克风模式：实时读取并发送数据
                val buffer = ByteArray(CHUNK_SIZE)
                var shouldContinue = true
                /*while (!isStopped && ctx.ret == 0 && ctx.asrText.isEmpty() && shouldContinue) {*/
                while (!isStopped && ctx.ret == 0) {
                    microStream?.let { stream ->
                        val bytesRead = stream.read(buffer)
                        if (!isStopped && bytesRead > 0) {
                            Timber.tag(TAG)
                                .d("[sendAudioData] Sending microphone data chunk: $bytesRead bytes")
                            if (!webSocket.send(buffer.toByteString(0, bytesRead))) {
                                Timber.tag(TAG)
                                    .d("[sendAudioData] Failed to send microphone data chunk")
                                ctx.ret = 1
                                shouldContinue = false
                            } else {
                                audioBuffer.write(buffer, 0, bytesRead)
                                Timber.tag(TAG)
                                    .d("[sendAudioData] Microphone data chunk sent successfully")
                                /*Thread.sleep(sleepTime)*/
                            }
                        }
                    }
                }
            } else {
                Timber.tag(TAG)
                    .d("[sendAudioData] Starting file mode data transmission")
                // 文件模式：发送预先准备的数据段
                for ((index, chunk) in wavSegs.withIndex()) {
                    if (ctx.ret != 0 || ctx.asrText.isNotEmpty()) {
                        Timber.tag(TAG)
                            .d("[sendAudioData] Stopping file transmission: ${if (ctx.ret != 0) "error occurred" else "ASR text received"}")
                        break
                    }
                    Timber.tag(TAG)
                        .d("[sendAudioData] Sending file chunk ${index + 1}/${wavSegs.size}: ${chunk.size} bytes")
                    Thread.sleep(sleepTime)
                    if (!webSocket.send(chunk.toByteString(0, chunk.size))) {
                        Timber.tag(TAG)
                            .d("[sendAudioData] Failed to send file chunk ${index + 1}")
                        ctx.ret = 1
                        break
                    }
                    audioBuffer.write(chunk)
                    Timber.tag(TAG)
                        .d("[sendAudioData] File chunk ${index + 1} sent successfully")
                }
            }

            // 发送结束信号
            if (ctx.ret == 0) {
                Timber.tag(TAG)
                    .d("[sendAudioData] ${System.currentTimeMillis() / 1000.0} Sending finish signal")
                val endSignal = JSONObject().apply {
                    put("type", "finish")
                }
                if (webSocket.send(endSignal.toString())) {
                    Timber.tag(TAG)
                        .d("[sendAudioData] Finish signal sent successfully")
                    val audioData = audioBuffer.toByteArray()
                    val wavFilePath = AudioFileUtils.saveToWavFile(audioData)
                    val pcmFilePath = AudioFilePcmUtils.saveToPcmFile(audioData)
                    AIAssistantManager.getInstance().aiAssistConfig.currentPcmFilePath =
                        pcmFilePath.toString()
                    Timber.tag(TAG).d("[sendAudioData] Audio saved to: $wavFilePath")
                    Timber.tag(TAG).d("[sendAudioData] Audio saved to: $pcmFilePath")
                } else {
                    Timber.tag(TAG)
                        .d("[sendAudioData] Failed to send finish signal")
                }
            }

            ctx.lock.withLock {
                ctx.timeSend = System.currentTimeMillis()
                Timber.tag(TAG)
                    .d("[sendAudioData] Updated send time: ${ctx.timeSend}")
            }
        } catch (e: Exception) {
            Timber.tag(TAG)
                .e("[sendAudioData] Error occurred: ${e.message}")
            ctx.ret = 1
        } finally {
            ctx.timeFinal = System.currentTimeMillis()
            Timber.tag(TAG)
                .d("[sendAudioData] Final time recorded: ${ctx.timeFinal}")
            if (isUsingMicrophone) {
                Timber.tag(TAG)
                    .d("[sendAudioData] Closing microphone resources")
                closeResources()
            }
            audioBuffer.reset()
        }
    }

    /**
     * 主要处理函数，执行完整的语音识别流程
     * 使用协程处理IO操作
     */
    suspend fun dealOne(uri: String) {
        synchronized(connectionLock) {
            if (isConnecting) {
                Timber.tag(TAG)
                    .d("Connection already in progress")
                return Unit
            }
            isConnecting = true
        }

        audioBuffer.reset()

        try {
            // 在IO线程读取配置文件
            val conf = conf.readText()

            ctx = Context(startData = JSONObject(conf))
            val currentCtx = ctx

            val sn = "luyongshuai_from_sota"
            val formattedUri = String.format(uri, sn)

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .pingInterval(5, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(formattedUri)
                .addHeader("User-Agent", "didp/ version/")
                .addHeader("Connection", "Upgrade")
                .addHeader("Upgrade", "websocket")
                .addHeader("Sec-WebSocket-Version", "13")
                .addHeader("X-Device-ID", "23a700000005cb")
                .addHeader("X-Client-IP", "112.224.190.141")
                /*.addHeader("X-PROXY-PASS", "wss://duer-kids.baidu.com/sandbox/sota/realtime_asr?sn=%s")*/
                /*.addHeader("X-PROXY-PASS", "wss://duer-kids.baidu.com")
                .addHeader("Token", "MGJjZTdiNWEyNjJjNDAyM2E5MWNhZTU1ZjY3YTBjMjE=")*/
                .build()

            suspendCancellableCoroutine<Unit> { continuation ->
                val connectionLatch = CountDownLatch(1)
                var ws: WebSocket? = null

                val listener = object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        synchronized(connectionLock) {
                            wsConnection = webSocket
                            connectionLatch.countDown()
                            Timber.tag(TAG)
                                .d("${System.currentTimeMillis() / 1000.0} WebSocket connected successfully")
                            continuation.resume(Unit)
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        try {
                            Timber.tag(TAG)
                                .d("[WebSocket.onMessage] Response: $text")
                            val json = JSONObject(text)
                            when (json.optString("type")) {
                                "mid_result" -> {
                                    val result = json.optString("result") ?: ""
                                    Timber.tag(TAG)
                                        .d("[WebSocket.onMessage.mid_result] $result")
                                    listener?.onMidResult(result)
                                }

                                "fin_result" -> {
                                    val result = json.optString("result") ?: ""
                                    Timber.tag(TAG)
                                        .d("[WebSocket.onMessage.fin_result] $result")
                                    val currentCtx = ctx
                                    currentCtx?.asrText = result
                                    listener?.onFinalResult(result)
                                }

                                "inside_rc" -> {
                                    val data = json.optJSONObject("data")
                                    if (data != null) {
                                        Timber.tag(TAG)
                                            .d("[WebSocket.onMessage.inside_rc] Data received")
                                        listener?.onDialogueResult(data)  // 直接传递整个data对象
                                    }
                                }

                                "ready", "inside_rc" -> {
                                    Timber.tag(TAG)
                                        .d("[WebSocket.onMessage.${json.optString("type")}] Received")
                                }

                                "dcs_decide" -> {
                                    if (json.optInt("end", 0) == 1) {
                                        Timber.tag(TAG)
                                            .d("[WebSocket.onMessage.dcs_decide] End flag received")
                                        Timber.tag(TAG)
                                            .d("[WebSocket.onMessage.dcs_decide] Stop receiving")
                                    }
                                }

                                else -> {
                                    // 处理directive消息
                                    if (text.contains("directive")) {
                                        Timber.tag(TAG)
                                            .d("[WebSocket.onMessage.directive] Directive received:")
                                        Timber.tag(TAG)
                                            .d("[WebSocket.onMessage.directive] Content: $text")
                                        val directive = json.optJSONObject("directive")
                                        if (directive != null) {
                                            val header = directive.optJSONObject("header")
                                            if (header != null) {
                                                val namespace = header.optString("namespace")
                                                val name = header.optString("name")
                                                val directiveName = "$namespace.$name"
                                                Timber.tag(TAG)
                                                    .d("[WebSocket.onMessage.directive] Namespace: $namespace, Name: $name")

                                                val payload = directive.optJSONObject("payload")
                                                val payloadMap = if (payload != null) {
                                                    mutableMapOf<String, Any>().apply {
                                                        payload.keys().forEach { key ->
                                                            put(key, payload.get(key))
                                                        }
                                                    }
                                                } else {
                                                    emptyMap()
                                                }
                                                Timber.tag(TAG)
                                                    .d("[WebSocket.onMessage.directive] Payload: $payloadMap")

                                                directives.add(directiveName to payloadMap)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Timber.tag(TAG)
                                .e("[WebSocket.onMessage] Error parsing message: ${e.message}")
                            listener?.onError(-1, "Error parsing message: ${e.message}")
                            cleanupResources()
                            if (continuation.isActive) {
                                continuation.resumeWithException(e)
                            }
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        // 不输出二进制消息日志
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        Timber.tag(TAG)
                            .d("[WebSocket.onClosing] code: $code, reason: $reason")
                        webSocket.close(code, reason)
                        cleanupResources()
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        Timber.tag(TAG)
                            .d("[WebSocket.onClosed] code: $code, reason: $reason")
                        onClosed()
                        listener?.onComplete()
                        cleanupResources()
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?
                    ) {
                        Timber.tag(TAG)
                            .e("[WebSocket.onFailure] Connection failed: ${t.message}")
                        listener?.onError(1, t.message ?: "Connection failed")
                        currentCtx?.ret = 1
                        connectionLatch.countDown()
                        cleanupResources()
                        if (continuation.isActive) {
                            continuation.resumeWithException(t)
                        }
                        onClosed()
                    }
                }

                ws = client.newWebSocket(request, listener)

                // 连接成功发送数据
                // sendAudioData
                if (connectionLatch.await(5, TimeUnit.SECONDS)) {
                    sendAudioData(wsConnection, ctx!!, emptyList())
                }

                continuation.invokeOnCancellation {
                    ws.cancel()
                    cleanupResources()
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG)
                .e("[dealOne] Error: ${e.message}")
            cleanupResources()
            currentCtx?.ret = 1
            throw e
        } finally {
            synchronized(connectionLock) {
                isConnecting = false
            }
        }
    }

    private fun cleanupResources() {
        synchronized(connectionLock) {
            try {
                wsConnection?.cancel()
                wsConnection = null
                currentCtx?.ret = 1
                audioBuffer.reset()
            } catch (e: Exception) {
                Timber.tag(TAG)
                    .e("[cleanupResources] Error during cleanup: ${e.message}")
            }
        }
    }

    /**
     * 获取ASR识别结果
     *
     * @return 识别的文本结果，如果没有结果返回空字符串
     */
    fun getAsrResult(): String {
        return ctx?.asrText ?: ""
    }

    // 存储收到的directive消息
    private val directives = mutableListOf<Pair<String, Map<String, Any>>>()

    /**
     * WebSocket连接关闭时的处理
     * 输出统计信息和结果
     */
    private fun onClosed() {
        Timber.tag(TAG)
            .d("${System.currentTimeMillis() / 1000.0} all done")
        Timber.tag(TAG)
            .d("fail?: ${ctx?.ret ?: 1}")
        Timber.tag(TAG)
            .d("asr_result: ${ctx?.asrText ?: ""}")

        // 计算并输出时间统计
        ctx?.let { ctx ->
            val sendTime = (ctx.timeSend - ctx.timeBegin) / 1000.0
            val asrTime = (ctx.timeAsr - ctx.timeBegin) / 1000.0
            val finalTime = (ctx.timeFinal - ctx.timeBegin) / 1000.0
            Timber.tag(TAG)
                .d("send: %.2f, asr: %.2f, final: %.2f".format(sendTime, asrTime, finalTime))
        }

        // 输出收集的directives信息
        if (directives.isNotEmpty()) {
            Timber.tag(TAG)
                .d("directive:")
            directives.forEach { (name, payload) ->
                Timber.tag(TAG)
                    .d("\t$name")
                if (payload.isNotEmpty()) {
                    Timber.tag(TAG)
                        .d("\t\t$payload")
                }
            }
        }
    }

    /**
     * 准备音频数据
     * 根据不同模式选择数据源
     *
     * @return 音频数据分段列表
     */
    private fun prepareAudioData(): List<ByteArray> {
        return if (isUsingMicrophone) {
            // 麦克风模式下返回空列表，因为数据会在sendAudioData中实时读取
            listOf()
        } else {
            // 文件模式：读取文件并分段
            wavPath?.let { path ->
                val data = if (!path.contains(".opus")) {
                    readWav(path)
                } else {
                    File(path).readBytes()
                }
                chunkList(data, CHUNK_SIZE)
            } ?: listOf()
        }
    }

    /**
     * 关闭资源
     */
    private fun closeResources() {
        if (isUsingMicrophone) {
            microStream?.close()
            microStream = null
        }
    }

    /**
     * 创建开始信号
     *
     * @param ctx 上下文对象
     * @return 开始信号的JSON对象
     */
    private fun createStartSignal(ctx: Context): JSONObject {
        // 创建client_context数组，包含设备状态信息
        val clientContext = JSONArray().apply {
            // 1. Voice Output State
            put(JSONObject().apply {
                put("payload", JSONObject().apply {
                    put("playerActivity", "FINISHED")
                    put("offsetInMilliseconds", 0)
                    put("token", "")
                })
                put("header", JSONObject().apply {
                    put("namespace", "ai.dueros.device_interface.voice_output")
                    put("name", "SpeechState")
                })
            })

            // 2. Speaker Controller State
            put(JSONObject().apply {
                put("payload", JSONObject().apply {
                    put("volume", 25)
                    put("muted", false)
                })
                put("header", JSONObject().apply {
                    put("namespace", "ai.dueros.device_interface.speaker_controller")
                    put("name", "Volume")
                })
            })

            // 3. Audio Player State
            put(JSONObject().apply {
                put("payload", JSONObject().apply {
                    put("playerActivity", "FINISHED")
                    put("offsetInMilliseconds", 0)
                    put("token", "")
                })
                put("header", JSONObject().apply {
                    put("namespace", "ai.dueros.device_interface.audio_player")
                    put("name", "PlaybackState")
                })
            })
        }

        // 构建开始信号
        return JSONObject().apply {
            put("type", "start")
            put("data", JSONObject(ctx.startData.toString()).apply {
                put("client_context", clientContext)
            })
        }
    }

    private var wsConnection: WebSocket? = null
    private var currentCtx: Context? = null
    private val connectionLock = Object()
    private var isConnecting = false

    /**
     * 停止语音识别
     * 关闭WebSocket连接并清理资源
     */
    fun stopRecognition() {
        isStopped = true  // 先设置停止标记
        cleanupResources()
        audioBuffer.reset()
    }

}