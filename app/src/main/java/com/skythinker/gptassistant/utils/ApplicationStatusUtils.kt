package com.skythinker.gptassistant.utils

import android.content.pm.PackageManager
import com.skythinker.gptassistant.App

object ApplicationStatusUtils {

    fun appIsInsert(pageName: String):Boolean {
        val packageManager: PackageManager = App.app.packageManager
        return try {
            packageManager.getPackageInfo(pageName, 0)
            true
        } catch (e : PackageManager.NameNotFoundException) {
            false
        }
    }
}