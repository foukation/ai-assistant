package com.cmdc.ai.assist.http

object ApiConfig {

    /*const val GATEWAY_BASE_URL = "https://z5f3vhk2.cxzfdm.com:30101"*/

    /*const val TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL =
        "https://62b98tux.cxzfdm.com:30101"*/

    const val TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL =
        "https://ivs.chinamobiledevice.com:11443"
    const val BASE_URL = "https://aqua-digital.aipaas.com"

    const val WSS_WEBSOCKET_ASR_BASE_URL = "wss://ivs.chinamobiledevice.com:11443/app-ws/v1/asr"

    const val TIMEOUT: Long = 15000L

    var useAgent: Boolean = false
    var agentBaseUrl = ""
    var apiToken = ""
    var auth_token = ""

    const val GATEWAY_API = "/apgp/pl"
    const val AVAILABLE_MODELS_API = "/v1/models"
    const val MODEL_CHAT_API = "/llm/v1/chat/completions"
    const val OBTAIN_DEVICE_INFORMATION_API = "/v2/customer/device/secret/info"
    const val DEVICE_DATA_REPORT_API = "/v2/customer/device/report"

}