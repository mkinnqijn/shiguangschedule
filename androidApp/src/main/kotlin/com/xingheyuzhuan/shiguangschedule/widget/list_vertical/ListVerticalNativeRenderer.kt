package com.xingheyuzhuan.shiguangschedule.widget.list_vertical

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.ui.formatCourseNameForDisplay
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import com.xingheyuzhuan.shiguangschedule.widget.WidgetCourseProto
import com.xingheyuzhuan.shiguangschedule.widget.selectTodayCourses
import com.xingheyuzhuan.shiguangschedule.widget.validCoursesOn
import java.time.LocalDate
import java.time.LocalTime

object ListVerticalNativeRenderer {

    fun render(context: Context, snapshot: WidgetSnapshot): RemoteViews =
        renderAt(context, snapshot, LocalDate.now(), LocalTime.now())

    internal fun renderAt(
        context: Context,
        snapshot: WidgetSnapshot,
        today: LocalDate,
        now: LocalTime
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_list_vertical_native)

        resetWidgetState(rv)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rv.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        val tomorrow = today.plusDays(1)
        val currentWeek = if (snapshot.current_week <= 0) null else snapshot.current_week

        if (currentWeek == null) {
            showFullStatus(
                rv,
                context.getString(R.string.title_vacation),
                context.getString(R.string.widget_vacation_expecting)
            )
            return rv
        }

        val todaySelection = snapshot.selectTodayCourses(today, now)
        val tomorrowCourses = snapshot.validCoursesOn(tomorrow)

        val weekDaysArray = context.resources.getStringArray(R.array.week_days_names)
        val dayOfWeekStr = weekDaysArray[today.dayOfWeek.value - 1]

        when {
            todaySelection.remainingTodayCourses.isNotEmpty() -> {
                val weekText = context.getString(R.string.title_current_week, currentWeek.toString())
                rv.setTextViewText(R.id.tv_header_title, "$weekText  $dayOfWeekStr")
                rv.setTextViewText(
                    R.id.tv_header_count_summary,
                    context.getString(
                        R.string.widget_remaining_courses_format_today,
                        todaySelection.remainingTodayCourses.size
                    )
                )
                renderCourseContent(context, rv, todaySelection.remainingTodayCourses, snapshot)
            }
            todaySelection.allTodayCourses.isNotEmpty() -> {
                val weekText = context.getString(R.string.title_current_week, currentWeek.toString())
                rv.setTextViewText(R.id.tv_header_title, "$weekText  $dayOfWeekStr")
                rv.setTextViewText(R.id.tv_header_count_summary, "")
                showInnerStatus(rv, context.getString(R.string.widget_today_courses_finished))
            }
            tomorrowCourses.isNotEmpty() -> {
                rv.setTextViewText(R.id.tv_header_title, context.getString(R.string.widget_tomorrow_course_preview))
                rv.setTextViewText(R.id.tv_header_count_summary, context.getString(R.string.widget_remaining_courses_format_tomorrow, tomorrowCourses.size))
                renderCourseContent(context, rv, tomorrowCourses, snapshot)
            }
            else -> {
                val weekText = context.getString(R.string.title_current_week, currentWeek.toString())
                rv.setTextViewText(R.id.tv_header_title, "$weekText  $dayOfWeekStr")
                showInnerStatus(rv, context.getString(R.string.text_no_courses_today))
                rv.setTextViewText(R.id.tv_header_count_summary, "")
            }
        }
        return rv
    }

    private fun resetWidgetState(rv: RemoteViews) {
        rv.setViewVisibility(R.id.container_full_status, View.GONE)
        rv.setViewVisibility(R.id.inner_content_card, View.VISIBLE)
        rv.setViewVisibility(R.id.container_courses, View.GONE)
        rv.setViewVisibility(R.id.container_status, View.GONE)
        rv.removeAllViews(R.id.container_courses)
    }

    private fun renderCourseContent(context: Context, rv: RemoteViews, courses: List<WidgetCourseProto>, snapshot: WidgetSnapshot) {
        rv.setViewVisibility(R.id.container_courses, View.VISIBLE)

        courses.forEachIndexed { index, course ->
            val itemRv = RemoteViews(context.packageName, R.layout.widget_item_course_list_node)

            itemRv.setTextViewText(R.id.tv_course_name, formatCourseNameForDisplay(course.name))
            itemRv.setTextViewText(R.id.tv_course_position, course.position)
            itemRv.setTextViewText(R.id.tv_course_start_time, course.start_time.take(5))
            itemRv.setTextViewText(R.id.tv_course_end_time, course.end_time.take(5))

            if (course.teacher.isNotBlank()) {
                itemRv.setViewVisibility(R.id.tv_course_teacher, View.VISIBLE)
                itemRv.setTextViewText(R.id.tv_course_teacher, course.teacher)
            } else {
                itemRv.setViewVisibility(R.id.tv_course_teacher, View.GONE)
            }

            val style = snapshot.style
            val colorInt = course.color_int
            if (style != null && colorInt < style.course_color_maps.size) {
                val colorPair = style.course_color_maps[colorInt]
                itemRv.setInt(R.id.course_indicator, "setColorFilter",
                    colorPair.light_color.toInt()
                )
                itemRv.setInt(R.id.course_indicator_dark, "setColorFilter",
                    colorPair.dark_color.toInt()
                )
            }

            rv.addView(R.id.container_courses, itemRv)

            if (index < courses.size - 1) {
                rv.addView(R.id.container_courses, RemoteViews(context.packageName, R.layout.widget_divider_list_vertical))
            }
        }
    }

    private fun showFullStatus(rv: RemoteViews, title: String, msg: String) {
        rv.setViewVisibility(R.id.inner_content_card, View.GONE)
        rv.setViewVisibility(R.id.container_full_status, View.VISIBLE)
        rv.setTextViewText(R.id.tv_full_status_title, title)
        rv.setTextViewText(R.id.tv_full_status_msg, msg)
    }

    private fun showInnerStatus(rv: RemoteViews, title: String) {
        rv.setViewVisibility(R.id.container_courses, View.GONE)
        rv.setViewVisibility(R.id.container_status, View.VISIBLE)
        rv.setTextViewText(R.id.tv_status_title, title)
    }
}
