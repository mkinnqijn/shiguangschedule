package com.xingheyuzhuan.shiguangschedule.widget.double_days

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.xingheyuzhuan.shiguangschedule.widget.WorkManagerHelper
import com.xingheyuzhuan.shiguangschedule.widget.syncAndUpdateAllWidgets
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class DoubleDaysNativeProvider : AppWidgetProvider() {
    // 使用 MainScope 处理异步任务
    private val scope = MainScope()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                // 当系统请求更新（或数据变化手动触发）时渲染 UI
                syncAndUpdateAllWidgets(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 启用更新计划任务表
        WorkManagerHelper.schedulePeriodicWork(context)

    }

    // 移除最后一个小组件时清除任务表
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManagerHelper.cancelAllWork(context)
    }
}
