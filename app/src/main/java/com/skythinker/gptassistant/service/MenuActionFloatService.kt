package com.skythinker.gptassistant.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.skythinker.gptassistant.canstant.MenuHandlerCallBack
import com.skythinker.gptassistant.App
import com.skythinker.gptassistant.helper.FloatComponent
import com.skythinker.gptassistant.helper.FloatType

class MenuActionFloatService : Service() {
    fun setTaskHandlerCallback(taskHandlerCallback: MenuHandlerCallBack) {
        FloatComponent.create(App.app, FloatType.MENU_ACTION_FLOAT, null, close = { taskHandlerCallback.onClose() })
    }

    override fun onBind(intent: Intent?): IBinder? {
        return ViewModelBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        FloatComponent.dismiss(FloatType.MENU_ACTION_FLOAT)
        return super.onUnbind(intent)
    }

    class ViewModelBinder: Binder() {
        fun getService():MenuActionFloatService{
            return MenuActionFloatService()
        }
    }
}