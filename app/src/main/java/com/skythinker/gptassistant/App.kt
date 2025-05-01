package com.skythinker.gptassistant

import android.app.Application
import android.content.Intent
import cn.vove7.andro_accessibility_api.AccessibilityApi
import com.skythinker.gptassistant.service.BaseAccessibilityService

import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.AIAssistantManager.Companion.getInstance
import com.cmdc.ai.assist.api.AIFoundationKit
import com.cmdc.ai.assist.constraint.AIAssistConfig
import com.cmdc.ai.assist.constraint.InsideRcChatRequest
import com.skythinker.gptassistant.service.FloatWindowService
import com.skythinker.gptassistant.service.MenuActionFloatService
import com.skythinker.gptassistant.utils.AppStateTracker
import com.skythinker.gptassistant.utils.Utils
import java.util.UUID
import timber.log.Timber

class App : Application() {
    private val TAG = App::class.simpleName.toString()

    companion object {

        lateinit var app: Application

        fun initializeServices(application: Application) {
            app = application
            AccessibilityApi.apply {
                BASE_SERVICE_CLS = BaseAccessibilityService::class.java
                GESTURE_SERVICE_CLS = BaseAccessibilityService::class.java
            }

            AccessibilityApi.init(
                app,
                BaseAccessibilityService::class.java
            )
            Timber.plant(Timber.DebugTree())
        }

    }

    override fun onCreate() {
        super.onCreate()

        initializeServices(this)

        // 创建配置 来酷
        val config = AIAssistConfig.Builder()
            .setProductId("1889495584410234882")
            .setProductKey("riAtcQzVmPLQprAL")
            .setDeviceNo("YM00GCDCK01896")
            .setDeviceNoType("SN")
            .build();

        // 检查配置是否有效
        if (config.isValid()) {
            // 使用配置初始化
            AIAssistantManager.initialize(this, config)
        }

        // 获取 ai 基础服务
        val gateWay = getInstance().gateWayHelp()
        val aiFoundationKit = getInstance().aiFoundationKit() as AIFoundationKit

        gateWay?.obtainDeviceInformation({ response ->
            Timber.tag(TAG).d("%s%s", "response: ", response)
            insideRcChat(aiFoundationKit, response.data!!.deviceId)
        }, { error ->
            Timber.tag(TAG).e("%s%s", "error: ", error)
        })

        AppStateTracker.track(this, object : AppStateTracker.AppStateChangeListener {
            override fun appTurnIntoForeground() {
                app.stopService(Intent(app, FloatWindowService::class.java))
            }

            override fun appTurnIntoBackGround() {
                if (!Utils.isServiceRunning(app, MenuActionFloatService::class.java)) {
                    app.startService(Intent(app, FloatWindowService::class.java))
                }
            }
        })
    }

    /**
     *
     * [Message(role=user, content=讲一个谜语),
     * Message(role=assistant, content=那我来一个简单的：千条线，万条线，掉到水里看不见。猜一自然现象，答案是“雨”哦！),
     *  Message(role=user, content=不知道),
     * Message(role=assistant, content=我无法回答这个问题，请换个问题问我吧。), Message(role=user, content=再来一个)]
     *
     * */
    private fun insideRcChat(aiFoundationKit: AIFoundationKit?, deviceId: String) {
        val messages = listOf(
            InsideRcChatRequest.Message(role = "user", content = "打开蓝牙"),
        )
        aiFoundationKit?.insideRcChat(
            InsideRcChatRequest(
                qid = UUID.randomUUID().toString(),
                third_user_id = UUID.randomUUID().toString(),
                cuid = deviceId,
                messages = messages,
                stream = false,
                dialog_request_id = UUID.randomUUID().toString()
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }
}