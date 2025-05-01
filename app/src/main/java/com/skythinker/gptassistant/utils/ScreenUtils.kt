package com.skythinker.gptassistant.utils

import android.content.Context
import android.graphics.Point
import android.view.WindowManager


object ScreenUtils {

    fun getScreenHeight(context: Context): Int {
        val metrics = context.resources.displayMetrics
        return metrics.heightPixels
    }
    
    fun getScreenSizeWithNavigationBar(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        return size
    }
}