package com.skythinker.gptassistant.service

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import cn.vove7.andro_accessibility_api.AccessibilityApi
import cn.vove7.auto.core.AppScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

class BaseAccessibilityService : AccessibilityApi() {
    override val enableListenPageUpdate: Boolean
        get() = true

    override fun onCreate() {
        super.onCreate()
        gestureService = this
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: BaseAccessibilityService? = null
        fun acquire(): BaseAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun takeScreenshotSec(result:(bitmap: Bitmap? ) -> Unit) {
        instance?.takeScreenshot(
            Display.DEFAULT_DISPLAY, Dispatchers.Main.asExecutor(), object : TakeScreenshotCallback {
                override fun onSuccess(p0: ScreenshotResult) {
                    try {
                        p0.hardwareBuffer.use { buffer ->
                            val bitmap = Bitmap.wrapHardwareBuffer(buffer, p0.colorSpace)
                            result(bitmap)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(p0: Int) {
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        gestureService = null
    }

    override fun onPageUpdate(currentScope: AppScope) {
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

}

