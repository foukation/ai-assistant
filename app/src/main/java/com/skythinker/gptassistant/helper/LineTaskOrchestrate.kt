package com.skythinker.gptassistant.helper

import android.os.Build
import androidx.annotation.RequiresApi
import cn.vove7.andro_accessibility_api.AccessibilityApi.Companion.isBaseServiceEnable
import com.agent.intention.api.Action
import com.skythinker.gptassistant.MainActivity
import com.skythinker.gptassistant.utils.Utils.showToastTop
import java.lang.Thread.sleep

/**
 *
 */
class LineTaskOrchestrate(
    private val context: MainActivity,
    private val actions: ArrayList<Action>,
    private val slots: Map<String,String>,
    private val execNum: Int,
) {
    private lateinit var taskHelper: LineTaskSocketHelper

    private fun openMenu() {
        FloatHelper.openFloatMenu(close = { close() })
    }

    private fun closeMenu() {
        FloatHelper.closeFloatMenu()
    }
    
    @RequiresApi(Build.VERSION_CODES.R)
    private fun taskRun(){
        openMenu()
        taskHelper.run()
        sleep(2000L)
        showToastTop(context,"点击悬浮球可关闭任务")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun start() {
        taskHelper = LineTaskSocketHelper(
            context,
            actions,
            slots,
            execNum,
            closeMenu = { closeMenu() },
        )

        if (isBaseServiceEnable) {
            taskRun()
        }
    }


    fun stop() {
        taskHelper.stop()
    }

    fun close() {
        taskHelper.close()
    }
}
