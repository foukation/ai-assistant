package com.skythinker.gptassistant.helper

import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import cn.vove7.andro_accessibility_api.AccessibilityApi.Companion.isBaseServiceEnable
import com.agent.intention.api.ActionsContent
import com.agent.intention.api.ClientApiActionsResMul
import com.agent.intention.api.ConditionsContent
import com.agent.intention.api.IntentionApi
import com.agent.intention.api.IntentionApi.gson
import com.agent.intention.api.RespResultStr
import com.ai.multimodal.business.MultimodalAssistant
import com.skythinker.gptassistant.MainActivity
import com.skythinker.gptassistant.config.PermissionType
import com.skythinker.gptassistant.utils.ApplicationStatusUtils
import com.skythinker.gptassistant.utils.Utils.sendAccessibilityCard
import com.skythinker.gptassistant.utils.Utils.sendTripChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object HandlerLineTaskHelper {
    private var requestIsRuntime: Boolean = false
    lateinit var sessionID: String

    fun start(context: MainActivity, resultString: String) {

        if (requestIsRuntime) {
            sendTripChatMessage(context, "请勿频繁操作")
            return
        }

        requestIsRuntime = true
        IntentionApi.handlerRequestClientActionsMul(
            resultString,
            sessionID,
            onSuccess = @RequiresApi(Build.VERSION_CODES.R)
            fun (response: ClientApiActionsResMul) {
                requestIsRuntime = false
                CoroutineScope(Dispatchers.Main).launch {
                    val resp = response.data
                    if (resp.mulIntent.Status == RespResultStr.ING.alias) {
                        sendTripChatMessage(context, resp.mulIntent.ResponseText)
                        sessionID = resp.mulIntent.SessionID
                    }

                    if (resp.mulIntent.Status == RespResultStr.DOWN.alias && resp.actionList == null) {
                        val multimodalAssistant = MultimodalAssistant()
                        if (resp.mulIntent.RecognizedJSON.intents.size > 0 ) {
                            multimodalAssistant.conversationIntent(
                                context,
                                resultString,
                                resp.mulIntent.RecognizedJSON.intents[0].intent
                            )
                        } else {
                            sendTripChatMessage(context, "抱歉，当前指令暂不支持")
                        }
                        return@launch
                    }

                    if (resp.mulIntent.Status == RespResultStr.DOWN.alias) {
                        if (resp.actionList == null) {
                            sendTripChatMessage(context, "报错信息如下：获取action失败")
                            return@launch
                        }

                        if (!Settings.canDrawOverlays(context)) {
                            sendAccessibilityCard(context, PermissionType.FLOAT)
                        }

                        if (!isBaseServiceEnable) {
                            sendAccessibilityCard(context, PermissionType.ACCESSIBILITY, Settings.canDrawOverlays(context))
                            return@launch
                        }

                        sessionID = ""

                        /*
                        * 槽位值处理
                        * */
                        val intents = resp.intents
                        val totalSlots = mutableMapOf<String, String>()
                        intents?.forEach { element ->
                            element.slots.let { it ->
                                it.product?.let { totalSlots["product"] = it }          // 商品
                                it.recipient?.let { totalSlots["recipient"] = it }      // 联系人
                                it.receiver?.let { totalSlots["receiver"] = it }        // 接收人
                                it.location?.let { totalSlots["location"] = it }        // 地区
                                it.destination?.let { totalSlots["destination"] = it }  // 目的地地区
                                it.content?.let { totalSlots["content"] = it }          // 消息内容
                                it.store?.let { totalSlots["store"] = it }              // 商店
                                it.times?.let { totalSlots["times"] = it }              // 执行次数
                                it.name?.let { totalSlots["name"] = it }
                                it.direction?.let { totalSlots["direction"] = it }
                                it.device?.let { totalSlots["device"] = it }
                                it.`object`?.let { totalSlots["object"] = it }
                                it.application?.let { totalSlots["application"] = it }
                            }
                        }

                        /*
                        * AES解密
                        * */

                        val actionString = AESHelper.decrypt(resp.actionList!!)
                        val result = gson.fromJson(
                            "{\"actions\":${actionString}}",
                            ActionsContent::class.java
                        )
                        val actions = result.actions
                        if (actions.size < 2) {
                            sendTripChatMessage(context, "至少需要两条动作，请先完善数据")
                            return@launch
                        }

                        /*
                        * 验证关联app是否安装
                        * */
                        val appNotAvailable: Boolean
                        val apps = resp.appId.split(',')
                        val appInfoList = AppListHelper.appInfoList
                        var curAppName = ""

                        appNotAvailable = apps.any { app ->
                            val appInfo = appInfoList?.find { ele ->
                                ele.id == app.toInt()
                            }
                            curAppName = appInfo?.name.toString()
                            appInfo?.let { ApplicationStatusUtils.appIsInsert(it.packageName) } == false
                        }

                        if (appNotAvailable) {
                            sendTripChatMessage(
                                context,
                                "要执行的程序（$curAppName）还未安装，请到应用商店下载安装！"
                            )
                            return@launch
                        }

                        /*
                        * 数据初始化
                        * */
                        actions.withIndex().forEach { action ->
                            val index = action.index
                            val value = action.value
                            val length = actions.size - 1
                            when (index) {
                                0 -> {
                                    value.isFirst = true
                                    value.nextExecuteId = actions[1].actionId
                                }

                                length -> {
                                    value.isLast = true
                                    value.preExecuteId = actions[length - 1].actionId
                                }

                                else -> {
                                    value.preExecuteId = actions[index - 1].actionId
                                    value.nextExecuteId = actions[index + 1].actionId
                                }
                            }

                            if (value.matchConditions != "") {
                                val json = gson.fromJson(
                                    "{\"conditions\":${value.matchConditions}}",
                                    ConditionsContent::class.java
                                )
                                value.matchConditionsFormat = json.conditions
                            }
                        }

                        val execNum = resp.execNum

                        LineTaskOrchestrate(
                            context,
                            actions,
                            totalSlots,
                            if (execNum == 0) 1 else execNum,
                        ).start()
                    }
                }
            },
            onError = fun(errMsg: String) {
                sendTripChatMessage(context, errMsg)
                sessionID = ""
                requestIsRuntime = false
            }
        )
    }
}
