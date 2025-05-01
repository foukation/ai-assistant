package com.skythinker.gptassistant.helper

import android.app.Application
import com.skythinker.gptassistant.config.MenuHandlerCallBack
import com.skythinker.gptassistant.layout.FloatModelView
import com.skythinker.gptassistant.layout.ModelType
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.skythinker.gptassistant.layout.AgentFloatView
import com.skythinker.gptassistant.utils.MenuActionFloat
import com.skythinker.gptassistant.utils.ScreenUtils

enum class FloatType{
    ACTION_FLOAT,
    MODAL_FLOAT,
    MODAL_TOAST_FLOAT,
    MENU_ACTION_FLOAT,
}
/**
 * 悬浮窗
 */
object FloatComponent {
    fun create(app: Application, floatType: FloatType, description:String?, close:()->Unit = {}, stop:()->Unit = {}, stopText: String = "停止", closeText: String = "关闭") {
        if (floatType == FloatType.MODAL_FLOAT) {
            EasyFloat
                .with(app)
                .setLayout(
                    FloatModelView(app, ModelType.DEFAULT, description, ok = close, cancel = stop, stopText = stopText, closeText = closeText)
                )
                .setShowPattern(ShowPattern.ALL_TIME)
                .setTag(FloatType.MODAL_FLOAT.name)
                .setLocation(0, 0)
                .setDragEnable(false)
                .setMatchParent(widthMatch = true, heightMatch = true)
                .show()
        } else if(floatType == FloatType.MODAL_TOAST_FLOAT) {
            EasyFloat
                .with(app)
                .setLayout(FloatModelView(app, ModelType.TASK_TOAST, description))
                .setShowPattern(ShowPattern.ALL_TIME)
                .setTag(FloatType.MODAL_FLOAT.name)
                .setLocation(0, 0)
                .setDragEnable(false)
                .setMatchParent(widthMatch = true, heightMatch = true)
                .show()
        } else if (floatType == FloatType.ACTION_FLOAT) {
            EasyFloat
                .with(app)
                .setLayout(AgentFloatView(app))
                .setShowPattern(ShowPattern.ALL_TIME)
                .setTag(FloatType.ACTION_FLOAT.name)
                .setAnimator(null)
                .setDragEnable(true)
                .setLocation(0, ScreenUtils.getScreenHeight(app) / 2 + 150)
                .setSidePattern(SidePattern.RESULT_HORIZONTAL)
                .show()
        } else if(floatType == FloatType.MENU_ACTION_FLOAT) {
            EasyFloat
                .with(app)
                .setLayout(MenuActionFloat(app, object: MenuHandlerCallBack {
                    override fun onClose() {
                        close()
                    }
                }))
                .setShowPattern(ShowPattern.ALL_TIME)
                .setTag(FloatType.MENU_ACTION_FLOAT.name)
                .setDragEnable(true)
                .setAnimator(null)
                .setLocation(0, ScreenUtils.getScreenHeight(app) / 2 + 150)
                .show()
        }
    }

    fun hide(floatType: FloatType) {
        EasyFloat.hide(floatType.name)
    }

    fun show(floatType: FloatType) {
        EasyFloat.show(floatType.name)
    }

    fun dismiss(floatType: FloatType) {
        EasyFloat.dismiss(floatType.name)
    }
}