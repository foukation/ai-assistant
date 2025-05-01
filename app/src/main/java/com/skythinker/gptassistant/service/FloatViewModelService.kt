package com.skythinker.gptassistant.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.skythinker.gptassistant.canstant.TaskHandlerCallback
import com.skythinker.gptassistant.App
import com.skythinker.gptassistant.helper.FloatComponent
import com.skythinker.gptassistant.helper.FloatType
import com.skythinker.gptassistant.layout.ModelType

class FloatViewModelService : Service() {
    fun setTaskHandlerCallback(taskHandlerCallback: TaskHandlerCallback, modelType: ModelType, description: String, stopText: String = "停止", closeText: String = "关闭", tag: String = "") {
        if (modelType == ModelType.DEFAULT) {
            FloatComponent.create(
                App.app, FloatType.MODAL_FLOAT, description,
                { taskHandlerCallback.onClose() }, { taskHandlerCallback.onStop() },stopText, closeText)
        } else if(modelType == ModelType.TASK_TOAST) {
            FloatComponent.create(App.app, FloatType.MODAL_TOAST_FLOAT, description)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return ViewModelBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        FloatComponent.dismiss(FloatType.MODAL_FLOAT)
        FloatComponent.dismiss(FloatType.MODAL_TOAST_FLOAT)
        return super.onUnbind(intent)
    }

    class ViewModelBinder: Binder() {
        fun getService(): FloatViewModelService {
            return FloatViewModelService()
        }
    }
}