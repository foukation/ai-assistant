package com.skythinker.gptassistant.helper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.skythinker.gptassistant.canstant.MenuHandlerCallBack
import com.skythinker.gptassistant.canstant.TaskHandlerCallback
import com.skythinker.gptassistant.service.FloatViewModelService
import com.skythinker.gptassistant.App
import com.skythinker.gptassistant.layout.ModelType
import com.skythinker.gptassistant.service.FloatWindowService
import com.skythinker.gptassistant.service.MenuActionFloatService

object FloatHelper {
    private class Connection(val description: String = "", val modelType: ModelType = ModelType.DEFAULT, val close: () -> Unit = {}, val stop: () -> Unit = {}, val stopText: String = "停止", val closeText: String = "关闭", var tag:String="")  : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val floatViewModelService = (service as FloatViewModelService.ViewModelBinder).getService()
            floatViewModelService.setTaskHandlerCallback(object : TaskHandlerCallback {
                override fun onClose() {
                    close()
                }
                override fun onStop() {
                    stop()
                }
            }, modelType, description, stopText, closeText, tag)
        }
        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    private var connection: Connection? = null
    private var menuConnection: MenuConnection? = null

    fun modelToast(description: String){
        val intent = Intent(App.app, FloatViewModelService::class.java)
        connection = Connection(description = description, modelType = ModelType.TASK_TOAST)
        App.app.applicationContext.bindService(intent, connection!!, Context.BIND_AUTO_CREATE)
    }

    fun actionToast(description: String, close:()->Unit, stop:()->Unit, stopText: String = "停止", closeText: String = "关闭",) {
        val intent = Intent(App.app, FloatViewModelService::class.java)
        connection = Connection( description, ModelType.DEFAULT, close, stop, stopText, closeText)
        App.app.applicationContext.bindService(intent, connection!!, Context.BIND_AUTO_CREATE)
    }

    fun closeModel() {
        if (connection != null) {
            App.app.applicationContext.unbindService(connection!!)
            connection = null
        }
    }

    private class MenuConnection(val close: () -> Unit = {})  : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val service = (service as MenuActionFloatService.ViewModelBinder).getService()
            service.setTaskHandlerCallback(object : MenuHandlerCallBack {
                override fun onClose() {
                    close()
                }
            })
        }
        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    fun openFloatMenu(close:()->Unit) {
        val intent = Intent(App.app, MenuActionFloatService::class.java)
        menuConnection = MenuConnection(close)
        App.app.stopService(Intent(App.app, FloatWindowService::class.java))
        App.app.applicationContext.bindService(intent, menuConnection!!, Context.BIND_AUTO_CREATE)
    }

    fun closeFloatMenu() {
        if (menuConnection != null) {
            App.app.startService(Intent(App.app, FloatWindowService::class.java))
            App.app.applicationContext.unbindService(menuConnection!!)
            menuConnection = null
        }
    }
}