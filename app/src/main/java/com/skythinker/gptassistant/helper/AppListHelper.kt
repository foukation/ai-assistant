package com.skythinker.gptassistant.helper

import com.agent.intention.api.AppData
import com.agent.intention.api.ClientApiAppListRes
import com.agent.intention.api.IntentionApi

object  AppListHelper {
    var appInfoList: ArrayList<AppData>? = null

    fun getAppList() {
        IntentionApi.handlerRequestClientAppList(
            onSuccess = fun (response: ClientApiAppListRes) {
                appInfoList = response.data.list
            },
            onError = fun (_: String) {
            }
        )
    }
}