package com.skythinker.gptassistant.helper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import cn.vove7.auto.core.api.click
import cn.vove7.auto.core.api.editor
import cn.vove7.auto.core.api.scrollDown
import cn.vove7.auto.core.viewfinder.ConditionGroup
import cn.vove7.auto.core.viewfinder.SF
import cn.vove7.auto.core.viewfinder.containsDesc
import cn.vove7.auto.core.viewfinder.containsText
import cn.vove7.auto.core.viewfinder.desc
import cn.vove7.auto.core.viewfinder.id
import cn.vove7.auto.core.viewfinder.text
import cn.vove7.auto.core.viewfinder.type
import cn.vove7.auto.core.viewnode.ViewNode
import com.agent.intention.api.Action
import com.agent.intention.api.ClientTimeData
import com.agent.intention.api.IntentionApi
import com.skythinker.gptassistant.AgentApi
import com.skythinker.gptassistant.App
import com.skythinker.gptassistant.MainActivity
import com.skythinker.gptassistant.config.MsgType
import com.skythinker.gptassistant.entity.ExecWay
import com.skythinker.gptassistant.entity.ImgContent
import com.skythinker.gptassistant.entity.IsOcrResult
import com.skythinker.gptassistant.entity.MatchMethod
import com.skythinker.gptassistant.entity.MatchType
import com.skythinker.gptassistant.entity.OcrResult
import com.skythinker.gptassistant.entity.QueryParams
import com.skythinker.gptassistant.entity.SlotType
import com.skythinker.gptassistant.entity.State
import com.skythinker.gptassistant.entity.Type
import com.skythinker.gptassistant.service.BaseAccessibilityService
import com.skythinker.gptassistant.utils.ScreenUtils
import com.skythinker.gptassistant.utils.TaskExecUtils
import com.skythinker.gptassistant.utils.TaskPool
import com.skythinker.gptassistant.utils.Utils.openThirdPartyApp
import com.skythinker.gptassistant.utils.Utils.sendTripChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Thread.sleep
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Timer
import java.util.TimerTask

class LineTaskSocketHelper(
    private val context: MainActivity,
    private val tasks: ArrayList<Action>,
    private val slots: Map<String, String>,
    private val excNum: Int = 1,
    private val closeMenu: () -> Unit,
){
    private var curExcId: String? = null
    private var curExcSucNum: Int = 0
    private var excAvailable: Boolean = true

    private fun resetTask (){
        tasks.forEach { ele->
            ele.state = State.INIT.alias
            ele.curExuNum = 0
            ele.watchResult = null
        }
    }

    private fun getFirstAction (): String? {
        val selector = tasks.find { ele-> ele.isFirst == true }
        return selector?.actionId
    }

    private fun getTaskById (id: String): Action? {
        val selector = tasks.find { ele-> ele.actionId == id }
        return selector
    }

    private fun updateCurExcId(id: String) {
        curExcId = id
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun executeInTimer(curTaskItem: Action, timer: Timer, nexTimer: Timer? = null) {
        val num = curTaskItem.curExuNum
        val maxNum = curTaskItem.maxExcNum

        curTaskItem.curExuNum = num + 1

        if (curTaskItem.curExuNum > maxNum) {

            curTaskItem.state = State.WATCH_OUT_TIME.alias
            sendTripChatMessage(context, "匹配超时，已自动结束任务")

            timer.cancel()
            close()

        } else {
            actionWatch(curTaskItem)
            if (curTaskItem.state == State.WATCH_SUC.alias) {
                actionExecute(curTaskItem, timer)
                nexTimer?.cancel()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun analyzeCoordinatesWithOcr (content: Context, query: String, type:String, curTaskItem: Action) {
        if(!excAvailable) {
            return
        }

        TaskPool.CACHE.execute {
            BaseAccessibilityService().takeScreenshotSec { result->
                val file = File(
                    App.app.getExternalFilesDir(null)!!.parent, "curScreen.png"
                )
                if (!file.exists()) {
                    file.createNewFile()
                }
                val fos = FileOutputStream(file)
                val bos = BufferedOutputStream(fos)
                if (result != null) {
                    result.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                    bos.close()
                    result.recycle()

                    sleep(200L)
                    val imagePath = "${content.getExternalFilesDir(null)?.getParent()}/curScreen.png"
                    TaskPool.MAIN.post {
                        val screenSize =  ScreenUtils.getScreenSizeWithNavigationBar(content)
                        val imageBase64 = TaskExecUtils.getBase64FromLocalImage(imagePath)
                        val params = QueryParams(
                            type,
                            false,
                            ImgContent("png", screenSize.x, screenSize.y, imageBase64),
                            query
                        )
                        AgentApi.handlerOCR(
                            params,
                            onSuccess= fun(resp: OcrResult){
                                if(excAvailable) {
                                    if(resp.state == "success") {
                                        runBlocking {
                                            val x = resp.content[0]
                                            val y = resp.content[1]
                                            val resultStr = "${(screenSize.x * x).toInt()},${(screenSize.y * y).toInt()}"
                                            println("screenSize--${screenSize.x}==${screenSize.y}")
                                            println("resultText==$query++++resultStr==$resultStr")

                                            val xyFormat = resultStr.split(',')
                                            click(xyFormat[0].toInt(), xyFormat[1].toInt())
                                            curTaskItem.state = State.EXEC_SUC.alias
                                            postpose(curTaskItem)
                                        }
                                    } else {
                                        curTaskItem.state = State.EXEC_ERR.alias
                                        sendTripChatMessage(context, "屏幕理解失败，已自动结束任务")
                                        close()
                                    }
                                }
                            },
                            onError = fun(errMsg: String) {
                                curTaskItem.state = State.EXEC_ERR.alias
                                sendTripChatMessage(context, errMsg)
                                close()
                            }
                        )
                    }
                }
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun isOcrAdditionHandler (content: Context, query: String, curTaskItem: Action, call:(isClick:Boolean)->Unit) {

        if(!excAvailable) {
            return
        }
        TaskPool.CACHE.execute {
            BaseAccessibilityService().takeScreenshotSec { result->
                val file = File(
                    App.app.getExternalFilesDir(null)!!.parent, "curScreen.png"
                )
                if (!file.exists()) {
                    file.createNewFile()
                }
                val fos = FileOutputStream(file)
                val bos = BufferedOutputStream(fos)
                if (result != null) {
                    result.compress(Bitmap.CompressFormat.JPEG, 80, bos)
                    bos.close()
                    result.recycle()

                    sleep(200L)
                    val imagePath = "${content.getExternalFilesDir(null)?.getParent()}/curScreen.png"
                    TaskPool.MAIN.post {
                        val screenSize =  ScreenUtils.getScreenSizeWithNavigationBar(content)
                        val imageBase64 = TaskExecUtils.getBase64FromLocalImage(imagePath)
                        val params = QueryParams(
                            "click_coordinate_addition",
                            false,
                            ImgContent("jpeg", screenSize.x, screenSize.y, imageBase64),
                            query
                        )
                        AgentApi.isAdditionOcr(
                            params,
                            onSuccess= fun(resp: IsOcrResult){
                                if(excAvailable) {
                                    if(resp.state == "success") {
                                        if (resp.content.state == 1) {
                                            call(true)
                                        } else {
                                            call(false)
                                        }
                                    } else {
                                        curTaskItem.state = State.EXEC_ERR.alias
                                        sendTripChatMessage(context, "屏幕理解失败，已自动结束任务")
                                        close()
                                    }
                                }
                            },
                            onError = fun(errMsg: String) {
                                curTaskItem.state = State.EXEC_ERR.alias
                                sendTripChatMessage(context, errMsg)
                                close()
                            }
                        )
                    }
                }
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun actionWatch(curTaskItem: Action) {
        when (curTaskItem.type) {

            Type.COMMON.alias,
            Type.TAB.alias,
            Type.TOAST.alias -> {
                val conditions = curTaskItem.matchConditionsFormat
                var findItem: ConditionGroup? = null

                fun updateFindItem(updateFn: () -> ConditionGroup) {
                    findItem = if (findItem == null) {
                        updateFn()
                    } else {
                        val updatedItem = updateFn()
                        when (curTaskItem.matchNodeType) {
                            MatchType.OR.alias -> findItem!!.or(updatedItem)
                            MatchType.AND.alias -> findItem!!.and(updatedItem)
                            else -> updatedItem
                        }
                    }
                }

                conditions?.forEach { ele ->
                    when (ele.way) {
                        MatchMethod.ID.alias -> updateFindItem { SF.id(ele.target) }
                        MatchMethod.TEXT.alias -> updateFindItem { SF.text(ele.target) }
                        MatchMethod.DESC.alias -> updateFindItem { SF.desc(ele.target) }
                        MatchMethod.TYPE.alias -> updateFindItem { SF.type(ele.target) }
                        MatchMethod.CONTAIN_TEXT.alias -> updateFindItem { SF.containsText(ele.target) }
                        MatchMethod.CONTAIN_DESC.alias -> updateFindItem { SF.containsDesc(ele.target) }
                        MatchMethod.EDITOR.alias -> updateFindItem { editor() }
                    }
                }

                runBlocking {
                    val result = findItem?.findFirst(
                        maxDepth = curTaskItem.matchNodeMaxDepth,
                        needActiveNode = curTaskItem.curExuNum % 2 == 1 ,
                        includeInvisible = curTaskItem.matchNodeWithActive == 1,
                    )
                    println("===#-${curTaskItem.actionId}-${result}")
                    if(result == null) {
                        curTaskItem.state = State.WATCH_ING.alias
                    } else {
                        curTaskItem.state = State.WATCH_SUC.alias
                        curTaskItem.watchResult = result
                    }
                }
            }

            Type.OCR.alias -> TODO()
            Type.APP.alias -> TODO()
            Type.DEEP_LINK.alias-> TODO()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun actionExecute(curTaskItem: Action, timer: Timer? = null) {
        val result = curTaskItem.watchResult
        val delay = curTaskItem.execDelayTimer
        if(delay > 0) {
            sleep(delay.toLong())
        }

        when (curTaskItem.type) {

            Type.COMMON.alias,
            Type.TAB.alias,
            Type.TOAST.alias -> {
                when (curTaskItem.execWay) {

                    ExecWay.TRY_CLICK.alias -> {
                        if(result is ViewNode) {
                            result.tryClick()
                        }
                    }

                    ExecWay.CLICK_BY_XY.alias -> {
                        val xy = curTaskItem.matchOcrTarget
                        if(xy.isNotEmpty()){
                            val xyFormat = xy.split(',')
                            runBlocking {
                                click(xyFormat[0].toInt(), xyFormat[1].toInt())
                            }
                        }
                    }

                    ExecWay.CLICK_BY_NODE.alias -> {
                        if(result is ViewNode) {
                            TaskExecUtils.clickByNodeRect(result)
                        }
                    }

                    ExecWay.CLICK_BY_NODE_TWICE.alias -> {
                        if(result is ViewNode) {
                            TaskExecUtils.clickByNodeRect(result, isTwice = true)
                        }
                    }

                    ExecWay.CLICK_BY_NODE_LONG.alias -> {
                        if(result is ViewNode) {
                            TaskExecUtils.clickByNodeRect(result, isLong = true)
                        }
                    }

                    ExecWay.INPUT.alias -> {
                        runBlocking {
                            val defaultValue = curTaskItem.slotDefaultValue
                            val slotsList = curTaskItem.slotKeyList.split(',')
                            val value = slotsList.find { slot -> slots[slot] != null }
                            val resultText = value?.let { slots[it] } ?: defaultValue

                            if(result is ViewNode) {
                                result.apply {
                                    text = resultText
                                }
                            }
                        }
                    }

                    ExecWay.SWIPER.alias -> {
                        println("执行滑动")
                        runBlocking {
                            sleep(curTaskItem.execDelayTimer.toLong())
                            scrollDown()
                        }
                    }
                }
            }

            Type.APP.alias -> {
                val appInfoList = AppListHelper.appInfoList
                if(curTaskItem.appId != null && appInfoList != null) {
                    val appInfo= appInfoList.find { ele->
                        ele.id == curTaskItem.appId
                    }
                    if (appInfo != null) {
                        openThirdPartyApp(context, appInfo.packageName, appInfo.homeActivity)
                    }
                } else {
                    sendTripChatMessage(context, "获取适配App列表失败")
                    close()
                }
            }

            Type.DEEP_LINK.alias -> TODO()
        }

        sleep(100L)
        curTaskItem.state = State.EXEC_SUC.alias
        postpose(curTaskItem)
        timer?.cancel()
    }

    // Todo 后续优化
    @RequiresApi(Build.VERSION_CODES.R)
    private fun timerAction() {
        val curTaskItem = curExcId?.let { getTaskById(it) }
        if (excAvailable && curTaskItem != null) {
            val preTask = curTaskItem.preExecuteId?.let { getTaskById(it) }
            if (curTaskItem.isFirst == true || preTask?.state == State.EXEC_SUC.alias || preTask?.type == Type.TOAST.alias) {
                sendTripChatMessage(context, curTaskItem.actionDesc)
                when (curTaskItem.type) {

                    Type.APP.alias -> {
                        actionExecute(curTaskItem)
                    }

                    Type.COMMON.alias -> {
                        if(curTaskItem.execWay == ExecWay.CLICK_BY_XY.alias || curTaskItem.execWay == ExecWay.SWIPER.alias) {
                            actionExecute(curTaskItem)
                        } else {
                            val timer = Timer()
                            val taskObj = object : TimerTask() {
                                @RequiresApi(Build.VERSION_CODES.R)
                                override fun run() {
                                    if(excAvailable) {
                                        executeInTimer(curTaskItem, timer)
                                    } else {
                                        timer.cancel()
                                    }
                                }
                            }
                            timer.schedule(taskObj, curTaskItem.firstExecTimer.toLong(), curTaskItem.interval.toLong())
                        }
                    }
                    
                    Type.OCR.alias -> {
                        ocrAction(curTaskItem)
                    }

                    Type.TAB.alias -> {
                        val nextTaskItem = curTaskItem.nextExecuteId?.let { getTaskById(it) }
                        if(nextTaskItem != null && nextTaskItem.type == Type.COMMON.alias ) {
                            val curTimer = Timer()
                            val nextTimer = Timer()

                            val curTaskObj = object : TimerTask() {
                                @RequiresApi(Build.VERSION_CODES.R)
                                override fun run() {
                                    if (excAvailable) {
                                        executeInTimer(curTaskItem, curTimer, nextTimer)
                                    } else {
                                        curTimer.cancel()
                                    }
                                }
                            }

                            val nextTaskObj = object : TimerTask() {
                                @RequiresApi(Build.VERSION_CODES.R)
                                override fun run() {
                                    if (excAvailable) {
                                        executeInTimer(nextTaskItem, nextTimer, curTimer)
                                    } else {
                                        nextTimer.cancel()
                                    }
                                }
                            }

                            curTimer.schedule(curTaskObj, curTaskItem.firstExecTimer.toLong(), curTaskItem.interval.toLong())
                            nextTimer.schedule(nextTaskObj, nextTaskItem.firstExecTimer.toLong(), nextTaskItem.interval.toLong())
                        }
                    }

                    Type.TOAST.alias -> {
                        val timer = Timer()
                        val taskObj = object : TimerTask() {
                            @RequiresApi(Build.VERSION_CODES.R)
                            override fun run() {
                                if (excAvailable) {
                                    val num = curTaskItem.curExuNum
                                    val maxNum = curTaskItem.maxExcNum

                                    curTaskItem.curExuNum = num + 1

                                    if (curTaskItem.curExuNum > maxNum) {
                                        timer.cancel()
                                    } else {
                                        actionWatch(curTaskItem)

                                        if (curTaskItem.state == State.WATCH_SUC.alias) {
                                            actionExecute(curTaskItem, timer)
                                        }
                                    }
                                } else {
                                    timer.cancel()
                                }
                            }
                        }

                        timer.schedule(taskObj, curTaskItem.firstExecTimer.toLong(), curTaskItem.interval.toLong())
                        curTaskItem.nextExecuteId?.let { updateCurExcId(it) }
                        timerAction()
                    }

                    Type.DEEP_LINK.alias -> {

                        val url = curTaskItem.deeplinkUrl
                        val args = curTaskItem.deeplinkUrlArgs

                        val defaultValue = curTaskItem.slotDefaultValue
                        val slotsList = curTaskItem.slotKeyList.split(',')
                        val value = slotsList.find { slot -> slots[slot] != null }
                        val resultText = value?.let { slots[it] } ?: defaultValue

                        val fullUrl = "${url}?${args}${resultText}"

                        runBlocking {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.setData(Uri.parse(fullUrl))
                                context.startActivity(intent)
                            } catch (e: Exception){
                                println(e)
                            }
                        }

                        curTaskItem.state = State.EXEC_SUC.alias
                        postpose(curTaskItem)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun ocrAction(curTaskItem: Action) {
        sleep(curTaskItem.execDelayTimer.toLong())
        curTaskItem.state = State.WATCH_SUC.alias
        val query = curTaskItem.matchOcrTarget
        val slotsList = curTaskItem.slotKeyList.split(',')
        val value = slotsList.find { slot -> slots[slot] != null }
        val slotType = curTaskItem.slotType
        var resultText = value?.let { slots[it] } ?: query

        if (curTaskItem.isOcrDate == 1 || slotType == SlotType.DATE.alias) {
            IntentionApi.handlerRequestClientNormalizeTime(
                resultText,
                onSuccess = @RequiresApi(Build.VERSION_CODES.R)
                fun (response: ClientTimeData) {
                    CoroutineScope(Dispatchers.Main).launch {
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        val timeDate = LocalDate.parse(response.standard_time, formatter)
                        if (timeDate != null) {
                            resultText = "${timeDate.monthValue}月${timeDate.dayOfMonth}号-农历${response.chinese_time}"
                            analyzeCoordinatesWithOcr(context, resultText, "click_time", curTaskItem)
                        } else {
                            sendTripChatMessage(context, "时间泛化失败")
                            close()
                        }
                    }
                },
                onError = fun (errMsg: String) {
                    sendTripChatMessage(context, errMsg)
                    close()
                }
            )
        }
        else if(curTaskItem.isOcrAddition == 1) {
            isOcrAdditionHandler(context, resultText, curTaskItem) {
                if(it) {
                    curTaskItem.state = State.EXEC_SUC.alias
                    postpose(curTaskItem)
                } else {
                    analyzeCoordinatesWithOcr(context, resultText, "click_coordinate", curTaskItem)
                }
            }
        }
        else {
            analyzeCoordinatesWithOcr(context, resultText, "click_coordinate", curTaskItem)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun postpose(curTaskItem: Action) {
        val selectorLast = tasks.find { ele -> ele.isLast == true }
        if (selectorLast?.state == State.EXEC_SUC.alias) {
            curExcSucNum ++
            if (curExcSucNum == excNum) {
                close()
                val sucStr = if(excNum == 1) "指令执行完成" else  "指令执行完成，共执行${excNum}次"
                sendTripChatMessage(context, sucStr)
            } else {
                run()
            }
        } else {
            curTaskItem.nextExecuteId?.let { updateCurExcId(it) }
            timerAction()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun run() {
        getFirstAction()?.let { updateCurExcId(it) }
        resetTask()
        timerAction()
    }

    fun close() {
        this.excAvailable = false
        closeMenu()
        /* val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)*/
    }

    fun stop() {
        // ToDo 暂停逻辑
    }
}