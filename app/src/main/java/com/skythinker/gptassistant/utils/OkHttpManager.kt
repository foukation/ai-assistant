package com.skythinker.gptassistant.utils

import com.agent.intention.api.OkHttpManager
import com.agent.intention.api.OkHttpManager.clientToken
import com.skythinker.gptassistant.entity.QueryParams
import com.skythinker.gptassistant.entity.QueryParamsForm
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


object OkHttpManager {
    
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
    })
    
    private val sslContext = SSLContext.getInstance("SSL").apply {
        init(null, trustAllCerts, SecureRandom())
    }
    
    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    fun postFile(url: String, params: QueryParamsForm, callback: Callback) {
        val file = File(params.imagePath)
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", file.name, requestBody)
            .addFormDataPart("query", params.query)
            .addFormDataPart("task_type", "click_coordinate")
            .addFormDataPart("stream", "False")
            .build()
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        
        client.newCall(request).enqueue(callback)
    }

    fun post(url: String, params: String, callback: Callback) {
        val headers = Headers.Builder()
            .add("Content-Type", "application/json; charset=utf-8")
            .build()

        val jsonRequestBody = fun(json: String): RequestBody {
            val mediaJson = "application/json; charset=utf-8".toMediaType()
            return json.toRequestBody(mediaJson)
        }

        val request = Request.Builder()
            .url(url)
            .headers(headers)
            .post(jsonRequestBody(params))
            .build()

        client.newCall(request).enqueue(callback)
    }
}
