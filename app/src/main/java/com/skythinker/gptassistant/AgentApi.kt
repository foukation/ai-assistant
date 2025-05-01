package com.skythinker.gptassistant

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.skythinker.gptassistant.config.ApiUrl
import com.skythinker.gptassistant.entity.IsOcrResult
import com.skythinker.gptassistant.entity.OcrResult
import com.skythinker.gptassistant.entity.QueryParams
import com.skythinker.gptassistant.utils.OkHttpManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

object AgentApi {
    private val LOG_KEY = AgentApi.javaClass.simpleName
    private val gson: Gson = GsonBuilder().create()
    
    fun handlerOCR(params: QueryParams, onSuccess: (OcrResult) -> Unit, onError: (errMsg: String) -> Unit) {
        OkHttpManager.post(ApiUrl.OCR_URL, gson.toJson(params), object: Callback {
            override fun onResponse(call: Call, response: Response) {
                val respString = response.body?.string()
                println("respString=$respString")
                try {
                    val resp = gson.fromJson(respString , OcrResult::class.java)
                    onSuccess(resp)
                } catch (e: Exception) {
                    onError("响应数据解析错误，请检查数据格式")
                    Timber.tag(LOG_KEY).e(e.message.toString())
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                onError("无法连接到服务器，请检查网络")
                e.message?.let { Timber.tag(LOG_KEY).e(it) }
            }
        })
    }

    fun isAdditionOcr(params: QueryParams, onSuccess: (IsOcrResult) -> Unit, onError: (errMsg: String) -> Unit) {
        OkHttpManager.post(ApiUrl.OCR_URL, gson.toJson(params), object: Callback {
            override fun onResponse(call: Call, response: Response) {
                val respString = response.body?.string()
                println("respString=$respString")
                try {
                    val resp = gson.fromJson(respString , IsOcrResult::class.java)
                    onSuccess(resp)
                } catch (e: Exception) {
                    onError("响应数据解析错误，请检查数据格式")
                    Timber.tag(LOG_KEY).e(e.message.toString())
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                onError("无法连接到服务器，请检查网络")
                e.message?.let { Timber.tag(LOG_KEY).e(it) }
            }
        })
    }
}