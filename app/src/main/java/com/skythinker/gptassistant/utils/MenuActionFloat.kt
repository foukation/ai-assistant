package com.skythinker.gptassistant.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import com.skythinker.gptassistant.config.MenuHandlerCallBack
import com.skythinker.gptassistant.R

@SuppressLint("ViewConstructor")
class MenuActionFloat @JvmOverloads constructor(
    context: Context,
    menuHandlerCallBack: MenuHandlerCallBack,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attributeSet, defStyleAttr){
    init {
        val view = LayoutInflater.from(context).inflate(R.layout.menu_float_action, this)
        val menuCloseTask = view.findViewById<ImageView>(R.id.menu_close_task)
        menuCloseTask?.setOnClickListener {
            menuHandlerCallBack.onClose()
        }
    }
}