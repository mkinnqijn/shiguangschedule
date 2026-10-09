package com.xingheyuzhuan.shiguangschedule.widget.double_days

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.widget.WidgetCourseItemRenderer
import com.xingheyuzhuan.shiguangschedule.widget.WidgetCourseProto
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import com.xingheyuzhuan.shiguangschedule.widget.narrowDisplayName
import com.xingheyuzhuan.shiguangschedule.widget.selectTodayCourses
import com.xingheyuzhuan.shiguangschedule.widget.validCoursesOn
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object DoubleDaysNativeRenderer {

    fun render(context: Context, snapshot: WidgetSnapshot): RemoteViews =
        renderAt(context, snapshot, LocalDate.now(), LocalTime.now())

    internal fun renderAt(
        context: Context,
        snapshot: WidgetSnapshot,
        today: LocalDate,
        now: LocalTime
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_double_days_native)

        // 状态彻底重置
        resetWidgetState(rv)

        // 点击跳转逻辑
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rv.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // 全局状态判断
        val currentWeek = if (snapshot.current_week <= 0) null else snapshot.current_week

        if (currentWeek == null) {
            rv.setViewVisibility(R.id.inner_content_card, View.GONE)
            rv.setViewVisibility(R.id.container_vacation, View.VISIBLE)
            rv.setTextViewText(R.id.tv_vacation_title, context.getString(R.string.title_vacation))
            rv.setTextViewText(R.id.tv_vacation_msg, context.getString(R.string.widget_vacation_expecting))
            return rv
        }

        rv.setViewVisibility(R.id.inner_content_card, View.VISIBLE)
        rv.setViewVisibility(R.id.container_vacation, View.GONE)
        rv.setTextViewText(R.id.tv_current_week, context.getString(R.string.status_current_week_format, currentWeek))

        val tomorrow = today.plusDays(1)

        // 渲染左侧：今日
        val todaySelection = snapshot.selectTodayCourses(today, now)
        val todayEmptyText = if (todaySelection.allTodayCourses.isNotEmpty()) {
            context.getString(R.string.widget_today_courses_finished)
        } else {
            context.getString(R.string.text_no_course)
        }

        renderColumn(
            context, rv,
            R.id.container_today, R.id.tv_today_date, R.id.tv_today_footer,
            R.id.empty_today_container,
            intArrayOf(R.id.container_today_course_1, R.id.container_today_course_2),
            R.id.divider_today_courses,
            today, todaySelection.remainingTodayCourses,
            isToday = true,
            emptyText = todayEmptyText,
            snapshot = snapshot
        )

        // 渲染右侧：明日
        val tomorrowCourses = snapshot.validCoursesOn(tomorrow)

        renderColumn(
            context, rv,
            R.id.container_tomorrow, R.id.tv_tomorrow_date, R.id.tv_tomorrow_footer,
            R.id.empty_tomorrow_container,
            intArrayOf(R.id.container_tomorrow_course_1, R.id.container_tomorrow_course_2),
            R.id.divider_tomorrow_courses,
            tomorrow, tomorrowCourses,
            isToday = false,
            emptyText = context.getString(R.string.text_no_course),
            snapshot = snapshot
        )

        return rv
    }

    private fun resetWidgetState(rv: RemoteViews) {
        rv.setViewVisibility(R.id.inner_content_card, View.VISIBLE)
        rv.setViewVisibility(R.id.container_vacation, View.GONE)
        val slots = intArrayOf(
            R.id.container_today_course_1,
            R.id.container_today_course_2,
            R.id.container_tomorrow_course_1,
            R.id.container_tomorrow_course_2
        )
        slots.forEach { slotId ->
            rv.removeAllViews(slotId)
            rv.setViewVisibility(slotId, View.INVISIBLE)
        }
        rv.setViewVisibility(R.id.divider_today_courses, View.GONE)
        rv.setViewVisibility(R.id.divider_tomorrow_courses, View.GONE)
        rv.setViewVisibility(R.id.empty_today_container, View.GONE)
        rv.setViewVisibility(R.id.empty_tomorrow_container, View.GONE)
    }

    private fun renderColumn(
        context: Context,
        rootRv: RemoteViews,
        containerId: Int,
        dateId: Int,
        footerId: Int,
        emptyContainerId: Int,
        slotIds: IntArray,
        dividerId: Int,
        date: LocalDate,
        displayCourses: List<WidgetCourseProto>,
        isToday: Boolean,
        emptyText: String,
        snapshot: WidgetSnapshot
    ) {
        // 设置日期标题
        val prefix = if (isToday) {
            context.getString(R.string.widget_title_today)
        } else {
            context.getString(R.string.widget_title_tomorrow)
        }
        val weekDaysArray = context.resources.getStringArray(R.array.week_days_names)
        val dayOfWeekStr = weekDaysArray[date.dayOfWeek.value - 1]
        val monthDayStr = date.format(DateTimeFormatter.ofPattern("M.dd"))

        rootRv.setTextViewText(dateId, "$prefix $monthDayStr $dayOfWeekStr")

        if (displayCourses.isEmpty()) {
            rootRv.setViewVisibility(containerId, View.GONE)
            rootRv.setViewVisibility(emptyContainerId, View.VISIBLE)
            rootRv.setViewVisibility(footerId, View.GONE)
            val emptyTextViewId = if (isToday) R.id.empty_today else R.id.empty_tomorrow
            rootRv.setTextViewText(emptyTextViewId, emptyText)
        } else {
            rootRv.setViewVisibility(containerId, View.VISIBLE)
            rootRv.setViewVisibility(emptyContainerId, View.GONE)
            rootRv.setViewVisibility(footerId, View.VISIBLE)

            val countRes = if (isToday) {
                R.string.widget_course_remaining_count
            } else {
                R.string.widget_course_total_count
            }
            rootRv.setTextViewText(footerId, context.getString(countRes, displayCourses.size))

            val visibleCourses = displayCourses.take(slotIds.size)
            visibleCourses.forEachIndexed { index, course ->
                val slotId = slotIds[index]
                val itemRv = WidgetCourseItemRenderer.create(
                    context = context,
                    course = course,
                    displayName = course.narrowDisplayName(),
                    snapshot = snapshot
                )
                rootRv.addView(slotId, itemRv)
                rootRv.setViewVisibility(slotId, View.VISIBLE)
            }
            rootRv.setViewVisibility(
                dividerId,
                if (visibleCourses.size == 2) View.VISIBLE else View.GONE
            )
        }
    }
}
