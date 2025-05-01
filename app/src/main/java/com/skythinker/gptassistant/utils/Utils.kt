package com.skythinker.gptassistant.utils

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Toast
import com.agent.intention.api.PoiSearchResultContent
import com.skythinker.gptassistant.App
import com.skythinker.gptassistant.MainActivity
import com.skythinker.gptassistant.config.MsgType
import com.skythinker.gptassistant.config.PermissionType
import java.util.UUID


object Utils {
    fun getUUID(): String {
        return UUID.randomUUID().toString()
    }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            App.app.startActivity(intent)
        } catch (_: Exception) {
        }
    }
    
    fun openThirdPartyApp(context: Context, packageName: String, activityClassName: String) {
        val intent = Intent()
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
        intent.setComponent(ComponentName(packageName, activityClassName))
        context.startActivity(intent)
    }

    fun sendTripChatMessage(context: MainActivity, msg: String, type: MsgType, isDelPre: Boolean, flowTotalTime: Int){
        if (Looper.myLooper() != Looper.getMainLooper()) {
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                MainActivity().sendTripChatMessage(context, msg, type, isDelPre, flowTotalTime)
            }
        } else {
            MainActivity().sendTripChatMessage(context, msg, type, isDelPre, flowTotalTime)
        }
    }

    fun sendTripChatMessage(context: MainActivity, msg: String) {
        sendTripChatMessage(context, msg, MsgType.COMMON, true, 0)
    }

    fun sendTripChatMessage(context: MainActivity, msg: String, type: MsgType) {
        sendTripChatMessage(context, msg, type, true, 0)
    }

    fun sendTripChatMessage(context: MainActivity, msg: String, type: MsgType, isDelPre: Boolean) {
        sendTripChatMessage(context, msg, type, isDelPre, 0)
    }

    fun sendTripCard(context: MainActivity,  resultInfo: PoiSearchResultContent, resultUrl: String) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                MainActivity().sendTripCard(context, resultInfo, resultUrl)
            }
        } else {
            MainActivity().sendTripCard(context, resultInfo, resultUrl)
        }
    }

    fun sendAccessibilityCard(context: MainActivity, type: PermissionType) {
        sendAccessibilityCard(context, type, true)
    }

    fun sendAccessibilityCard(context: MainActivity, type: PermissionType, isDelPre: Boolean) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                MainActivity().sendAccessibilityCard(context, type, isDelPre)
            }
        } else {
            MainActivity().sendAccessibilityCard(context, type, isDelPre)
        }
    }

    fun showToastTop(context: MainActivity, text: String) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                val t= Toast.makeText(context, text, Toast.LENGTH_SHORT)
                t.setGravity(Gravity.TOP,0,0)
                t.show()
            }
        } else {
            val t= Toast.makeText(context, text, Toast.LENGTH_SHORT)
            t.setGravity(Gravity.TOP,0,0)
            t.show();
        }
    }

    fun loadImageAsBitmap(context: Context, fileName: String): Bitmap? {
        return try {
            context.assets.open(fileName).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun String.removeLineBreaks(): String {
        return this.filterNot { it.isWhitespace() }
    }


    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val serviceList = activityManager.getRunningServices(Integer.MAX_VALUE)

        for (service in serviceList) {
            if (serviceClass.name.equals(service.service.className)) {
                return true
            }
        }
        return false
    }

    fun subStr(str:String, len:Int):String {
        return if (str.length > len) {
            str.substring(0, len) + "..."
        } else {
            str
        }
    }
}