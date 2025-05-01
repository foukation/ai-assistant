package com.skythinker.gptassistant.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.skythinker.gptassistant.App
import com.skythinker.gptassistant.helper.FloatComponent
import com.skythinker.gptassistant.helper.FloatType

class FloatWindowService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        FloatComponent.create(App.app, FloatType.ACTION_FLOAT, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        FloatComponent.dismiss(FloatType.ACTION_FLOAT)
    }
}